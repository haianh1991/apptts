package com.example.webreader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.webreader.ui.BrowserScreen
import com.example.webreader.ui.BrowserViewModel
import com.example.webreader.ui.SettingsScreen
import com.example.webreader.ui.LoginScreen

@Composable
fun MainNavigation() {
  val viewModel: BrowserViewModel = viewModel()
  val currentUser by viewModel.currentUser.collectAsState()

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
