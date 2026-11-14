package com.example.telecom

import android.annotation.SuppressLint
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Immutable
data class ActiveCallUiState(
  val number: String,
  val displayName: String,
  val state: Int,
  val isMuted: Boolean = false,
  val isSpeakerOn: Boolean = false,
  val isHeld: Boolean = false,
  val elapsedSeconds: Long = 0L,
  val hasBluetoothRoute: Boolean = false,
) {
  val isRinging: Boolean
    get() = state == Call.STATE_RINGING

  val isConnected: Boolean
    get() = state == Call.STATE_ACTIVE || state == Call.STATE_HOLDING

  val stateLabel: String
    get() = when (state) {
      Call.STATE_DIALING -> "Dialing…"
      Call.STATE_RINGING -> "Incoming Call"
      Call.STATE_HOLDING -> "On Hold"
      Call.STATE_ACTIVE -> "Connected"
      Call.STATE_DISCONNECTING -> "Ending…"
      Call.STATE_DISCONNECTED -> "Call Ended"
      else -> "Connecting…"
    }
}

object ForkLoreCallManager {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var inCallService: InCallService? = null
  private var currentCall: Call? = null
  private var connectedAtMs: Long = 0L
  private var timerJob: Job? = null

  private val _activeCall = MutableStateFlow<ActiveCallUiState?>(null)
  val activeCall: StateFlow<ActiveCallUiState?> = _activeCall.asStateFlow()

  private val callCallback = object : Call.Callback() {
    override fun onStateChanged(call: Call, state: Int) {
      updateFromCall(call, state)
    }

    override fun onDetailsChanged(call: Call, details: Call.Details) {
      updateFromCall(call, getCallStateCompat(call))
    }
  }

  fun attachService(service: InCallService) {
    inCallService = service
  }

  fun detachService() {
    inCallService = null
  }

  fun onCallAdded(call: Call) {
    currentCall?.unregisterCallback(callCallback)
    currentCall = call
    connectedAtMs = 0L
    call.registerCallback(callCallback)
    updateFromCall(call, getCallStateCompat(call))
  }

  fun onCallRemoved(call: Call) {
    call.unregisterCallback(callCallback)
    if (currentCall == call) {
      currentCall = null
      connectedAtMs = 0L
      stopTimer()
      _activeCall.value = null
    }
  }

  @SuppressLint("MissingPermission")
  fun onAudioStateChanged(audioState: CallAudioState) {
    _activeCall.update { current ->
      current?.copy(
        isMuted = audioState.isMuted,
        isSpeakerOn = audioState.route == CallAudioState.ROUTE_SPEAKER,
        hasBluetoothRoute = (audioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0,
      )
    }
  }

  fun answerCall() {
    currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
  }

  fun rejectOrEndCall() {
    val call = currentCall ?: return
    if (getCallStateCompat(call) == Call.STATE_RINGING) {
      call.reject(false, null)
    } else {
      call.disconnect()
    }
  }

  fun toggleMute() {
    val nextMuted = !(_activeCall.value?.isMuted ?: false)
    inCallService?.setMuted(nextMuted)
    _activeCall.update { it?.copy(isMuted = nextMuted) }
  }

  @Suppress("DEPRECATION")
  fun toggleSpeaker() {
    val nextSpeaker = !(_activeCall.value?.isSpeakerOn ?: false)
    val targetRoute = if (nextSpeaker) {
      CallAudioState.ROUTE_SPEAKER
    } else {
      CallAudioState.ROUTE_WIRED_OR_EARPIECE
    }
    inCallService?.setAudioRoute(targetRoute)
    _activeCall.update { it?.copy(isSpeakerOn = nextSpeaker) }
  }

  fun toggleHold() {
    val call = currentCall ?: return
    if (getCallStateCompat(call) == Call.STATE_HOLDING) {
      call.unhold()
    } else {
      call.hold()
    }
  }

  fun sendDtmf(tone: Char) {
    val call = currentCall ?: return
    call.playDtmfTone(tone)
    scope.launch {
      delay(160L)
      call.stopDtmfTone()
    }
  }

  private fun updateFromCall(call: Call, state: Int) {
    val number = call.details?.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() } ?: "Unknown"
    val callerName = call.details?.callerDisplayName?.takeIf { it.isNotBlank() } ?: number

    if (state == Call.STATE_ACTIVE && connectedAtMs == 0L) {
      connectedAtMs = System.currentTimeMillis()
      startTimer()
    } else if (state == Call.STATE_DISCONNECTED) {
      stopTimer()
    }

    val elapsed = if (connectedAtMs > 0L) ((System.currentTimeMillis() - connectedAtMs) / 1000L).coerceAtLeast(0L) else 0L
    val prev = _activeCall.value
    _activeCall.value = ActiveCallUiState(
      number = number,
      displayName = callerName,
      state = state,
      isMuted = prev?.isMuted ?: false,
      isSpeakerOn = prev?.isSpeakerOn ?: false,
      isHeld = state == Call.STATE_HOLDING,
      elapsedSeconds = elapsed,
      hasBluetoothRoute = prev?.hasBluetoothRoute ?: false,
    )
  }

  @Suppress("DEPRECATION")
  private fun getCallStateCompat(call: Call): Int {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
      call.details.state
    } else {
      call.state
    }
  }

  private fun startTimer() {
    timerJob?.cancel()
    timerJob = scope.launch {
      while (isActive && connectedAtMs > 0L) {
        val elapsed = ((System.currentTimeMillis() - connectedAtMs) / 1000L).coerceAtLeast(0L)
        _activeCall.update { it?.copy(elapsedSeconds = elapsed) }
        delay(1000L)
      }
    }
  }

  private fun stopTimer() {
    timerJob?.cancel()
    timerJob = null
  }
}
