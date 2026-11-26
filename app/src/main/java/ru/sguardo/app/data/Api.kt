package ru.sguardo.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import ru.sguardo.app.BuildConfig
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ApiError(val status: Int, override val message: String) : Exception(message)

/** Кука сессии sid (path "/", httpOnly) хранится в зашифрованных настройках и переживает перезапуск. */
class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs: SharedPreferences = try {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "sg_cookies", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (_: Throwable) {
        context.getSharedPreferences("sg_cookies_plain", Context.MODE_PRIVATE)
    }
    private val cache = mutableMapOf<String, Cookie>()

    init {
        val base = BuildConfig.BASE_URL.toHttpUrl()
        prefs.all.forEach { (k, v) ->
            val c = (v as? String)?.let { Cookie.parse(base, it) }
            if (c != null && c.expiresAt > System.currentTimeMillis()) cache[k] = c
        }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val ed = prefs.edit()
        for (c in cookies) {
            val key = c.domain + "|" + c.name
            if (c.expiresAt < System.currentTimeMillis() || c.value.isEmpty()) {
                cache.remove(key); ed.remove(key)
            } else {
                cache[key] = c; ed.putString(key, c.toString())
            }
        }
        ed.apply()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        cache.values.filter { it.matches(url) && it.expiresAt > System.currentTimeMillis() }

    @Synchronized
    fun hasSession() = cache.values.any { it.name == "sid" }

    @Synchronized
    fun clear() { cache.clear(); prefs.edit().clear().apply() }
}

class Api(context: Context) {
    val base: String = BuildConfig.BASE_URL
    val cookies = PersistentCookieJar(context.applicationContext)
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookies)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("Accept-Language", "ru").build())
        }
        .build()

    /** Для SSE: без таймаута чтения (сервер шлёт ping раз в 20 с). */
    val streamClient: OkHttpClient = client.newBuilder().readTimeout(0, TimeUnit.SECONDS).build()

    /** Для чужих адресов (картинки из сетей): без кук. */
    val foreignClient: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    /** Путь API всегда со слэшем на конце (trailingSlash), иначе 308. */
    fun url(path: String, query: Map<String, String> = emptyMap()): HttpUrl {
        var p = if (path.startsWith("/")) path else "/$path"
        if (!p.endsWith("/")) p += "/"
        val b = (base + p).toHttpUrl().newBuilder()
        query.forEach { (k, v) -> b.addQueryParameter(k, v) }
        return b.build()
    }

    /** Ссылка на файл из данных кабинета → полный адрес. Чужие адреса — только https. */
    fun media(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (raw.startsWith("http", ignoreCase = true)) return raw.takeIf { it.startsWith("https://", true) }
        var p = if (raw.startsWith("/")) raw else "/$raw"
        if (!p.contains("?") && !p.endsWith("/")) p += "/"
        return base + p
    }

    fun isOwn(url: String) = url.startsWith(base)

    suspend fun raw(method: String, path: String, body: JsonElement? = null, query: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap()): Response {
        val rb = Request.Builder().url(url(path, query)).header("Accept", "application/json")
        headers.forEach { (k, v) -> rb.header(k, v) }
        val reqBody = body?.let { JsonCfg.encodeToString(JsonElement.serializer(), it).toRequestBody(jsonType) }
        when (method) {
            "GET" -> rb.get()
            "DELETE" -> if (reqBody != null) rb.delete(reqBody) else rb.delete()
            else -> rb.method(method, reqBody ?: "{}".toRequestBody(jsonType))
        }
        return client.newCall(rb.build()).await()
    }

    suspend fun call(method: String, path: String, body: JsonElement? = null, query: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            raw(method, path, body, query).use { r ->
                val text = r.body?.string().orEmpty()
                if (r.code >= 400) throw ApiError(r.code, errorText(text, r.code))
                text
            }
        }

    suspend inline fun <reified T> get(path: String, query: Map<String, String> = emptyMap()): T =
        decode(call("GET", path, null, query))

    suspend inline fun <reified T> post(path: String, body: JsonElement): T = decode(call("POST", path, body))

    inline fun <reified T> decode(text: String): T = try {
        JsonCfg.decodeFromString<T>(text)
    } catch (e: Exception) {
        throw ApiError(0, "Сервер ответил непонятно, обновите приложение")
    }

    suspend fun upload(file: File, name: String, mime: String): Attachment = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("name", name)
            .addFormDataPart("file", name, file.asRequestBody(mime.toMediaType()))
            .build()
        val req = Request.Builder().url(url("/api/files/upload/")).post(body).build()
        client.newCall(req).await().use { r ->
            val text = r.body?.string().orEmpty()
            if (r.code >= 400) throw ApiError(r.code, errorText(text, r.code))
            decode<UploadResponse>(text).attachment
        }
    }

    fun errorText(body: String, status: Int): String {
        val e = try { JsonCfg.decodeFromString<ErrorBody>(body).error } catch (_: Exception) { null }
        if (!e.isNullOrBlank()) return e
        return when (status) {
            401 -> "Нужно войти заново"
            402 -> "Кабинет не оплачен: продлите тариф в веб-версии"
            403 -> "Нет доступа"
            404 -> "Не найдено"
            409 -> "Не получилось: данные изменились"
            429 -> "Слишком много попыток, подождите немного"
            else -> if (status >= 500) "Сервер не ответил, попробуйте ещё раз" else "Ошибка $status"
        }
    }
}

fun obj(vararg pairs: Pair<String, Any?>): JsonObject = buildJsonObject {
    for ((k, v) in pairs) when (v) {
        null -> {}
        is String -> put(k, v)
        is Number -> put(k, v)
        is Boolean -> put(k, v)
        is JsonElement -> put(k, v)
        is List<*> -> put(k, kotlinx.serialization.json.JsonArray(v.map { toJson(it) }))
        is Map<*, *> -> put(k, JsonObject(v.entries.associate { it.key.toString() to toJson(it.value) }))
        else -> put(k, v.toString())
    }
}

private fun toJson(v: Any?): JsonElement = when (v) {
    null -> kotlinx.serialization.json.JsonNull
    is String -> kotlinx.serialization.json.JsonPrimitive(v)
    is Number -> kotlinx.serialization.json.JsonPrimitive(v)
    is Boolean -> kotlinx.serialization.json.JsonPrimitive(v)
    is JsonElement -> v
    is List<*> -> kotlinx.serialization.json.JsonArray(v.map { toJson(it) })
    is Map<*, *> -> JsonObject(v.entries.associate { it.key.toString() to toJson(it.value) })
    else -> kotlinx.serialization.json.JsonPrimitive(v.toString())
}

suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { if (!cont.isCancelled) cont.resumeWithException(e) }
        override fun onResponse(call: Call, response: Response) { cont.resume(response) }
    })
    cont.invokeOnCancellation { try { cancel() } catch (_: Throwable) {} }
}

fun friendly(e: Throwable): String = when (e) {
    is ApiError -> e.message
    is java.net.UnknownHostException, is java.net.ConnectException -> "Нет интернета"
    is java.net.SocketTimeoutException -> "Сервер долго не отвечает"
    is IOException -> "Сеть недоступна"
    else -> e.message ?: "Что-то пошло не так"
}
