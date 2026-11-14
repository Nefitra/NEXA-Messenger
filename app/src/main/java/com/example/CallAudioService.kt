package com.example

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/** Keeps an explicitly accepted/outgoing microphone call alive when the screen turns off. */
class CallAudioService : Service() {
  companion object {
    @Volatile internal var hangUp: (() -> Unit)? = null
    fun start(context: Context, end: () -> Unit) {
      hangUp = end
      context.startForegroundService(Intent(context, CallAudioService::class.java))
    }
    fun stop(context: Context) {
      hangUp = null
      context.stopService(Intent(context, CallAudioService::class.java))
    }
  }
  override fun onCreate() {
    super.onCreate()
    getSystemService(NotificationManager::class.java).createNotificationChannel(
      NotificationChannel("tonino-call", "Голосовые звонки", NotificationManager.IMPORTANCE_LOW))
    val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val end = PendingIntent.getService(this, 1, Intent(this, CallAudioService::class.java).setAction("end"),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val notification = Notification.Builder(this, "tonino-call")
      .setSmallIcon(android.R.drawable.sym_action_call).setContentTitle("Tonino — голосовой звонок")
      .setContentText("Нажмите, чтобы вернуться к разговору")
      .setContentIntent(open).setOngoing(true).setCategory(Notification.CATEGORY_CALL)
      .addAction(Notification.Action.Builder(null, "Завершить", end).build()).build()
    if (Build.VERSION.SDK_INT >= 30) startForeground(12, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
    else startForeground(12, notification)
  }
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == "end") { hangUp?.invoke(); stopSelf() }
    return START_NOT_STICKY
  }
  override fun onBind(intent: Intent?): IBinder? = null
}
