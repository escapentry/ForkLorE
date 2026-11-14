package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ForkLoreRepository
import com.example.data.WallpaperPreset
import com.example.data.WallpaperRepository
import com.example.data.WallpaperState
import com.example.model.CallLogEntry
import com.example.model.ContactEntry
import com.example.model.ConversationThread
import com.example.model.DefaultHandlerStatus
import com.example.model.ForkLoreNumberHelper
import com.example.model.ForkLoreTab
import com.example.model.SmsMessageEntry
import com.example.sms.ForkLoreSmsEvents
import com.example.telecom.ActiveCallUiState
import com.example.telecom.ForkLoreCallManager
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ForkLoreUiState(
  val isInitialLoading: Boolean = true,
  val activeTab: ForkLoreTab = ForkLoreTab.KEYPAD,
  val searchQuery: String = "",
  val dialpadDigits: String = "",
  val roleStatus: DefaultHandlerStatus = DefaultHandlerStatus(),
  val contacts: List<ContactEntry> = emptyList(),
  val callLogs: List<CallLogEntry> = emptyList(),
  val conversations: List<ConversationThread> = emptyList(),
  val openChatAddress: String? = null,
  val openChatTitle: String? = null,
  val openChatMessages: List<SmsMessageEntry> = emptyList(),
  val selectedContact: ContactEntry? = null,
  val statusBanner: String? = null,
  val isNewMessageDialogOpen: Boolean = false,
  val isAddContactDialogOpen: Boolean = false,
  val addContactPrefillPhone: String = "",
  val wallpaperState: WallpaperState = WallpaperState(),
  val isWallpaperMenuOpen: Boolean = false,
  val activeCall: ActiveCallUiState? = null,
) {
  val filteredContacts: List<ContactEntry>
    get() {
      val q = searchQuery.trim().lowercase(Locale.US)
      if (q.isEmpty()) return contacts
      return contacts.filter { c ->
        c.displayName.lowercase(Locale.US).contains(q) ||
          c.allNumbers.any { it.contains(q) }
      }
    }

  val filteredCallLogs: List<CallLogEntry>
    get() {
      val q = searchQuery.trim().lowercase(Locale.US)
      if (q.isEmpty()) return callLogs
      return callLogs.filter { entry ->
        (entry.displayName?.lowercase(Locale.US)?.contains(q) == true) ||
          entry.number.lowercase(Locale.US).contains(q)
      }
    }

  val filteredConversations: List<ConversationThread>
    get() {
      val q = searchQuery.trim().lowercase(Locale.US)
      if (q.isEmpty()) return conversations
      return conversations.filter { t ->
        (t.displayName?.lowercase(Locale.US)?.contains(q) == true) ||
          t.address.lowercase(Locale.US).contains(q) ||
          t.snippet.lowercase(Locale.US).contains(q)
      }
    }

  val dialpadMatches: List<ContactEntry>
    get() {
      if (dialpadDigits.isBlank()) return emptyList()
      return contacts.filter { ForkLoreNumberHelper.matchesDialpadQuery(it, dialpadDigits) }.take(8)
    }
}

class ForkLoreViewModel(application: Application) : AndroidViewModel(application) {

  val repository = ForkLoreRepository(application)
  private val wallpaperRepository = WallpaperRepository(application)

  private val _uiState = MutableStateFlow(ForkLoreUiState())
  val uiState: StateFlow<ForkLoreUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      val wp = wallpaperRepository.loadWallpaperState()
      _uiState.update { it.copy(wallpaperState = wp, isInitialLoading = false) }
      refreshAllData()
    }

    viewModelScope.launch {
      ForkLoreSmsEvents.incomingRefreshTick.collect {
        refreshSmsAndActiveChat()
      }
    }

    viewModelScope.launch {
      ForkLoreCallManager.activeCall.collect { call ->
        _uiState.update { it.copy(activeCall = call) }
      }
    }
  }

  fun refreshAllData() {
    viewModelScope.launch {
      val status = repository.checkDefaultAndPermissions()
      val contacts = repository.loadContacts()
      val logs = repository.loadCallLogs(contacts)
      val convos = repository.loadConversations(contacts)
      val openAddr = _uiState.value.openChatAddress
      val messages = if (openAddr != null) repository.loadThreadMessages(openAddr) else emptyList()

      _uiState.update {
        it.copy(
          roleStatus = status,
          contacts = contacts,
          callLogs = logs,
          conversations = convos,
          openChatMessages = messages,
        )
      }
    }
  }

  private fun refreshSmsAndActiveChat() {
    viewModelScope.launch {
      val contacts = _uiState.value.contacts
      val convos = repository.loadConversations(contacts)
      val openAddr = _uiState.value.openChatAddress
      val messages = if (openAddr != null) repository.loadThreadMessages(openAddr) else emptyList()
      _uiState.update {
        it.copy(
          conversations = convos,
          openChatMessages = messages,
        )
      }
    }
  }

  fun selectTab(tab: ForkLoreTab) {
    _uiState.update { it.copy(activeTab = tab) }
  }

  fun updateSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun appendDialpadChar(ch: Char) {
    _uiState.update { it.copy(dialpadDigits = it.dialpadDigits + ch) }
  }

  fun backspaceDialpad() {
    _uiState.update {
      if (it.dialpadDigits.isNotEmpty()) {
        it.copy(dialpadDigits = it.dialpadDigits.dropLast(1))
      } else {
        it
      }
    }
  }

  fun clearDialpad() {
    _uiState.update { it.copy(dialpadDigits = "") }
  }

  fun setDialpadDigits(number: String) {
    _uiState.update { it.copy(dialpadDigits = number, activeTab = ForkLoreTab.KEYPAD) }
  }

  fun placeCall(number: String = _uiState.value.dialpadDigits) {
    val clean = number.trim()
    if (clean.isEmpty()) return
    val ok = repository.placeCall(clean)
    if (!ok) {
      _uiState.update { it.copy(statusBanner = "Unable to place call to $clean") }
    } else {
      viewModelScope.launch {
        kotlinx.coroutines.delay(1200L)
        refreshAllData()
      }
    }
  }

  fun openChat(address: String, displayName: String? = null) {
    val clean = address.trim()
    if (clean.isEmpty()) return
    val matchedName = displayName ?: _uiState.value.contacts.firstOrNull { c ->
      c.allNumbers.any { ForkLoreNumberHelper.numbersMatch(it, clean) }
    }?.displayName

    _uiState.update {
      it.copy(
        activeTab = ForkLoreTab.MESSAGES,
        openChatAddress = clean,
        openChatTitle = matchedName ?: clean,
        isNewMessageDialogOpen = false,
      )
    }

    viewModelScope.launch {
      val messages = repository.loadThreadMessages(clean)
      messages.firstOrNull()?.threadId?.let { tid ->
        repository.markThreadRead(tid)
      }
      val convos = repository.loadConversations(_uiState.value.contacts)
      _uiState.update {
        it.copy(
          openChatMessages = messages,
          conversations = convos,
        )
      }
    }
  }

  fun closeChat() {
    _uiState.update {
      it.copy(openChatAddress = null, openChatTitle = null, openChatMessages = emptyList())
    }
  }

  fun sendMessage(address: String, body: String) {
    viewModelScope.launch {
      val result = repository.sendSms(address, body)
      result.onSuccess {
        refreshSmsAndActiveChat()
      }.onFailure { err ->
        _uiState.update { it.copy(statusBanner = err.localizedMessage ?: "Failed to send SMS") }
      }
    }
  }

  fun selectContact(contact: ContactEntry?) {
    _uiState.update { it.copy(selectedContact = contact) }
  }

  fun toggleContactStar(contact: ContactEntry) {
    viewModelScope.launch {
      val ok = repository.toggleContactStar(contact.id, !contact.isStarred)
      if (ok) {
        refreshAllData()
        _uiState.update { state ->
          if (state.selectedContact?.id == contact.id) {
            state.copy(selectedContact = contact.copy(isStarred = !contact.isStarred))
          } else {
            state
          }
        }
      } else {
        _uiState.update { it.copy(statusBanner = "Grant Contacts permission to edit favorites") }
      }
    }
  }

  fun openNewMessageDialog() {
    _uiState.update { it.copy(isNewMessageDialogOpen = true) }
  }

  fun closeNewMessageDialog() {
    _uiState.update { it.copy(isNewMessageDialogOpen = false) }
  }

  fun openAddContactDialog(prefillPhone: String = "") {
    _uiState.update {
      it.copy(isAddContactDialogOpen = true, addContactPrefillPhone = prefillPhone)
    }
  }

  fun closeAddContactDialog() {
    _uiState.update { it.copy(isAddContactDialogOpen = false, addContactPrefillPhone = "") }
  }

  fun saveNewContact(name: String, phone: String) {
    viewModelScope.launch {
      val saved = repository.addContact(name, phone)
      if (saved) {
        _uiState.update {
          it.copy(
            isAddContactDialogOpen = false,
            addContactPrefillPhone = "",
            statusBanner = "Saved contact $name",
          )
        }
        refreshAllData()
      } else {
        _uiState.update { it.copy(statusBanner = "Grant Contacts permission to save contacts") }
      }
    }
  }

  fun handleIncomingIntent(intent: Intent?) {
    if (intent == null) return
    val data: Uri? = intent.data
    when (intent.action) {
      Intent.ACTION_DIAL,
      Intent.ACTION_VIEW -> {
        val number = data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart
        if (!number.isNullOrBlank()) {
          setDialpadDigits(number)
        }
      }

      Intent.ACTION_SENDTO,
      Intent.ACTION_SEND -> {
        val scheme = data?.scheme
        if (scheme == "sms" || scheme == "smsto" || scheme == "mms" || scheme == "mmsto") {
          val address = data.schemeSpecificPart?.substringBefore('?')
          if (!address.isNullOrBlank()) {
            openChat(address)
          }
        }
      }
    }
  }

  fun dismissStatusBanner() {
    _uiState.update { it.copy(statusBanner = null) }
  }

  fun openWallpaperMenu() {
    _uiState.update { it.copy(isWallpaperMenuOpen = true) }
  }

  fun dismissWallpaperMenu() {
    _uiState.update { it.copy(isWallpaperMenuOpen = false) }
  }

  fun selectWallpaperPreset(preset: WallpaperPreset) {
    _uiState.update { it.copy(wallpaperState = it.wallpaperState.copy(activePreset = preset)) }
    viewModelScope.launch { wallpaperRepository.selectPreset(preset) }
  }

  fun saveCustomWallpaper(uri: Uri) {
    viewModelScope.launch {
      val bmp = wallpaperRepository.saveCustomPhotoFromUri(uri)
      if (bmp != null) {
        _uiState.update {
          it.copy(
            wallpaperState = it.wallpaperState.copy(
              activePreset = WallpaperPreset.CUSTOM_PHOTO,
              customBitmap = bmp,
              customVersion = it.wallpaperState.customVersion + 1,
            )
          )
        }
      }
    }
  }
}
