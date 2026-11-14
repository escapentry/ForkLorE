package com.example.telecom

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ForkLoreNumberHelper
import com.example.ui.components.DefaultAppleSquircleShape
import com.example.ui.theme.ForkLorETheme

class CallActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
    )
    super.onCreate(savedInstanceState)

    setContent {
      ForkLorETheme {
        val callState by ForkLoreCallManager.activeCall.collectAsStateWithLifecycle()
        LaunchedEffect(callState) {
          if (callState == null) {
            finish()
          }
        }
        callState?.let { active ->
          InCallScreen(call = active)
        }
      }
    }
  }
}

@Composable
private fun InCallScreen(call: ActiveCallUiState) {
  var showDtmfPad by remember { mutableStateOf(false) }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF060E24), Color(0xFF0A1428), Color(0xFF050810))
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 24.dp, vertical = 28.dp),
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = if (call.isConnected) {
          ForkLoreNumberHelper.formatCallDuration(call.elapsedSeconds)
        } else {
          call.stateLabel
        },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(22.dp))
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(104.dp)
          .clip(DefaultAppleSquircleShape)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
          .border(1.dp, Color.White.copy(alpha = 0.18f), DefaultAppleSquircleShape),
      ) {
        Icon(
          imageVector = Icons.Rounded.Person,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(52.dp),
        )
      }
      Spacer(modifier = Modifier.height(18.dp))
      Text(
        text = call.displayName,
        style = MaterialTheme.typography.displayLarge.copy(
          fontSize = MaterialTheme.typography.headlineMedium.fontSize,
          fontWeight = FontWeight.Bold,
        ),
        color = Color.White,
        textAlign = TextAlign.Center,
      )
      if (call.displayName != call.number) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = call.number,
          style = MaterialTheme.typography.bodyLarge,
          color = Color.White.copy(alpha = 0.68f),
        )
      }
    }

    if (showDtmfPad) {
      val keys = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('*', '0', '#'),
      )
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        keys.forEach { row ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            row.forEach { digit ->
              Surface(
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(54.dp)
                  .clip(RoundedCornerShape(18.dp))
                  .clickable { ForkLoreCallManager.sendDtmf(digit) },
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = digit.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                  )
                }
              }
            }
          }
        }
      }
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(24.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      if (!call.isRinging) {
        Row(
          horizontalArrangement = Arrangement.SpaceEvenly,
          modifier = Modifier.fillMaxWidth(),
        ) {
          CallControlTile(
            icon = if (call.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
            label = if (call.isMuted) "Muted" else "Mute",
            active = call.isMuted,
            onClick = ForkLoreCallManager::toggleMute,
          )
          CallControlTile(
            icon = Icons.Rounded.Dialpad,
            label = "Keypad",
            active = showDtmfPad,
            onClick = { showDtmfPad = !showDtmfPad },
          )
          CallControlTile(
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            label = "Speaker",
            active = call.isSpeakerOn,
            onClick = ForkLoreCallManager::toggleSpeaker,
          )
          CallControlTile(
            icon = Icons.Rounded.Pause,
            label = if (call.isHeld) "Resume" else "Hold",
            active = call.isHeld,
            onClick = ForkLoreCallManager::toggleHold,
          )
        }
      }

      Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (call.isRinging) {
          FilledIconButton(
            onClick = ForkLoreCallManager::rejectOrEndCall,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.size(72.dp),
          ) {
            Icon(Icons.Rounded.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(32.dp))
          }
          FilledIconButton(
            onClick = ForkLoreCallManager::answerCall,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF10B981)),
            modifier = Modifier.size(72.dp),
          ) {
            Icon(Icons.Rounded.Call, contentDescription = "Answer", tint = Color.White, modifier = Modifier.size(32.dp))
          }
        } else {
          FilledIconButton(
            onClick = ForkLoreCallManager::rejectOrEndCall,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.size(72.dp),
          ) {
            Icon(Icons.Rounded.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(32.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun CallControlTile(
  icon: ImageVector,
  label: String,
  active: Boolean,
  onClick: () -> Unit,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(58.dp)
        .clip(CircleShape)
        .background(if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f))
        .clickable(onClick = onClick),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (active) MaterialTheme.colorScheme.onPrimary else Color.White,
        modifier = Modifier.size(26.dp),
      )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = Color.White.copy(alpha = 0.78f),
    )
  }
}
