/*
 * WensChat navigation entry point.
 * New installations begin with account onboarding, not the chat home screen.
 */
package io.getstream.whatsappclone.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.getstream.whatsappclone.auth.WelcomeAuthFlow
import io.getstream.whatsappclone.ui.WhatsAppTabPager
import io.getstream.whatsappclone.ui.WhatsAppTopBar
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier

private const val AUTH_ROUTE = "wenschat_auth"

@Composable
fun WhatsAppNavHost(navHostController: NavHostController) {
  NavHost(
    navController = navHostController,
    startDestination = AUTH_ROUTE
  ) {
    composable(AUTH_ROUTE) {
      WelcomeAuthFlow(
        onSendCode = { /* Supabase Auth integration required. */ },
        onVerifyCode = { _, _ -> /* Never mark a user verified without backend confirmation. */ },
        onProfileContinue = { /* Create the profile only after verified authentication. */ },
        onLoginClick = { /* Login backend flow is not connected yet. */ }
      )
    }

    composable(WhatsAppScreens.Home.name) {
      Scaffold(topBar = { WhatsAppTopBar() }) { padding ->
        WhatsAppTabPager(modifier = Modifier.padding(padding))
      }
    }

    whatsAppHomeNavigation()
  }
}
