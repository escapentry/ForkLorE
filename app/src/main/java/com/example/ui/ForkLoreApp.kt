package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneEnabled
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.display.DisplayAutoScaler
import com.example.display.ForkLoreDisplayProfile
import com.example.model.ContactEntry
import com.example.model.ForkLoreNumberHelper
import com.example.model.ForkLoreTab
import com.example.telecom.ActiveCallUiState
import com.example.telecom.CallActivity
import com.example.ui.components.AIOMediaAtmosphericBackground
import com.example.ui.components.AddContactDialog
import com.example.ui.components.BackdropThemePickerSheetContent
import com.example.ui.components.ChatThreadView
import com.example.ui.components.ContactsListView
import com.example.ui.components.ConversationsListView
import com.example.ui.components.ForkLoreLoadingSplash
import com.example.ui.components.KeypadPaneView
import com.example.ui.components.NewMessageDialog
import com.example.ui.components.RecentsListView
import com.example.ui.components.SquircleInitialAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForkLoreApp(viewModel: ForkLoreViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val density = LocalDensity.current

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) {
    viewModel.refreshAllData()
  }

  val roleLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
  ) {
    viewModel.refreshAllData()
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      viewModel.saveCustomWallpaper(uri)
    }
  }

  val launchPermissions = {
    permissionLauncher.launch(viewModel.repository.requiredRuntimePermissions())
  }
  val launchDefaultDialer = {
    viewModel.repository.createDefaultDialerIntent()?.let { roleLauncher.launch(it) }
    Unit
  }
  val launchDefaultSms = {
    viewModel.repository.createDefaultSmsIntent()?.let { roleLauncher.launch(it) }
    Unit
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF030E2C)),
  ) {
    AIOMediaAtmosphericBackground(wallpaperState = uiState.wallpaperState)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val profile = DisplayAutoScaler.computeProfile(
        availableWidthDp = maxWidth.value,
        availableHeightDp = maxHeight.value,
        density = density.density,
        systemFontScale = density.fontScale,
      )

      val inCompactChatThread =
        !profile.useTwoPaneLayout &&
          uiState.activeTab == ForkLoreTab.MESSAGES &&
          uiState.openChatAddress != null

      BackHandler(enabled = inCompactChatThread) {
        viewModel.closeChat()
      }

      val showBottomBar = !profile.useNavigationRail && !inCompactChatThread

      Row(modifier = Modifier.fillMaxSize()) {
        if (profile.useNavigationRail) {
          ForkLoreNavRail(
            activeTab = uiState.activeTab,
            onSelectTab = viewModel::selectTab,
          )
        }

        Scaffold(
          containerColor = Color.Transparent,
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
          bottomBar = {
            if (showBottomBar) {
              ForkLoreBottomBar(
                activeTab = uiState.activeTab,
                onSelectTab = viewModel::selectTab,
              )
            }
          },
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        ) { innerPadding ->
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
              .statusBarsPadding()
              .then(if (!showBottomBar) Modifier.navigationBarsPadding() else Modifier)
              .imePadding()
              .padding(horizontal = profile.horizontalPaddingDp),
          ) {
            if (!inCompactChatThread) {
              ForkLoreTopHeader(
                uiState = uiState,
                onOpenThemePicker = viewModel::openWallpaperMenu,
                onRequestDefaultPhone = launchDefaultDialer,
                onRequestDefaultSms = launchDefaultSms,
                onGrantPermissions = launchPermissions,
              )
            } else {
              Spacer(modifier = Modifier.height(6.dp))
            }

            uiState.activeCall?.let { call ->
              ActiveCallBanner(
                call = call,
                onClick = {
                  val intent = Intent(context, CallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  }
                  context.startActivity(intent)
                },
              )
              Spacer(modifier = Modifier.height(8.dp))
            }

            uiState.statusBanner?.let { banner ->
              StatusBannerCard(
                message = banner,
                onDismiss = viewModel::dismissStatusBanner,
              )
              Spacer(modifier = Modifier.height(8.dp))
            }

            if (!inCompactChatThread && uiState.activeTab != ForkLoreTab.KEYPAD) {
              ForkLoreSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
              )
              Spacer(modifier = Modifier.height(10.dp))
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            ) {
              ForkLoreTabContent(
                uiState = uiState,
                profile = profile,
                viewModel = viewModel,
                onGrantPermissions = launchPermissions,
              )
            }
          }
        }
      }
    }

    if (uiState.isNewMessageDialogOpen) {
      NewMessageDialog(
        onDismiss = viewModel::closeNewMessageDialog,
        onStartChat = { address, initialMsg ->
          viewModel.openChat(address)
          if (!initialMsg.isNullOrBlank()) {
            viewModel.sendMessage(address, initialMsg)
          }
        },
      )
    }

    if (uiState.isAddContactDialogOpen) {
      AddContactDialog(
        prefillPhone = uiState.addContactPrefillPhone,
        onDismiss = viewModel::closeAddContactDialog,
        onSave = viewModel::saveNewContact,
      )
    }

    val selectedContact = uiState.selectedContact
    if (selectedContact != null && !uiState.isInitialLoading) {
      ContactQuickSheetDialog(
        contact = selectedContact,
        onDismiss = { viewModel.selectContact(null) },
        onCall = { number ->
          viewModel.selectContact(null)
          viewModel.placeCall(number)
        },
        onMessage = { number, name ->
          viewModel.selectContact(null)
          viewModel.openChat(number, name)
        },
        onToggleStar = { viewModel.toggleContactStar(selectedContact) },
      )
    }

    if (uiState.isWallpaperMenuOpen) {
      val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
      ModalBottomSheet(
        onDismissRequest = viewModel::dismissWallpaperMenu,
        sheetState = sheetState,
        containerColor = Color(0xEE0B111E),
      ) {
        BackdropThemePickerSheetContent(
          wallpaperState = uiState.wallpaperState,
          onSelectPreset = viewModel::selectWallpaperPreset,
          onChooseCustomPhoto = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
        )
      }
    }

    AnimatedVisibility(
      visible = uiState.isInitialLoading,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      ForkLoreLoadingSplash()
    }
  }
}

@Composable
private fun ForkLoreTopHeader(
  uiState: ForkLoreUiState,
  onOpenThemePicker: () -> Unit,
  onRequestDefaultPhone: () -> Unit,
  onRequestDefaultSms: () -> Unit,
  onGrantPermissions: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = stringResource(id = R.string.app_name),
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
      )
      IconButton(
        onClick = onOpenThemePicker,
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.08f))
          .testTag("theme_picker_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Palette,
          contentDescription = stringResource(id = R.string.wallpaper_menu_title),
          tint = Color.White,
        )
      }
    }

    val status = uiState.roleStatus
    val needsPermissions =
      !status.hasContactsPermission || !status.hasPhonePermissions || !status.hasSmsPermissions

    if (!status.allConfigured) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
      ) {
        if (!status.isDefaultDialer) {
          SetupActionChip(
            icon = Icons.Rounded.PhoneEnabled,
            label = stringResource(id = R.string.set_default_phone),
            accent = MaterialTheme.colorScheme.primary,
            onClick = onRequestDefaultPhone,
            testTag = "set_default_phone_chip",
          )
        }
        if (!status.isDefaultSms) {
          SetupActionChip(
            icon = Icons.Rounded.Sms,
            label = stringResource(id = R.string.set_default_sms),
            accent = MaterialTheme.colorScheme.secondary,
            onClick = onRequestDefaultSms,
            testTag = "set_default_sms_chip",
          )
        }
        if (needsPermissions) {
          SetupActionChip(
            icon = Icons.Rounded.Security,
            label = stringResource(id = R.string.grant_permissions),
            accent = MaterialTheme.colorScheme.tertiary,
            onClick = onGrantPermissions,
            testTag = "grant_permissions_chip",
          )
        }
      }
    }
  }
}

@Composable
private fun SetupActionChip(
  icon: ImageVector,
  label: String,
  accent: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    color = accent.copy(alpha = 0.16f),
    shape = CircleShape,
    modifier = Modifier
      .heightIn(min = 38.dp)
      .clip(CircleShape)
      .border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
      .clickable(onClick = onClick)
      .testTag(testTag),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
      Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun ActiveCallBanner(
  call: ActiveCallUiState,
  onClick: () -> Unit,
) {
  Surface(
    color = Color(0xCC065F46),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .border(1.dp, Color(0xFF34D399).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag("active_call_banner"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
      Icon(Icons.Rounded.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = call.displayName,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = if (call.isConnected) {
            "Ongoing • ${ForkLoreNumberHelper.formatCallDuration(call.elapsedSeconds)}"
          } else {
            call.stateLabel
          },
          style = MaterialTheme.typography.labelSmall,
          color = Color.White.copy(alpha = 0.85f),
        )
      }
      Text(
        text = "Tap to open",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
      )
    }
  }
}

@Composable
private fun StatusBannerCard(
  message: String,
  onDismiss: () -> Unit,
) {
  Surface(
    color = Color(0xB8182238),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
    ) {
      Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
      }
    }
  }
}

@Composable
private fun ForkLoreSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
) {
  Surface(
    color = Color(0x88101624),
    shape = CircleShape,
    modifier = Modifier
      .fillMaxWidth()
      .height(48.dp)
      .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 14.dp),
    ) {
      Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Box(modifier = Modifier.weight(1f)) {
        if (query.isEmpty()) {
          Text(
            text = stringResource(id = R.string.search_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.45f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("search_input"),
        )
      }
      if (query.isNotEmpty()) {
        IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
          Icon(
            Icons.Rounded.Close,
            contentDescription = stringResource(id = R.string.clear_search),
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun ForkLoreTabContent(
  uiState: ForkLoreUiState,
  profile: ForkLoreDisplayProfile,
  viewModel: ForkLoreViewModel,
  onGrantPermissions: () -> Unit,
) {
  if (profile.useTwoPaneLayout) {
    when (uiState.activeTab) {
      ForkLoreTab.KEYPAD -> {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxSize()) {
          KeypadPaneView(
            digits = uiState.dialpadDigits,
            matches = uiState.dialpadMatches,
            profile = profile,
            onAppendChar = viewModel::appendDialpadChar,
            onBackspace = viewModel::backspaceDialpad,
            onClear = viewModel::clearDialpad,
            onCall = viewModel::placeCall,
            onMessage = { viewModel.openChat(it) },
            onAddContact = viewModel::openAddContactDialog,
            modifier = Modifier.weight(0.48f),
          )
          RecentsListView(
            callLogs = uiState.filteredCallLogs,
            hasPermission = uiState.roleStatus.hasPhonePermissions,
            onCall = viewModel::placeCall,
            onMessage = viewModel::openChat,
            onSaveContact = viewModel::openAddContactDialog,
            onGrantPermissions = onGrantPermissions,
            modifier = Modifier.weight(0.52f),
          )
        }
      }

      ForkLoreTab.RECENTS -> {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxSize()) {
          RecentsListView(
            callLogs = uiState.filteredCallLogs,
            hasPermission = uiState.roleStatus.hasPhonePermissions,
            onCall = viewModel::placeCall,
            onMessage = viewModel::openChat,
            onSaveContact = viewModel::openAddContactDialog,
            onGrantPermissions = onGrantPermissions,
            modifier = Modifier.weight(0.56f),
          )
          KeypadPaneView(
            digits = uiState.dialpadDigits,
            matches = uiState.dialpadMatches,
            profile = profile,
            onAppendChar = viewModel::appendDialpadChar,
            onBackspace = viewModel::backspaceDialpad,
            onClear = viewModel::clearDialpad,
            onCall = viewModel::placeCall,
            onMessage = { viewModel.openChat(it) },
            onAddContact = viewModel::openAddContactDialog,
            modifier = Modifier.weight(0.44f),
          )
        }
      }

      ForkLoreTab.MESSAGES -> {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxSize()) {
          ConversationsListView(
            conversations = uiState.filteredConversations,
            hasSmsPermission = uiState.roleStatus.hasSmsPermissions,
            selectedAddress = uiState.openChatAddress,
            onOpenThread = { viewModel.openChat(it.address, it.displayName) },
            onNewMessage = viewModel::openNewMessageDialog,
            onGrantPermissions = onGrantPermissions,
            modifier = Modifier.weight(0.42f),
          )
          Box(modifier = Modifier.weight(0.58f).fillMaxHeight()) {
            val openAddr = uiState.openChatAddress
            if (openAddr != null) {
              ChatThreadView(
                title = uiState.openChatTitle ?: openAddr,
                address = openAddr,
                messages = uiState.openChatMessages,
                showBackButton = false,
                onBack = viewModel::closeChat,
                onCall = viewModel::placeCall,
                onSendMessage = viewModel::sendMessage,
              )
            } else {
              EmptyDetailCard(
                icon = Icons.AutoMirrored.Rounded.Message,
                title = "Select a Conversation",
                subtitle = "Choose any SMS thread on the left or tap New Message to start texting.",
              )
            }
          }
        }
      }

      ForkLoreTab.CONTACTS -> {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxSize()) {
          ContactsListView(
            contacts = uiState.filteredContacts,
            hasPermission = uiState.roleStatus.hasContactsPermission,
            selectedContactId = uiState.selectedContact?.id,
            onSelectContact = viewModel::selectContact,
            onCall = viewModel::placeCall,
            onMessage = viewModel::openChat,
            onToggleStar = viewModel::toggleContactStar,
            onAddContact = { viewModel.openAddContactDialog() },
            onGrantPermissions = onGrantPermissions,
            modifier = Modifier.weight(0.5f),
          )
          Box(modifier = Modifier.weight(0.5f).fillMaxHeight()) {
            val contact = uiState.selectedContact
            if (contact != null) {
              ContactDetailCard(
                contact = contact,
                onCall = viewModel::placeCall,
                onMessage = { num -> viewModel.openChat(num, contact.displayName) },
                onToggleStar = { viewModel.toggleContactStar(contact) },
              )
            } else {
              EmptyDetailCard(
                icon = Icons.Rounded.Contacts,
                title = "Select a Contact",
                subtitle = "Tap any contact on the left to inspect all phone numbers, call, or send an SMS.",
              )
            }
          }
        }
      }
    }
    return
  }

  // Single-pane adaptive layouts (phones & foldables)
  Box(
    contentAlignment = Alignment.TopCenter,
    modifier = Modifier.fillMaxSize(),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 620.dp),
    ) {
      when (uiState.activeTab) {
        ForkLoreTab.KEYPAD -> KeypadPaneView(
          digits = uiState.dialpadDigits,
          matches = uiState.dialpadMatches,
          profile = profile,
          onAppendChar = viewModel::appendDialpadChar,
          onBackspace = viewModel::backspaceDialpad,
          onClear = viewModel::clearDialpad,
          onCall = viewModel::placeCall,
          onMessage = { viewModel.openChat(it) },
          onAddContact = viewModel::openAddContactDialog,
        )

        ForkLoreTab.RECENTS -> RecentsListView(
          callLogs = uiState.filteredCallLogs,
          hasPermission = uiState.roleStatus.hasPhonePermissions,
          onCall = viewModel::placeCall,
          onMessage = viewModel::openChat,
          onSaveContact = viewModel::openAddContactDialog,
          onGrantPermissions = onGrantPermissions,
        )

        ForkLoreTab.MESSAGES -> {
          val openAddr = uiState.openChatAddress
          if (openAddr != null) {
            ChatThreadView(
              title = uiState.openChatTitle ?: openAddr,
              address = openAddr,
              messages = uiState.openChatMessages,
              showBackButton = true,
              onBack = viewModel::closeChat,
              onCall = viewModel::placeCall,
              onSendMessage = viewModel::sendMessage,
            )
          } else {
            ConversationsListView(
              conversations = uiState.filteredConversations,
              hasSmsPermission = uiState.roleStatus.hasSmsPermissions,
              selectedAddress = null,
              onOpenThread = { viewModel.openChat(it.address, it.displayName) },
              onNewMessage = viewModel::openNewMessageDialog,
              onGrantPermissions = onGrantPermissions,
            )
          }
        }

        ForkLoreTab.CONTACTS -> ContactsListView(
          contacts = uiState.filteredContacts,
          hasPermission = uiState.roleStatus.hasContactsPermission,
          selectedContactId = uiState.selectedContact?.id,
          onSelectContact = viewModel::selectContact,
          onCall = viewModel::placeCall,
          onMessage = viewModel::openChat,
          onToggleStar = viewModel::toggleContactStar,
          onAddContact = { viewModel.openAddContactDialog() },
          onGrantPermissions = onGrantPermissions,
        )
      }
    }
  }
}

@Composable
private fun ContactDetailCard(
  contact: ContactEntry,
  onCall: (String) -> Unit,
  onMessage: (String) -> Unit,
  onToggleStar: () -> Unit,
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
      SquircleInitialAvatar(name = contact.displayName, sizeDp = 76.dp)
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = contact.displayName,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
      )
      Spacer(modifier = Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { onCall(contact.primaryNumber) }, shape = RoundedCornerShape(14.dp)) {
          Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Call")
        }
        OutlinedButton(onClick = { onMessage(contact.primaryNumber) }, shape = RoundedCornerShape(14.dp)) {
          Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Text")
        }
        IconButton(onClick = onToggleStar) {
          Icon(
            imageVector = if (contact.isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = "Favorite",
            tint = if (contact.isStarred) Color(0xFFFACC15) else Color.White.copy(alpha = 0.65f),
          )
        }
      }
      Spacer(modifier = Modifier.height(18.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        contact.allNumbers.forEach { number ->
          Surface(
            color = Color.White.copy(alpha = 0.06f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .clickable { onCall(number) },
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
              Text(text = number, style = MaterialTheme.typography.bodyLarge, color = Color.White, modifier = Modifier.weight(1f))
              IconButton(onClick = { onMessage(number) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = "Text $number", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
              }
              IconButton(onClick = { onCall(number) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Call, contentDescription = "Call $number", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ContactQuickSheetDialog(
  contact: ContactEntry,
  onDismiss: () -> Unit,
  onCall: (String) -> Unit,
  onMessage: (String, String) -> Unit,
  onToggleStar: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        SquircleInitialAvatar(name = contact.displayName, sizeDp = 42.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = contact.displayName, modifier = Modifier.weight(1f))
        IconButton(onClick = onToggleStar) {
          Icon(
            imageVector = if (contact.isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = "Favorite",
            tint = if (contact.isStarred) Color(0xFFFACC15) else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        contact.allNumbers.forEach { number ->
          Surface(
            color = Color.White.copy(alpha = 0.06f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
              Text(text = number, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
              IconButton(onClick = { onMessage(number, contact.displayName) }) {
                Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = "Text $number")
              }
              IconButton(onClick = { onCall(number) }) {
                Icon(Icons.Rounded.Call, contentDescription = "Call $number", tint = MaterialTheme.colorScheme.tertiary)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}

@Composable
private fun EmptyDetailCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
    Surface(
      color = Color(0x66101624),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier
        .widthIn(max = 360.dp)
        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp)),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(28.dp),
      ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.65f))
      }
    }
  }
}

private data class NavItemSpec(
  val tab: ForkLoreTab,
  val labelRes: Int,
  val icon: ImageVector,
  val testTag: String,
)

private val navItems = listOf(
  NavItemSpec(ForkLoreTab.KEYPAD, R.string.nav_keypad, Icons.Rounded.Dialpad, "nav_keypad"),
  NavItemSpec(ForkLoreTab.RECENTS, R.string.nav_recents, Icons.Rounded.History, "nav_recents"),
  NavItemSpec(ForkLoreTab.MESSAGES, R.string.nav_messages, Icons.AutoMirrored.Rounded.Message, "nav_messages"),
  NavItemSpec(ForkLoreTab.CONTACTS, R.string.nav_contacts, Icons.Rounded.Contacts, "nav_contacts"),
)

@Composable
private fun ForkLoreBottomBar(
  activeTab: ForkLoreTab,
  onSelectTab: (ForkLoreTab) -> Unit,
) {
  NavigationBar(
    containerColor = Color(0xCC0A101D),
    contentColor = Color.White,
  ) {
    navItems.forEach { item ->
      val selected = activeTab == item.tab
      NavigationBarItem(
        selected = selected,
        onClick = { onSelectTab(item.tab) },
        icon = { Icon(item.icon, contentDescription = stringResource(id = item.labelRes)) },
        label = { Text(stringResource(id = item.labelRes), maxLines = 1) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.primary,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          unselectedIconColor = Color.White.copy(alpha = 0.62f),
          unselectedTextColor = Color.White.copy(alpha = 0.62f),
          indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        ),
        modifier = Modifier.testTag(item.testTag),
      )
    }
  }
}

@Composable
private fun ForkLoreNavRail(
  activeTab: ForkLoreTab,
  onSelectTab: (ForkLoreTab) -> Unit,
) {
  NavigationRail(
    containerColor = Color(0xB8090E1A),
    contentColor = Color.White,
    modifier = Modifier
      .fillMaxHeight()
      .statusBarsPadding()
      .navigationBarsPadding(),
  ) {
    Spacer(modifier = Modifier.height(12.dp))
    navItems.forEach { item ->
      val selected = activeTab == item.tab
      NavigationRailItem(
        selected = selected,
        onClick = { onSelectTab(item.tab) },
        icon = { Icon(item.icon, contentDescription = stringResource(id = item.labelRes)) },
        label = { Text(stringResource(id = item.labelRes), maxLines = 1) },
        colors = NavigationRailItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.primary,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          unselectedIconColor = Color.White.copy(alpha = 0.62f),
          unselectedTextColor = Color.White.copy(alpha = 0.62f),
          indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        ),
        modifier = Modifier.testTag(item.testTag),
      )
    }
  }
}
