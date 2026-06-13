package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.MidnightBlue
import com.example.ui.theme.ErrorRed

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        val viewModel: NexaViewModel = remember { NexaViewModel() }
        val currentScreen by viewModel.currentScreen.collectAsState()
        val selectedChatId by viewModel.selectedChatId.collectAsState()
        val selectedServiceId by viewModel.selectedServiceId.collectAsState()
        val isAdminSimulated by viewModel.isAdminSimulated.collectAsState()

        var showAdminPanelInstead by remember { mutableStateOf(false) }

        // Watch for screen changes to reset Admin screen overlay
        LaunchedEffect(currentScreen) {
          showAdminPanelInstead = false
        }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = MidnightBlue,
          bottomBar = {
            // Render fixed standard bottom bar only if we are NOT inside a full-screen detailed chat conversation
            if (selectedChatId == null) {
              NexaBottomNav(
                currentScreen = if (showAdminPanelInstead) Screen.Profile else currentScreen,
                onScreenSelected = { target ->
                  showAdminPanelInstead = false
                  viewModel.navigateTo(target)
                }
              )
            }
          },
          floatingActionButton = {
            // Render administrative shortcut FAB if simulation mode is active (and not engaged in active chat)
            if (isAdminSimulated && selectedChatId == null) {
              FloatingActionButton(
                onClick = { showAdminPanelInstead = !showAdminPanelInstead },
                containerColor = if (showAdminPanelInstead) ErrorRed else Color(0xFF6200EE),
                contentColor = Color.White,
                modifier = Modifier.testTag("admin_fab_toggle")
              ) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = "Toggle Administration Console"
                )
              }
            }
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(MidnightBlue)
              .padding(innerPadding)
          ) {
            if (selectedChatId != null) {
              // Direct fullscreen chat thread overrides main tab content
              ChatWindow(
                chatId = selectedChatId!!,
                viewModel = viewModel,
                onBack = { viewModel.selectChat(null) },
                onOpenService = { serviceId ->
                  viewModel.selectService(serviceId)
                }
              )
            } else if (showAdminPanelInstead) {
              // Admin portal display logic
              PartnerAdminPanel(viewModel = viewModel)
            } else {
              // Main Tab navigation body switch
              when (currentScreen) {
                is Screen.Chats -> ChatsTab(
                  viewModel = viewModel,
                  onSelectChat = { id -> viewModel.selectChat(id) }
                )
                is Screen.Discover -> DiscoverTab(
                  viewModel = viewModel,
                  onOpenService = { id -> viewModel.selectService(id) }
                )
                is Screen.Crypto -> CryptoTab(viewModel = viewModel)
                is Screen.Services -> ServicesTab(
                  viewModel = viewModel,
                  onOpenService = { id -> viewModel.selectService(id) }
                )
                is Screen.Profile -> ProfileTab(viewModel = viewModel)
              }
            }

            // Global Overlay Dialog for Product Detail Cards if a partner is clicked
            selectedServiceId?.let { sId ->
              ServiceDetailDialog(
                serviceId = sId,
                viewModel = viewModel,
                onDismiss = { viewModel.selectService(null) }
              )
            }
          }
        }
      }
    }
  }
}
