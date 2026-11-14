package com.example.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.display.ForkLoreDisplayProfile
import com.example.model.CallDirection
import com.example.model.CallLogEntry
import com.example.model.ContactEntry
import com.example.model.ForkLoreNumberHelper

private data class DialKey(val digit: Char, val letters: String)

private val dialKeys = listOf(
  listOf(DialKey('1', ""), DialKey('2', "ABC"), DialKey('3', "DEF")),
  listOf(DialKey('4', "GHI"), DialKey('5', "JKL"), DialKey('6', "MNO")),
  listOf(DialKey('7', "PQRS"), DialKey('8', "TUV"), DialKey('9', "WXYZ")),
  listOf(DialKey('*', ""), DialKey('0', "+"), DialKey('#', "")),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadPaneView(
  digits: String,
  matches: List<ContactEntry>,
  profile: ForkLoreDisplayProfile,
  onAppendChar: (Char) -> Unit,
  onBackspace: () -> Unit,
  onClear: () -> Unit,
  onCall: (String) -> Unit,
  onMessage: (String) -> Unit,
  onAddContact: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current

  Column(
    verticalArrangement = Arrangement.Bottom,
    modifier = modifier
      .fillMaxSize()
      .padding(bottom = 12.dp),
  ) {
    // T9 & Number Matches above dialpad
    if (matches.isNotEmpty()) {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
      ) {
        items(matches, key = { it.id }) { contact ->
          Surface(
            color = Color(0x88101624),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
              .clickable { onCall(contact.primaryNumber) },
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
              SquircleInitialAvatar(name = contact.displayName, sizeDp = 38.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = contact.displayName,
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = contact.primaryNumber,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              IconButton(onClick = { onMessage(contact.primaryNumber) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = "Text", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    } else {
      Spacer(modifier = Modifier.weight(1f))
      if (digits.isNotBlank()) {
        Row(
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
          Surface(
            color = Color.White.copy(alpha = 0.08f),
            shape = CircleShape,
            modifier = Modifier
              .clip(CircleShape)
              .clickable { onAddContact(digits) },
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
              Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save Contact", style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
          }
        }
      }
    }

    // Entered Number Bar
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
    ) {
      Text(
        text = digits.ifEmpty { "Enter number" },
        style = MaterialTheme.typography.displayLarge.copy(
          fontSize = if (digits.length > 12) MaterialTheme.typography.headlineMedium.fontSize else MaterialTheme.typography.displayLarge.fontSize * 0.78f,
          fontWeight = FontWeight.SemiBold,
        ),
        color = if (digits.isEmpty()) Color.White.copy(alpha = 0.32f) else Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .weight(1f)
          .testTag("dialpad_number_display"),
      )
      if (digits.isNotEmpty()) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onBackspace()
              },
              onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClear()
              },
            )
            .testTag("dialpad_backspace"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Backspace,
            contentDescription = "Backspace",
            tint = Color.White.copy(alpha = 0.78f),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Frosted Squircle Keypad Grid
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      dialKeys.forEach { row ->
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          row.forEach { key ->
            Surface(
              color = Color(0x88101624),
              shape = RoundedCornerShape(22.dp),
              modifier = Modifier
                .weight(1f)
                .height(profile.dialpadButtonHeightDp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                .combinedClickable(
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAppendChar(key.digit)
                  },
                  onLongClick = {
                    if (key.digit == '0') {
                      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                      onAppendChar('+')
                    }
                  },
                )
                .testTag("dialpad_key_${key.digit}"),
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                Text(
                  text = key.digit.toString(),
                  style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = profile.dialpadDigitFontSp,
                    fontWeight = FontWeight.SemiBold,
                  ),
                  color = Color.White,
                )
                if (key.letters.isNotEmpty() && !profile.isCompactHeight) {
                  Text(
                    text = key.letters,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.50f),
                  )
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Call & Quick Message Buttons
    Row(
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth(),
    ) {
      IconButton(
        onClick = { if (digits.isNotBlank()) onMessage(digits) },
        enabled = digits.isNotBlank(),
        modifier = Modifier
          .size(52.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = if (digits.isNotBlank()) 0.12f else 0.04f))
          .testTag("dialpad_message_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.Message,
          contentDescription = "Send SMS",
          tint = if (digits.isNotBlank()) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.30f),
        )
      }

      Spacer(modifier = Modifier.width(24.dp))

      FilledIconButton(
        onClick = { onCall(digits) },
        enabled = digits.isNotBlank(),
        colors = IconButtonDefaults.filledIconButtonColors(
          containerColor = Color(0xFF10B981),
          contentColor = Color.White,
          disabledContainerColor = Color(0x4410B981),
          disabledContentColor = Color.White.copy(alpha = 0.45f),
        ),
        modifier = Modifier
          .size(width = 88.dp, height = 58.dp)
          .clip(DefaultAppleSquircleShape)
          .testTag("dialpad_call_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Call,
          contentDescription = "Call",
          modifier = Modifier.size(28.dp),
        )
      }

      Spacer(modifier = Modifier.width(76.dp))
    }
  }
}

@Composable
fun RecentsListView(
  callLogs: List<CallLogEntry>,
  hasPermission: Boolean,
  onCall: (String) -> Unit,
  onMessage: (String, String?) -> Unit,
  onSaveContact: (String) -> Unit,
  onGrantPermissions: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!hasPermission) {
    PermissionEmptyCard(
      title = "Call History Unavailable",
      subtitle = "Allow Phone and Call Log access to see your recent calls and place calls directly inside ForkLorE.",
      buttonLabel = "Grant Permissions",
      onAction = onGrantPermissions,
      modifier = modifier,
    )
    return
  }

  if (callLogs.isEmpty()) {
    PermissionEmptyCard(
      title = "No Recent Calls",
      subtitle = "Your incoming, outgoing, and missed calls will appear here automatically.",
      buttonLabel = null,
      onAction = {},
      modifier = modifier,
    )
    return
  }

  LazyColumn(
    contentPadding = PaddingValues(bottom = 18.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.fillMaxSize(),
  ) {
    items(callLogs, key = { it.id }) { entry ->
      val (dirIcon, dirColor, dirText) = when (entry.direction) {
        CallDirection.OUTGOING -> Triple(Icons.AutoMirrored.Rounded.CallMade, MaterialTheme.colorScheme.primary, "Outgoing")
        CallDirection.INCOMING -> Triple(Icons.AutoMirrored.Rounded.CallReceived, MaterialTheme.colorScheme.tertiary, "Incoming")
        CallDirection.MISSED -> Triple(Icons.AutoMirrored.Rounded.CallMissed, MaterialTheme.colorScheme.error, "Missed")
        CallDirection.REJECTED -> Triple(Icons.AutoMirrored.Rounded.CallMissed, MaterialTheme.colorScheme.error, "Declined")
        CallDirection.BLOCKED -> Triple(Icons.Rounded.Block, Color.White.copy(alpha = 0.55f), "Blocked")
      }
      val label = entry.displayName ?: entry.number
      val relTime = DateUtils.getRelativeTimeSpanString(
        entry.timestampMs,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
      ).toString()

      Surface(
        color = Color(0x88101624),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
          .clickable { onCall(entry.number) },
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
          SquircleInitialAvatar(name = label, sizeDp = 44.dp)
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = label,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              color = if (entry.direction == CallDirection.MISSED) MaterialTheme.colorScheme.error else Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(dirIcon, contentDescription = null, tint = dirColor, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              val durSuffix = if (entry.durationSec > 0L) " (${ForkLoreNumberHelper.formatCallDuration(entry.durationSec)})" else ""
              Text(
                text = "$dirText$durSuffix • $relTime",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          if (entry.displayName == null) {
            IconButton(onClick = { onSaveContact(entry.number) }, modifier = Modifier.size(40.dp)) {
              Icon(Icons.Rounded.PersonAdd, contentDescription = "Save Contact", tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(18.dp))
            }
          }
          IconButton(onClick = { onMessage(entry.number, entry.displayName) }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = "Message", tint = Color.White.copy(alpha = 0.80f), modifier = Modifier.size(18.dp))
          }
          IconButton(onClick = { onCall(entry.number) }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
          }
        }
      }
    }
  }
}

@Composable
fun SquircleInitialAvatar(
  name: String,
  sizeDp: androidx.compose.ui.unit.Dp,
) {
  val initial = name.trimStart().firstOrNull()?.uppercaseChar()?.toString() ?: "#"
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .size(sizeDp)
      .clip(DefaultAppleSquircleShape)
      .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
      .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), DefaultAppleSquircleShape),
  ) {
    Text(
      text = initial,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.primary,
    )
  }
}

@Composable
fun PermissionEmptyCard(
  title: String,
  subtitle: String,
  buttonLabel: String?,
  onAction: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.fillMaxSize().padding(16.dp),
  ) {
    Surface(
      color = Color(0x8A101624),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
          color = Color.White,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.72f),
          textAlign = TextAlign.Center,
        )
        if (buttonLabel != null) {
          Spacer(modifier = Modifier.height(16.dp))
          androidx.compose.material3.Button(
            onClick = onAction,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.heightIn(min = 48.dp),
          ) {
            Text(buttonLabel)
          }
        }
      }
    }
  }
}
