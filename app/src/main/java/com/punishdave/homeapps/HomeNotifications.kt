package com.punishdave.homeapps

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

private const val HomeChannel = "homeapps_updates"

fun createHomeNotificationChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(NotificationChannel(HomeChannel, "HomeApps updates", NotificationManager.IMPORTANCE_DEFAULT))
}

fun notifyGameNight(context: Context, text: String) {
    notifyHome(context, 4102, "GameWithDave", text, "gamewithdave")
}

fun notifyHome(context: Context, id: Int, title: String, text: String, route: String? = null) {
    val contentIntent = route?.let {
        PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).putExtra("homeapps_route", it),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    val notification = NotificationCompat.Builder(context, HomeChannel)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
        .build()
    context.getSystemService(NotificationManager::class.java).notify(id, notification)
}
