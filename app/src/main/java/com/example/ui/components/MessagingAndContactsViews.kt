package com.example.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ContactEntry
import com.example.model.ConversationThread
import com.example.model.SmsMessageEntry

@Composable
fun ConversationsListView(
  conversations: List<ConversationThread>,
  hasSmsPermission: Boolean,
  selectedAddress: String?,
  onOpenThread: (ConversationThread) -> Unit,
  onNewMessage: () -> Unit,
  onGrantPermissions: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    if (!hasSmsPermission) {
      PermissionEmptyCard(
        title = "SMS Messaging Permissions Needed",
        subtitle = "Set ForkLorE as Default SMS and allow SMS permissions to read and send text messages.",
        buttonLabel = stringResource(id = R.string.grant_permissions),
        onAction = onGrantPermissions,
      )
    } else if (conversations.isEmpty()) {
      PermissionEmptyCard(
        title = "No Messages Yet",
        subtitle = "Start a conversation with any contact or phone number.",
        buttonLabel = stringResource(id = R.string.new_message),
        onAction = onNewMessage,
      )
    } else {
      LazyColumn(
        contentPadding = PaddingValues(bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        items(conversations, key = { "${it.threadId}_${it.address}" }) { convo ->
          val title = convo.displayName ?: convo.address
          val relTime = DateUtils.getRelativeTimeSpanString(
            convo.timestampMs,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
          ).toString()
          val isSelected = selectedAddress != null && convo.address == selectedAddress

          Surface(
            color = if (isSelected) Color(0xAA172642) else Color(0x88101624),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(18.dp),
              )
              .clickable { onOpenThread(convo) },
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
              SquircleInitialAvatar(name = title, sizeDp = 46.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = if (convo.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = relTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.56f),
                  )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = convo.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = if (convo.unreadCount > 0) 0.92f else 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  if (convo.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    ) {
                      Text(
                        text = convo.unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      FloatingActionButton(
        onClick = onNewMessage,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = DefaultAppleSquircleShape,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(12.dp)
          .testTag("new_message_fab"),
      ) {
        Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = stringResource(id = R.string.new_message))
      }
    }
  }
}

@Composable
fun ChatThreadView(
  title: String,
  address: String,
  messages: List<SmsMessageEntry>,
  showBackButton: Boolean,
  onBack: () -> Unit,
  onCall: (String) -> Unit,
  onSendMessage: (String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var draftText by remember(address) { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("chat_thread_view"),
  ) {
    // Thread Header
    Surface(
      color = Color(0x99101726),
      shape = RoundedCornerShape(18.dp),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      ) {
        if (showBackButton) {
          IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        }
        SquircleInitialAvatar(name = title, sizeDp = 38.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (title != address) {
            Text(
              text = address,
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.62f),
            )
          }
        }
        IconButton(onClick = { onCall(address) }, modifier = Modifier.size(44.dp)) {
          Icon(Icons.Rounded.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.tertiary)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    LazyColumn(
      state = listState,
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(vertical = 8.dp),
      modifier = Modifier.weight(1f),
    ) {
      items(messages, key = { "${it.id}_${it.timestampMs}" }) { msg ->
        val alignEnd = !msg.isIncoming
        Row(
          horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Surface(
            color = if (alignEnd) {
              MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            } else {
              Color(0xAA161F33)
            },
            shape = RoundedCornerShape(
              topStart = 18.dp,
              topEnd = 18.dp,
              bottomStart = if (alignEnd) 18.dp else 6.dp,
              bottomEnd = if (alignEnd) 6.dp else 18.dp,
            ),
            modifier = Modifier
              .widthIn(max = 310.dp)
              .border(
                width = 1.dp,
                color = if (alignEnd) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(
                  topStart = 18.dp,
                  topEnd = 18.dp,
                  bottomStart = if (alignEnd) 18.dp else 6.dp,
                  bottomEnd = if (alignEnd) 6.dp else 18.dp,
                ),
              ),
          ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
              Text(
                text = msg.body,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
              )
              Spacer(modifier = Modifier.height(3.dp))
              val rel = DateUtils.getRelativeTimeSpanString(
                msg.timestampMs,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
              ).toString()
              Text(
                text = rel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.52f),
              )
            }
          }
        }
      }
    }

    // Message Composer
    Surface(
      color = Color(0xAA121A2B),
      shape = CircleShape,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
      ) {
        Box(modifier = Modifier.weight(1f)) {
          if (draftText.isEmpty()) {
            Text(
              text = stringResource(id = R.string.type_message_hint),
              style = MaterialTheme.typography.bodyLarge,
              color = Color.White.copy(alpha = 0.45f),
            )
          }
          BasicTextField(
            value = draftText,
            onValueChange = { draftText = it },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
            maxLines = 4,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("sms_composer_input"),
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        FilledIconButton(
          onClick = {
            val text = draftText.trim()
            if (text.isNotEmpty()) {
              onSendMessage(address, text)
              draftText = ""
            }
          },
          enabled = draftText.isNotBlank(),
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier.size(44.dp).testTag("sms_send_button"),
        ) {
          Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
        }
      }
    }
  }
}

@Composable
fun ContactsListView(
  contacts: List<ContactEntry>,
  hasPermission: Boolean,
  selectedContactId: Long?,
  onSelectContact: (ContactEntry) -> Unit,
  onCall: (String) -> Unit,
  onMessage: (String, String) -> Unit,
  onToggleStar: (ContactEntry) -> Unit,
  onAddContact: () -> Unit,
  onGrantPermissions: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    if (!hasPermission) {
      PermissionEmptyCard(
        title = "Contacts Permission Needed",
        subtitle = "Allow Contacts access to view, favorite, call, message, and save contacts in ForkLorE.",
        buttonLabel = stringResource(id = R.string.grant_permissions),
        onAction = onGrantPermissions,
      )
    } else if (contacts.isEmpty()) {
      PermissionEmptyCard(
        title = "No Contacts Found",
        subtitle = "Add your first contact to call and text effortlessly.",
        buttonLabel = stringResource(id = R.string.new_contact),
        onAction = onAddContact,
      )
    } else {
      LazyColumn(
        contentPadding = PaddingValues(bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        items(contacts, key = { it.id }) { contact ->
          val isSelected = selectedContactId == contact.id
          Surface(
            color = if (isSelected) Color(0xAA172642) else Color(0x88101624),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(18.dp),
              )
              .clickable { onSelectContact(contact) },
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
              SquircleInitialAvatar(name = contact.displayName, sizeDp = 44.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = contact.displayName,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = contact.primaryNumber,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.65f),
                )
              }
              IconButton(onClick = { onToggleStar(contact) }, modifier = Modifier.size(38.dp)) {
                Icon(
                  imageVector = if (contact.isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                  contentDescription = "Favorite",
                  tint = if (contact.isStarred) Color(0xFFFACC15) else Color.White.copy(alpha = 0.48f),
                  modifier = Modifier.size(19.dp),
                )
              }
              IconButton(onClick = { onMessage(contact.primaryNumber, contact.displayName) }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = "Message", tint = Color.White.copy(alpha = 0.80f), modifier = Modifier.size(18.dp))
              }
              IconButton(onClick = { onCall(contact.primaryNumber) }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
              }
            }
          }
        }
      }

      FloatingActionButton(
        onClick = onAddContact,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = DefaultAppleSquircleShape,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(12.dp)
          .testTag("add_contact_fab"),
      ) {
        Icon(Icons.Rounded.Add, contentDescription = stringResource(id = R.string.new_contact))
      }
    }
  }
}

@Composable
fun NewMessageDialog(
  onDismiss: () -> Unit,
  onStartChat: (String, String?) -> Unit,
) {
  var numberInput by remember { mutableStateOf("") }
  var messageInput by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(id = R.string.new_message)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = numberInput,
          onValueChange = { numberInput = it },
          label = { Text("Phone number") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = messageInput,
          onValueChange = { messageInput = it },
          label = { Text("Initial message (optional)") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (numberInput.isNotBlank()) {
            onStartChat(numberInput.trim(), messageInput.trim().takeIf { it.isNotEmpty() })
          }
        },
      ) {
        Text("Open Chat")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun AddContactDialog(
  prefillPhone: String,
  onDismiss: () -> Unit,
  onSave: (String, String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var phone by remember(prefillPhone) { mutableStateOf(prefillPhone) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(id = R.string.new_contact)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone number") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (name.isNotBlank() && phone.isNotBlank()) {
            onSave(name.trim(), phone.trim())
          }
        },
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
