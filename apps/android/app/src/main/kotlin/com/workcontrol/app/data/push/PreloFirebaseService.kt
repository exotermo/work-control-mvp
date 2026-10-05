package com.workcontrol.app.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.workcontrol.app.MainActivity
import com.workcontrol.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

const val PRELO_ALERTS_CHANNEL = "prelo_alerts"

fun createPreloAlertsChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(NotificationChannel(
        PRELO_ALERTS_CHANNEL, "Aprovações e deploys", NotificationManager.IMPORTANCE_HIGH))
}

@AndroidEntryPoint
class PreloFirebaseService : FirebaseMessagingService() {
    @Inject lateinit var registrar: PushRegistrar

    override fun onNewToken(token: String) { registrar.onNewToken(token) }

    override fun onMessageReceived(message: RemoteMessage) {
        val destination = PushDestination.from(message.data) ?: return
        val notification = message.notification ?: return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        // Text is supplied by Prelo/FCM. No local content is assembled from payload data.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("kind", destination.kind)
            putExtra("id", destination.id)
            destination.projectId?.let { putExtra("projectId", it) }
        }
        val pending = PendingIntent.getActivity(this, destination.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val built = NotificationCompat.Builder(this, PRELO_ALERTS_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(this).notify(destination.kind, destination.id.hashCode(), built)
    }
}
