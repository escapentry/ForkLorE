package com.example.telecom

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import androidx.core.content.ContextCompat
import com.example.R

class ForkLoreInCallService : InCallService() {

  companion object {
    private const val CHANNEL_ID = "folklore_calls"
    private const val CALL_NOTIF_ID = 4091
    const val ACTION_ANSWER_CALL = "com.thallyace.folklore.ACTION_ANSWER_CALL"
    const val ACTION_DECLINE_CALL = "com.thallyace.folklore.ACTION_DECLINE_CALL"
  }

  override fun onCreate() {
    super.onCreate()
    ForkLoreCallManager.attachService(this)
    ensureNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_ANSWER_CALL -> ForkLoreCallManager.answerCall()
      ACTION_DECLINE_CALL -> ForkLoreCallManager.rejectOrEndCall()
    }
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onCallAdded(call: Call) {
    super.onCallAdded(call)
    ForkLoreCallManager.onCallAdded(call)
    postCallNotification(call)
    launchCallUi()
  }

  override fun onCallRemoved(call: Call) {
    super.onCallRemoved(call)
    ForkLoreCallManager.onCallRemoved(call)
    cancelCallNotification()
  }

  @Deprecated("Overrides InCallService callback for API 24-36 compatibility")
  override fun onCallAudioStateChanged(audioState: CallAudioState) {
    @Suppress("DEPRECATION")
    super.onCallAudioStateChanged(audioState)
    ForkLoreCallManager.onAudioStateChanged(audioState)
  }

  override fun onDestroy() {
    cancelCallNotification()
    ForkLoreCallManager.detachService()
    super.onDestroy()
  }

  private fun launchCallUi() {
    runCatching {
      val intent = Intent(this, CallActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
      }
      startActivity(intent)
    }
  }

  private fun ensureNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(NotificationManager::class.java) ?: return
      if (nm.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          "Calls",
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = "Incoming and ongoing voice calls"
        }
        nm.createNotificationChannel(channel)
      }
    }
  }

  private fun postCallNotification(call: Call) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }

    val number = call.details?.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() } ?: "Incoming Call"
    val callerName = call.details?.callerDisplayName?.takeIf { it.isNotBlank() } ?: number

    val fullScreenIntent = Intent(this, CallActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
    }
    val fullScreenPending = PendingIntent.getActivity(
      this,
      10,
      fullScreenIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val answerPending = PendingIntent.getService(
      this,
      11,
      Intent(this, ForkLoreInCallService::class.java).apply { action = ACTION_ANSWER_CALL },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val declinePending = PendingIntent.getService(
      this,
      12,
      Intent(this, ForkLoreInCallService::class.java).apply { action = ACTION_DECLINE_CALL },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Notification.Builder(this, CHANNEL_ID)
    } else {
      @Suppress("DEPRECATION")
      Notification.Builder(this)
    }

    val notification = builder
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(callerName)
      .setContentText("ForkLorE Call")
      .setOngoing(true)
      .setContentIntent(fullScreenPending)
      .setFullScreenIntent(fullScreenPending, true)
      .addAction(Notification.Action.Builder(null, "Answer", answerPending).build())
      .addAction(Notification.Action.Builder(null, "Decline", declinePending).build())
      .build()

    val nm = getSystemService(NotificationManager::class.java)
    nm?.notify(CALL_NOTIF_ID, notification)
  }

  private fun cancelCallNotification() {
    getSystemService(NotificationManager::class.java)?.cancel(CALL_NOTIF_ID)
  }
}
