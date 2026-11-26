# Sguardo: правила R8 поверх тех, что приходят с библиотеками (kotlinx.serialization, OkHttp, Firebase, CameraX их содержат).

# kotlinx.serialization: модели ответа сервера разбираются сгенерированными сериализаторами
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class ru.sguardo.app.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class ru.sguardo.app.data.**$$serializer { *; }
-keepclassmembers class ru.sguardo.app.data.** {
    *** Companion;
}

# Tink внутри EncryptedSharedPreferences ссылается на аннотации, которых нет в рантайме
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**

# OkHttp: необязательные TLS-провайдеры
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Сервис push объявлен в манифесте
-keep class ru.sguardo.app.push.PushService { *; }
