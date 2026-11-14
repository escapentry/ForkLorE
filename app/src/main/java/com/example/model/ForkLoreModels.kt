package com.example.model

import androidx.compose.runtime.Immutable
import java.util.Locale

enum class ForkLoreTab {
  KEYPAD,
  RECENTS,
  MESSAGES,
  CONTACTS,
}

enum class CallDirection {
  INCOMING,
  OUTGOING,
  MISSED,
  REJECTED,
  BLOCKED,
}

@Immutable
data class ContactEntry(
  val id: Long,
  val lookupKey: String,
  val displayName: String,
  val primaryNumber: String,
  val allNumbers: List<String>,
  val photoUri: String?,
  val isStarred: Boolean,
)

@Immutable
data class CallLogEntry(
  val id: Long,
  val number: String,
  val displayName: String?,
  val direction: CallDirection,
  val timestampMs: Long,
  val durationSec: Long,
)

@Immutable
data class SmsMessageEntry(
  val id: Long,
  val threadId: Long,
  val address: String,
  val body: String,
  val timestampMs: Long,
  val isIncoming: Boolean,
  val isRead: Boolean,
)

@Immutable
data class ConversationThread(
  val threadId: Long,
  val address: String,
  val displayName: String?,
  val snippet: String,
  val timestampMs: Long,
  val unreadCount: Int,
  val photoUri: String?,
)

@Immutable
data class DefaultHandlerStatus(
  val isDefaultDialer: Boolean = false,
  val isDefaultSms: Boolean = false,
  val hasContactsPermission: Boolean = false,
  val hasPhonePermissions: Boolean = false,
  val hasSmsPermissions: Boolean = false,
) {
  val allConfigured: Boolean
    get() = isDefaultDialer && isDefaultSms && hasContactsPermission && hasPhonePermissions && hasSmsPermissions
}

object ForkLoreNumberHelper {

  fun normalizeDigits(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
      if (ch.isDigit()) sb.append(ch)
    }
    return sb.toString()
  }

  fun matchKey(phone: String): String {
    val digits = normalizeDigits(phone)
    return if (digits.length > 10) digits.takeLast(10) else digits
  }

  fun numbersMatch(a: String?, b: String?): Boolean {
    if (a.isNullOrBlank() || b.isNullOrBlank()) return false
    val ka = matchKey(a)
    val kb = matchKey(b)
    return ka.isNotEmpty() && ka == kb
  }

  fun toT9Digits(text: String): String {
    val sb = StringBuilder(text.length)
    for (ch in text.lowercase(Locale.US)) {
      val digit = when (ch) {
        'a', 'b', 'c' -> '2'
        'd', 'e', 'f' -> '3'
        'g', 'h', 'i' -> '4'
        'j', 'k', 'l' -> '5'
        'm', 'n', 'o' -> '6'
        'p', 'q', 'r', 's' -> '7'
        't', 'u', 'v' -> '8'
        'w', 'x', 'y', 'z' -> '9'
        in '0'..'9' -> ch
        else -> null
      }
      if (digit != null) sb.append(digit)
    }
    return sb.toString()
  }

  fun matchesDialpadQuery(contact: ContactEntry, dialpadInput: String): Boolean {
    val queryDigits = normalizeDigits(dialpadInput)
    if (queryDigits.isEmpty()) return false
    val numberMatch = contact.allNumbers.any { normalizeDigits(it).contains(queryDigits) }
    if (numberMatch) return true
    return toT9Digits(contact.displayName).contains(queryDigits)
  }

  fun formatCallDuration(seconds: Long): String {
    if (seconds <= 0L) return "0s"
    val mins = seconds / 60L
    val rem = seconds % 60L
    val hrs = mins / 60L
    val remMins = mins % 60L
    return when {
      hrs > 0L -> "${hrs}h ${remMins}m"
      mins > 0L -> "${mins}m ${rem}s"
      else -> "${rem}s"
    }
  }
}
