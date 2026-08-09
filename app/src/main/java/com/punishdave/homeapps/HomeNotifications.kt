package com.punishdave.homeapps

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

private const val HomeChannel = "homeapps_updates"

fun createHomeNotificationChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(NotificationChannel(HomeChannel, "HomeApps updates", NotificationManager.IMPORTANCE_DEFAULT))
}

fun notifyGameNight(context: Context, text: String) {
    val notification = NotificationCompat.Builder(context, HomeChannel)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("GameWithDave")
        .setContentText(text)
        .setAutoCancel(true)
        .build()
    context.getSystemService(NotificationManager::class.java).notify(4102, notification)
}
