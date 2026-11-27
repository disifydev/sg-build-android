package ru.sguardo.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.Coil
import kotlinx.coroutines.launch
import coil.ImageLoader
import ru.sguardo.app.data.Phase
import ru.sguardo.app.data.Repo
import ru.sguardo.app.ui.Permissions
import ru.sguardo.app.ui.SgApp

class MainActivity : ComponentActivity(), Permissions {
    private lateinit var repo: Repo
    private var permDone: ((Boolean) -> Unit)? = null
    private var pickDone: ((Uri?) -> Unit)? = null
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r -> permDone?.invoke(r.values.all { it }); permDone = null }
    private val imageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> pickDone?.invoke(u); pickDone = null }
    private val fileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { u -> pickDone?.invoke(u); pickDone = null }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        repo = Repo.get(this)
        Coil.setImageLoader(ImageLoader.Builder(this).okHttpClient(repo.api.client).crossfade(true)
            .components { add(coil.decode.VideoFrameDecoder.Factory()) }.build())
        repo.watchNetwork()
        if (repo.phase.value == Phase.Launching) repo.bootstrap()
        repo.registerPushIfPossible()
        handle(intent)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { if (repo.phase.value == Phase.Ready && !repo.demo.value) { repo.startLive(); repo.scope.launch { repo.reload() }; repo.registerPushIfPossible() } }
            override fun onStop(owner: LifecycleOwner) { repo.stopLive() }
        })
        setContent { SgApp(repo, this) }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handle(intent) }

    private fun handle(i: Intent?) { i?.getStringExtra("url")?.let { repo.handleUrl(it) } }

    private val askedSp by lazy { getSharedPreferences("sg_perm", MODE_PRIVATE) }

    private fun ask(perms: Array<String>, done: (Boolean) -> Unit) {
        val need = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (need.isEmpty()) { done(true); return }
        android.util.Log.i("SGPERM", "системный запрос: ${need.joinToString()}")
        askedSp.edit().apply { need.forEach { putBoolean(it, true) } }.apply()
        permDone = { ok -> android.util.Log.i("SGPERM", "ответ: $ok"); done(ok) }; permLauncher.launch(need.toTypedArray())
    }

    /** Уже спрашивали, а система больше не покажет окно («Больше не спрашивать» или два отказа). */
    private fun isBlocked(vararg perms: String) = perms.any { p ->
        !granted(p) && askedSp.getBoolean(p, false) && !shouldShowRequestPermissionRationale(p)
    }

    override fun notifications(done: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= 33) ask(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) { ok -> if (ok) repo.registerPushIfPossible(); done(ok) }
        else {
            // до Android 13 окна-запроса у системы нет: уведомления включены сразу, если их не выключили в настройках
            repo.registerPushIfPossible()
            val on = androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()
            if (!on) openNotifySettings()
            done(on)
        }
    }
    override fun audioBlocked() = isBlocked(Manifest.permission.RECORD_AUDIO)
    override fun cameraBlocked() = isBlocked(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    override fun notificationsBlocked() = if (Build.VERSION.SDK_INT >= 33) isBlocked(Manifest.permission.POST_NOTIFICATIONS) else !hasNotifications()
    override fun openSettings() {
        startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    override fun openNotifySettings() {
        try { startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { openSettings() }
    }
    override fun contacts(done: (Boolean) -> Unit) = ask(arrayOf(Manifest.permission.READ_CONTACTS)) { ok -> repo.book.refresh(); done(ok) }
    override fun audio(done: (Boolean) -> Unit) = ask(arrayOf(Manifest.permission.RECORD_AUDIO), done)
    override fun camera(done: (Boolean) -> Unit) = ask(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), done)
    override fun hasContacts() = granted(Manifest.permission.READ_CONTACTS)
    override fun hasNotifications() = androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled() && (Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS))
    override fun hasAudio() = granted(Manifest.permission.RECORD_AUDIO)
    override fun hasCamera() = granted(Manifest.permission.CAMERA)
    override fun pickImage(done: (Uri?) -> Unit) { pickDone = done; imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
    override fun pickFile(done: (Uri?) -> Unit) { pickDone = done; fileLauncher.launch(arrayOf("*/*")) }
    private fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
}
