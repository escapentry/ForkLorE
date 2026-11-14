package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.ForkLoreApp
import com.example.ui.ForkLoreViewModel
import com.example.ui.theme.ForkLorETheme

class MainActivity : ComponentActivity() {

  private val viewModel: ForkLoreViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    super.onCreate(savedInstanceState)
    enableHighestRefreshRate()
    viewModel.handleIncomingIntent(intent)

    setContent {
      ForkLorETheme {
        ForkLoreApp(viewModel = viewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    viewModel.handleIncomingIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshAllData()
  }

  private fun enableHighestRefreshRate() {
    runCatching {
      val currentDisplay: Display? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          display
        } else {
          @Suppress("DEPRECATION")
          windowManager.defaultDisplay
        }

      if (currentDisplay != null) {
        val currentMode = currentDisplay.mode
        val bestMode = currentDisplay.supportedModes
          .filter {
            it.physicalWidth == currentMode.physicalWidth &&
              it.physicalHeight == currentMode.physicalHeight
          }
          .maxByOrNull { it.refreshRate }

        val lp = window.attributes
        if (bestMode != null) {
          lp.preferredDisplayModeId = bestMode.modeId
          lp.preferredRefreshRate = bestMode.refreshRate
        }
        window.attributes = lp
      }
    }
  }
}
