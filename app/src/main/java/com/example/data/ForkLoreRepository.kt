package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.telecom.TelecomManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.model.CallDirection
import com.example.model.CallLogEntry
import com.example.model.ContactEntry
import com.example.model.ConversationThread
import com.example.model.DefaultHandlerStatus
import com.example.model.ForkLoreNumberHelper
import com.example.model.SmsMessageEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForkLoreRepository(private val context: Context) {

  private val resolver = context.contentResolver

  fun checkDefaultAndPermissions(): DefaultHandlerStatus {
    val isDefaultDialer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val rm = context.getSystemService(RoleManager::class.java)
      rm?.isRoleHeld(RoleManager.ROLE_DIALER) == true
    } else {
      val tm = context.getSystemService(TelecomManager::class.java)
      tm?.defaultDialerPackage == context.packageName
    }

    val isDefaultSms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val rm = context.getSystemService(RoleManager::class.java)
      rm?.isRoleHeld(RoleManager.ROLE_SMS) == true
    } else {
      Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
    }

    val hasContacts = hasPermission(Manifest.permission.READ_CONTACTS)
    val hasPhone = hasPermission(Manifest.permission.CALL_PHONE) &&
      hasPermission(Manifest.permission.READ_CALL_LOG)
    val hasSms = hasPermission(Manifest.permission.READ_SMS) &&
      hasPermission(Manifest.permission.SEND_SMS)

    return DefaultHandlerStatus(
      isDefaultDialer = isDefaultDialer,
      isDefaultSms = isDefaultSms,
      hasContactsPermission = hasContacts,
      hasPhonePermissions = hasPhone,
      hasSmsPermissions = hasSms,
    )
  }

  fun createDefaultDialerIntent(): Intent? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val rm = context.getSystemService(RoleManager::class.java)
      if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
        return rm.createRequestRoleIntent(RoleManager.ROLE_DIALER)
      }
    }
    return Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
      putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
    }
  }

  fun createDefaultSmsIntent(): Intent? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val rm = context.getSystemService(RoleManager::class.java)
      if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_SMS)) {
        return rm.createRequestRoleIntent(RoleManager.ROLE_SMS)
      }
    }
    return Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
      putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
    }
  }

  fun requiredRuntimePermissions(): Array<String> = buildList {
    add(Manifest.permission.READ_CONTACTS)
    add(Manifest.permission.WRITE_CONTACTS)
    add(Manifest.permission.CALL_PHONE)
    add(Manifest.permission.READ_PHONE_STATE)
    add(Manifest.permission.READ_CALL_LOG)
    add(Manifest.permission.WRITE_CALL_LOG)
    add(Manifest.permission.READ_SMS)
    add(Manifest.permission.SEND_SMS)
    add(Manifest.permission.RECEIVE_SMS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      add(Manifest.permission.ANSWER_PHONE_CALLS)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      add(Manifest.permission.POST_NOTIFICATIONS)
    }
  }.toTypedArray()

  suspend fun loadContacts(): List<ContactEntry> = withContext(Dispatchers.IO) {
    if (!hasPermission(Manifest.permission.READ_CONTACTS)) return@withContext emptyList()
    val grouped = LinkedHashMap<Long, MutableContactBuilder>()
    val projection = arrayOf(
      ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
      ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
      ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
      ContactsContract.CommonDataKinds.Phone.NUMBER,
      ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
      ContactsContract.CommonDataKinds.Phone.STARRED,
    )
    runCatching {
      resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        "${ContactsContract.CommonDataKinds.Phone.STARRED} DESC, ${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC",
      )?.use { cursor ->
        val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val lookupCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
        val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
        val numCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val photoCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
        val starCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

        while (cursor.moveToNext()) {
          val id = if (idCol >= 0) cursor.getLong(idCol) else continue
          val lookup = if (lookupCol >= 0) cursor.getString(lookupCol).orEmpty() else ""
          val name = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else "Unnamed"
          val num = if (numCol >= 0) cursor.getString(numCol).orEmpty().trim() else ""
          val photo = if (photoCol >= 0) cursor.getString(photoCol) else null
          val starred = starCol >= 0 && cursor.getInt(starCol) == 1
          if (num.isBlank()) continue

          val builder = grouped.getOrPut(id) {
            MutableContactBuilder(
              id = id,
              lookupKey = lookup,
              displayName = name.ifBlank { num },
              photoUri = photo,
              isStarred = starred,
            )
          }
          if (num !in builder.numbers) {
            builder.numbers.add(num)
          }
        }
      }
    }
    grouped.values.map { it.build() }
  }

  suspend fun toggleContactStar(contactId: Long, starred: Boolean): Boolean = withContext(Dispatchers.IO) {
    if (!hasPermission(Manifest.permission.WRITE_CONTACTS)) return@withContext false
    runCatching {
      val values = ContentValues().apply {
        put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
      }
      resolver.update(
        ContactsContract.Contacts.CONTENT_URI,
        values,
        "${ContactsContract.Contacts._ID} = ?",
        arrayOf(contactId.toString()),
      ) > 0
    }.getOrDefault(false)
  }

  suspend fun addContact(name: String, phone: String): Boolean = withContext(Dispatchers.IO) {
    if (!hasPermission(Manifest.permission.WRITE_CONTACTS)) return@withContext false
    if (name.isBlank() || phone.isBlank()) return@withContext false
    runCatching {
      val ops = arrayListOf<ContentProviderOperation>()
      ops.add(
        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
          .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
          .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
          .build()
      )
      ops.add(
        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
          .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
          .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
          .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name.trim())
          .build()
      )
      ops.add(
        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
          .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
          .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
          .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.trim())
          .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
          .build()
      )
      resolver.applyBatch(ContactsContract.AUTHORITY, ops)
      true
    }.getOrDefault(false)
  }

  @SuppressLint("MissingPermission")
  suspend fun loadCallLogs(contacts: List<ContactEntry>): List<CallLogEntry> = withContext(Dispatchers.IO) {
    if (!hasPermission(Manifest.permission.READ_CALL_LOG)) return@withContext emptyList()
    val results = ArrayList<CallLogEntry>()
    val projection = arrayOf(
      CallLog.Calls._ID,
      CallLog.Calls.NUMBER,
      CallLog.Calls.CACHED_NAME,
      CallLog.Calls.TYPE,
      CallLog.Calls.DATE,
      CallLog.Calls.DURATION,
    )
    runCatching {
      resolver.query(
        CallLog.Calls.CONTENT_URI,
        projection,
        null,
        null,
        "${CallLog.Calls.DATE} DESC LIMIT 200",
      )?.use { cursor ->
        val idCol = cursor.getColumnIndex(CallLog.Calls._ID)
        val numCol = cursor.getColumnIndex(CallLog.Calls.NUMBER)
        val nameCol = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
        val typeCol = cursor.getColumnIndex(CallLog.Calls.TYPE)
        val dateCol = cursor.getColumnIndex(CallLog.Calls.DATE)
        val durCol = cursor.getColumnIndex(CallLog.Calls.DURATION)

        while (cursor.moveToNext()) {
          val id = if (idCol >= 0) cursor.getLong(idCol) else continue
          val number = if (numCol >= 0) cursor.getString(numCol).orEmpty() else ""
          val cachedName = if (nameCol >= 0) cursor.getString(nameCol) else null
          val type = if (typeCol >= 0) cursor.getInt(typeCol) else CallLog.Calls.INCOMING_TYPE
          val date = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
          val dur = if (durCol >= 0) cursor.getLong(durCol) else 0L

          val matchedContact = contacts.firstOrNull { c ->
            c.allNumbers.any { ForkLoreNumberHelper.numbersMatch(it, number) }
          }
          val direction = when (type) {
            CallLog.Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
            CallLog.Calls.MISSED_TYPE -> CallDirection.MISSED
            CallLog.Calls.REJECTED_TYPE -> CallDirection.REJECTED
            CallLog.Calls.BLOCKED_TYPE -> CallDirection.BLOCKED
            else -> CallDirection.INCOMING
          }

          results.add(
            CallLogEntry(
              id = id,
              number = number.ifBlank { "Private Number" },
              displayName = matchedContact?.displayName ?: cachedName?.takeIf { it.isNotBlank() },
              direction = direction,
              timestampMs = date,
              durationSec = dur.coerceAtLeast(0L),
            )
          )
        }
      }
    }
    results
  }

  suspend fun loadConversations(contacts: List<ContactEntry>): List<ConversationThread> =
    withContext(Dispatchers.IO) {
      if (!hasPermission(Manifest.permission.READ_SMS)) return@withContext emptyList()
      val byThreadKey = LinkedHashMap<String, ConversationThread>()
      val projection = arrayOf(
        Telephony.Sms._ID,
        Telephony.Sms.THREAD_ID,
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE,
        Telephony.Sms.READ,
      )

      runCatching {
        resolver.query(
          Telephony.Sms.CONTENT_URI,
          projection,
          null,
          null,
          "${Telephony.Sms.DATE} DESC LIMIT 500",
        )?.use { cursor ->
          val threadCol = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
          val addrCol = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
          val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
          val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)
          val readCol = cursor.getColumnIndex(Telephony.Sms.READ)

          while (cursor.moveToNext()) {
            val threadId = if (threadCol >= 0) cursor.getLong(threadCol) else 0L
            val address = if (addrCol >= 0) cursor.getString(addrCol).orEmpty() else ""
            if (address.isBlank()) continue
            val body = if (bodyCol >= 0) cursor.getString(bodyCol).orEmpty() else ""
            val date = if (dateCol >= 0) cursor.getLong(dateCol) else 0L
            val isRead = readCol < 0 || cursor.getInt(readCol) == 1

            val key = ForkLoreNumberHelper.matchKey(address).ifBlank { address }
            val existing = byThreadKey[key]
            if (existing == null) {
              val contact = contacts.firstOrNull { c ->
                c.allNumbers.any { ForkLoreNumberHelper.numbersMatch(it, address) }
              }
              byThreadKey[key] = ConversationThread(
                threadId = threadId,
                address = address,
                displayName = contact?.displayName,
                snippet = body,
                timestampMs = date,
                unreadCount = if (isRead) 0 else 1,
                photoUri = contact?.photoUri,
              )
            } else if (!isRead) {
              byThreadKey[key] = existing.copy(unreadCount = existing.unreadCount + 1)
            }
          }
        }
      }
      byThreadKey.values.toList()
    }

  suspend fun loadThreadMessages(targetAddress: String): List<SmsMessageEntry> =
    withContext(Dispatchers.IO) {
      if (!hasPermission(Manifest.permission.READ_SMS)) return@withContext emptyList()
      val targetKey = ForkLoreNumberHelper.matchKey(targetAddress)
      val messages = ArrayList<SmsMessageEntry>()
      val projection = arrayOf(
        Telephony.Sms._ID,
        Telephony.Sms.THREAD_ID,
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE,
        Telephony.Sms.TYPE,
        Telephony.Sms.READ,
      )
      runCatching {
        resolver.query(
          Telephony.Sms.CONTENT_URI,
          projection,
          null,
          null,
          "${Telephony.Sms.DATE} ASC LIMIT 400",
        )?.use { cursor ->
          val idCol = cursor.getColumnIndex(Telephony.Sms._ID)
          val threadCol = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
          val addrCol = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
          val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
          val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)
          val typeCol = cursor.getColumnIndex(Telephony.Sms.TYPE)
          val readCol = cursor.getColumnIndex(Telephony.Sms.READ)

          while (cursor.moveToNext()) {
            val addr = if (addrCol >= 0) cursor.getString(addrCol).orEmpty() else ""
            val addrKey = ForkLoreNumberHelper.matchKey(addr)
            val matches = (targetKey.isNotEmpty() && addrKey == targetKey) ||
              addr.equals(targetAddress, ignoreCase = true)
            if (!matches) continue

            val type = if (typeCol >= 0) cursor.getInt(typeCol) else Telephony.Sms.MESSAGE_TYPE_INBOX
            messages.add(
              SmsMessageEntry(
                id = if (idCol >= 0) cursor.getLong(idCol) else 0L,
                threadId = if (threadCol >= 0) cursor.getLong(threadCol) else 0L,
                address = addr,
                body = if (bodyCol >= 0) cursor.getString(bodyCol).orEmpty() else "",
                timestampMs = if (dateCol >= 0) cursor.getLong(dateCol) else 0L,
                isIncoming = type == Telephony.Sms.MESSAGE_TYPE_INBOX,
                isRead = readCol < 0 || cursor.getInt(readCol) == 1,
              )
            )
          }
        }
      }
      messages
    }

  suspend fun markThreadRead(threadId: Long) = withContext(Dispatchers.IO) {
    if (threadId <= 0L) return@withContext
    runCatching {
      val values = ContentValues().apply {
        put(Telephony.Sms.READ, 1)
        put(Telephony.Sms.SEEN, 1)
      }
      resolver.update(
        Telephony.Sms.CONTENT_URI,
        values,
        "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
        arrayOf(threadId.toString()),
      )
    }
  }

  suspend fun sendSms(address: String, body: String): Result<Unit> = withContext(Dispatchers.IO) {
    if (address.isBlank() || body.isBlank()) {
      return@withContext Result.failure(IllegalArgumentException("Enter a number and message"))
    }
    if (!hasPermission(Manifest.permission.SEND_SMS)) {
      return@withContext Result.failure(SecurityException("SMS permission is required"))
    }
    runCatching {
      val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(SmsManager::class.java)
      } else {
        @Suppress("DEPRECATION")
        SmsManager.getDefault()
      }
      val parts = smsManager.divideMessage(body.trim())
      smsManager.sendMultipartTextMessage(address.trim(), null, parts, null, null)

      // Persist sent message when acting as Default SMS app
      runCatching {
        val values = ContentValues().apply {
          put(Telephony.Sms.ADDRESS, address.trim())
          put(Telephony.Sms.BODY, body.trim())
          put(Telephony.Sms.DATE, System.currentTimeMillis())
          put(Telephony.Sms.READ, 1)
          put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
        }
        resolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
      }
      Unit
    }
  }

  @SuppressLint("MissingPermission")
  fun placeCall(rawNumber: String): Boolean {
    val trimmed = rawNumber.trim()
    if (trimmed.isEmpty()) return false
    val uri = Uri.fromParts("tel", trimmed, null)

    if (hasPermission(Manifest.permission.CALL_PHONE)) {
      val placedViaTelecom = runCatching {
        val telecomManager = context.getSystemService(TelecomManager::class.java)
        telecomManager?.placeCall(uri, null)
        telecomManager != null
      }.getOrDefault(false)
      if (placedViaTelecom) return true
    }

    return runCatching {
      val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(dialIntent)
      true
    }.getOrDefault(false)
  }

  private fun hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
  }

  private class MutableContactBuilder(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val photoUri: String?,
    val isStarred: Boolean,
  ) {
    val numbers = ArrayList<String>()
    fun build(): ContactEntry = ContactEntry(
      id = id,
      lookupKey = lookupKey,
      displayName = displayName,
      primaryNumber = numbers.firstOrNull().orEmpty(),
      allNumbers = numbers.toList(),
      photoUri = photoUri,
      isStarred = isStarred,
    )
  }
}
