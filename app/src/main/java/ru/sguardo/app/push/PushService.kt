package ru.sguardo.app.push

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ru.sguardo.app.MainActivity
import ru.sguardo.app.R
import ru.sguardo.app.data.Repo
import kotlinx.coroutines.launch

/**
 * Push из FCM (src/server/native-push.ts): notification {title, body}, data {url: "/messages?c=<id>", tag}.
 * Когда приложение на экране, уведомление рисуем сами; нажатие открывает нужный чат.
 */
class PushService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Repo.get(this).registerToken(token)
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        val repo = Repo.get(this)
        if (!repo.prefs.notifyMessages.value) return
        val title = msg.notification?.title ?: msg.data["title"] ?: "Sguardo"
        val body = msg.notification?.body ?: msg.data["body"] ?: ""
        val url = msg.data["url"] ?: "/messages"
        val tag = msg.data["tag"] ?: url
        val open = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("url", url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(this, tag.hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, "messages")
            .setSmallIcon(R.drawable.ic_stat_sguardo)
            .setColor(ContextCompat.getColor(this, R.color.brand))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .build()
        try { NotificationManagerCompat.from(this).notify(tag, 1, n) } catch (_: SecurityException) {}
        repo.scope.launch { repo.reload() }
    }
}
