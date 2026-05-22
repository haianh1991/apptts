package com.example.webreader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.webreader.ui.BrowserScreen
import com.example.webreader.ui.BrowserViewModel
import com.example.webreader.ui.SettingsScreen
import com.example.webreader.ui.LoginScreen
import com.example.webreader.ui.LocalAppStrings
import com.example.webreader.ui.ViAppStrings
import com.example.webreader.ui.EnAppStrings
import com.example.webreader.ui.ZhAppStrings

@Composable
fun MainNavigation() {
  val viewModel: BrowserViewModel = viewModel()
  val currentUser by viewModel.currentUser.collectAsState()
  val displayLang by viewModel.appDisplayLanguage.collectAsState()

  val appStrings = remember(displayLang) {
    when (displayLang) {
      "en" -> EnAppStrings()
      "zh" -> ZhAppStrings()
      else -> ViAppStrings()
    }
  }

  androidx.compose.runtime.CompositionLocalProvider(LocalAppStrings provides appStrings) {
    if (currentUser == null) {
      LoginScreen(viewModel = viewModel)
    } else {
      val backStack = rememberNavBackStack(Browser)

      NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
          entryProvider {
            entry<Browser> {
              BrowserScreen(
                viewModel = viewModel,
                onOpenSettings = { backStack.add(Settings) }
              )
            }
            entry<Settings> {
              SettingsScreen(
                viewModel = viewModel,
                onBackClick = { backStack.removeLastOrNull() }
              )
            }
          },
      )
    }
  }
}
