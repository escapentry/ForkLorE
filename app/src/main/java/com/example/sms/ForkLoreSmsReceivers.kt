package com.example.sms

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ForkLoreSmsEvents {
  private val _incomingRefreshTick = MutableStateFlow(0)
  val incomingRefreshTick = _incomingRefreshTick.asStateFlow()

  fun notifySmsUpdated() {
    _incomingRefreshTick.value = _incomingRefreshTick.value + 1
  }
}

class ForkLoreSmsReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
    if (messages.isNullOrEmpty()) return

    val groupedBySender = LinkedHashMap<String, StringBuilder>()
    var latestTimestamp = System.currentTimeMillis()
    for (msg in messages) {
      val sender = msg.displayOriginatingAddress ?: msg.originatingAddress ?: "Unknown"
      val body = msg.displayMessageBody ?: msg.messageBody ?: ""
      groupedBySender.getOrPut(sender) { StringBuilder() }.append(body)
      if (msg.timestampMillis > 0L) latestTimestamp = msg.timestampMillis
    }

    for ((sender, bodyBuilder) in groupedBySender) {
      val fullBody = bodyBuilder.toString()
      runCatching {
        val values = ContentValues().apply {
          put(Telephony.Sms.ADDRESS, sender)
          put(Telephony.Sms.BODY, fullBody)
          put(Telephony.Sms.DATE, latestTimestamp)
          put(Telephony.Sms.READ, 0)
          put(Telephony.Sms.SEEN, 0)
          put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
        }
        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
      }
      showSmsNotification(context, sender, fullBody)
    }

    ForkLoreSmsEvents.notifySmsUpdated()
  }

  private fun showSmsNotification(context: Context, sender: String, body: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }

    val channelId = "folklore_messages"
    val nm = context.getSystemService(NotificationManager::class.java) ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(channelId) == null) {
      nm.createNotificationChannel(
        NotificationChannel(channelId, "Messages", NotificationManager.IMPORTANCE_HIGH)
      )
    }

    val openIntent = Intent(context, MainActivity::class.java).apply {
      action = Intent.ACTION_SENDTO
      data = Uri.parse("smsto:${Uri.encode(sender)}")
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      sender.hashCode(),
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Notification.Builder(context, channelId)
    } else {
      @Suppress("DEPRECATION")
      Notification.Builder(context)
    }

    val notif = builder
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(sender)
      .setContentText(body)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    nm.notify(sender.hashCode(), notif)
  }
}

class ForkLoreMmsReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    // Required receiver registration for Default SMS app eligibility
    ForkLoreSmsEvents.notifySmsUpdated()
  }
}

class ForkLoreRespondViaMessageService : Service() {
  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val recipient = intent?.data?.schemeSpecificPart?.substringBefore('?')
    val messageText = intent?.getStringExtra(Intent.EXTRA_TEXT)
    if (!recipient.isNullOrBlank() && !messageText.isNullOrBlank()) {
      runCatching {
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          getSystemService(SmsManager::class.java)
        } else {
          @Suppress("DEPRECATION")
          SmsManager.getDefault()
        }
        val parts = smsManager.divideMessage(messageText)
        smsManager.sendMultipartTextMessage(recipient, null, parts, null, null)
        val values = ContentValues().apply {
          put(Telephony.Sms.ADDRESS, recipient)
          put(Telephony.Sms.BODY, messageText)
          put(Telephony.Sms.DATE, System.currentTimeMillis())
          put(Telephony.Sms.READ, 1)
          put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
        }
        contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
        ForkLoreSmsEvents.notifySmsUpdated()
      }
    }
    stopSelf(startId)
    return START_NOT_STICKY
  }
}
