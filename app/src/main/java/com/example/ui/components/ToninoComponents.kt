package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ToninoViewModel
import com.example.Screen
import com.example.data.MockData
import com.example.types.*
import com.example.ui.theme.*

// ==========================================
// CENTRAL GLASS CONTAINER DESIGN
// ==========================================
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  borderColor: Color = Color.White.copy(alpha = 0.12f),
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  val shape = RoundedCornerShape(24.dp)
  val baseModifier = modifier
    .clip(shape)
    .background(
      Brush.linearGradient(
        colors = listOf(
          CyberPurple.copy(alpha = 0.15f),
          CyberBlue.copy(alpha = 0.08f)
        )
      )
    )
    .border(1.dp, borderColor, shape)

  val cardModifier = if (onClick != null) {
    baseModifier.clickable(onClick = onClick)
  } else {
    baseModifier
  }

  Column(
    modifier = cardModifier.padding(20.dp),
    content = content
  )
}

// ==========================================
// GEOMETRIC BALANCE HEADER COMPONENT
// ==========================================
@Composable
fun GeometricHeader(
  title: String,
  subtitle: String,
  badgeText: String? = null,
  badgeColor: Color = CyberBlue,
  onBadgeClick: (() -> Unit)? = null,
  imageVector: ImageVector? = null,
  onIconClick: (() -> Unit)? = null
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column {
      Text(
        text = "TONINO PLATFORM",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = CyberBlue,
        letterSpacing = 2.5.sp
      )
      Spacer(modifier = Modifier.height(2.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = title,
          fontSize = 24.sp,
          fontWeight = FontWeight.ExtraBold,
          color = TextPrimary
        )
        if (subtitle.isNotEmpty()) {
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "· $subtitle",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (badgeText != null) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor.copy(alpha = 0.15f))
            .let { if (onBadgeClick != null) it.clickable(onClick = onBadgeClick) else it }
            .border(0.5.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = badgeText.uppercase(),
            color = badgeColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      if (imageVector != null && onIconClick != null) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(GlassOverlay)
            .clickable(onClick = onIconClick),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(18.dp)
          )
        }
      } else {
        // High quality Felix styled geometric circular avatar
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(CyberBlue.copy(alpha = 0.15f))
            .border(0.5.dp, GlassOverlay, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "F",
            color = CyberBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
      }
    }
  }
}

// ==========================================
// 1. BOTTOM NAVIGATION
// ==========================================
@Composable
fun ToninoBottomNav(
  currentScreen: Screen,
  onScreenSelected: (Screen) -> Unit,
  modifier: Modifier = Modifier
) {
  NavigationBar(
    containerColor = DarkSlate.copy(alpha = 0.95f),
    tonalElevation = 8.dp,
    modifier = modifier
      .border(0.5.dp, GlassOverlay, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
      .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
  ) {
    val items = listOf(
      Triple(Screen.Chats, Icons.Default.ChatBubble, "Chats"),
      Triple(Screen.Discover, Icons.Default.Explore, "Discover"),
      Triple(Screen.Crypto, Icons.Default.AccountBalanceWallet, "Crypto"),
      Triple(Screen.Services, Icons.Default.GridOn, "Services"),
      Triple(Screen.Profile, Icons.Default.Person, "Profile")
    )

    items.forEach { (screen, icon, label) ->
      val selected = currentScreen == screen
      NavigationBarItem(
        icon = {
          Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) CyberBlue else TextSecondary,
            modifier = Modifier.size(24.dp)
          )
        },
        label = {
          Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) CyberBlue else TextSecondary
          )
        },
        selected = selected,
        onClick = { onScreenSelected(screen) },
        colors = NavigationBarItemDefaults.colors(
          indicatorColor = CyberBlue.copy(alpha = 0.15f)
        ),
        modifier = Modifier.testTag("nav_btn_${label.lowercase()}")
      )
    }
  }
}

// ==========================================
// 2. CHATS SECTION & LIST
// ==========================================
@Composable
fun ChatsTab(
  viewModel: ToninoViewModel,
  onSelectChat: (String) -> Unit
) {
  val chats by viewModel.chats.collectAsState()
  var searchQuery by remember { mutableStateOf("") }

  val filteredChats = chats.filter {
    it.name.contains(searchQuery, ignoreCase = true) ||
      it.lastMessage.contains(searchQuery, ignoreCase = true)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MidnightBlue)
      .padding(horizontal = 16.dp)
  ) {
    // Header
    Spacer(modifier = Modifier.statusBarsPadding())
    GeometricHeader(
      title = "Tonino Chat",
      subtitle = "Berlin, DE",
      badgeText = "TELEGRAM ONLINE",
      badgeColor = CyberBlue
    )

    // Search bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("Search messages, bots...", color = TextSecondary) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .testTag("chats_search_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DarkSlate,
        unfocusedContainerColor = DarkSlate,
        focusedBorderColor = CyberBlue,
        unfocusedBorderColor = GlassOverlay,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
      ),
      shape = RoundedCornerShape(12.dp),
      singleLine = true
    )

    // Chat items
    if (filteredChats.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = TextSecondary)
          Spacer(modifier = Modifier.height(16.dp))
          Text("No chats found", color = TextSecondary, fontSize = 16.sp)
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Pinned Chats first
        val pinned = filteredChats.filter { it.isPinned }
        val ordinary = filteredChats.filter { !it.isPinned }

        if (pinned.isNotEmpty()) {
          item {
            Text(
              "PINNED CHATS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = CyberPurple,
              modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
          }
          items(pinned) { chat ->
            ChatRowItem(chat = chat, onClick = { onSelectChat(chat.id) })
          }
        }

        if (ordinary.isNotEmpty()) {
          item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              "ALL CONVERSATIONS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = TextSecondary,
              modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
          }
          items(ordinary) { chat ->
            ChatRowItem(chat = chat, onClick = { onSelectChat(chat.id) })
          }
        }
      }
    }
  }
}

@Composable
fun ChatRowItem(chat: Chat, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(if (chat.isPinned) ChatBubbleSelf.copy(alpha = 0.3f) else DarkSlate)
      .border(
        0.5.dp,
        if (chat.isPinned) CyberBlue.copy(alpha = 0.3f) else GlassOverlay,
        RoundedCornerShape(12.dp)
      )
      .clickable(onClick = onClick)
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Avatar with Online indicator
    Box(contentAlignment = Alignment.BottomEnd) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .background(
            Brush.linearGradient(
              listOf(CyberPurple.copy(alpha = 0.60f), CyberBlue.copy(alpha = 0.60f))
            ),
            CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = chat.name.take(2).trim(),
          fontWeight = FontWeight.Bold,
          color = Color.White,
          fontSize = 16.sp
        )
      }
      if (chat.isOnline) {
        Box(
          modifier = Modifier
            .size(12.dp)
            .background(MidnightBlue, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(NeonCyan, CircleShape)
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    // Info columns
    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = chat.name,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (chat.isGroup) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
              modifier = Modifier
                .background(CyberPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
              Text("group", fontSize = 8.sp, color = CyberPurple, fontWeight = FontWeight.Bold)
            }
          }
        }
        Text(
          text = chat.lastMessageTime,
          fontSize = 11.sp,
          color = TextSecondary
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = chat.lastMessage,
        fontSize = 13.sp,
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Indicators: pin & unreadCount
    Column(horizontalAlignment = Alignment.End) {
      if (chat.isPinned) {
        Icon(
          Icons.Default.PushPin,
          contentDescription = "Pinned",
          tint = CyberBlue,
          modifier = Modifier.size(14.dp)
        )
      }
      if (chat.unreadCount > 0) {
        Spacer(modifier = Modifier.height(4.dp))
        Box(
          modifier = Modifier
            .background(CyberBlue, CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = chat.unreadCount.toString(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

// ==========================================
// 2.1 CONVERSATION DIRECT VIEW
// ==========================================
@Composable
fun ChatWindow(
  chatId: String,
  viewModel: ToninoViewModel,
  onBack: () -> Unit,
  onOpenService: (String) -> Unit
) {
  val chats by viewModel.chats.collectAsState()
  val partnerList by viewModel.services.collectAsState()
  val walletState by viewModel.walletState.collectAsState()
  val chat = chats.firstOrNull { it.id == chatId } ?: return

  var inputtedText by remember { mutableStateOf("") }
  var showAttachmentDialog by remember { mutableStateOf(false) }
  var showCryptoTransferDialog by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MidnightBlue)
      .imePadding()
  ) {
    // Elegant Top header
    Row(
      modifier = Modifier
        .statusBarsPadding()
        .fillMaxWidth()
        .background(DarkSlate)
        .border(0.5.dp, GlassOverlay, RoundedCornerShape(0.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.testTag("chat_window_back_btn")
      ) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
      }

      Spacer(modifier = Modifier.width(4.dp))

      Box {
        Box(
          modifier = Modifier
            .size(40.dp)
            .background(
              Brush.linearGradient(listOf(CyberBlue, CyberPurple)),
              CircleShape
            ),
          contentAlignment = Alignment.Center
        ) {
          Text(chat.name.take(2).trim(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        if (chat.isOnline) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .background(MidnightBlue, CircleShape)
              .align(Alignment.BottomEnd)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(NeonCyan, CircleShape)
                .align(Alignment.Center)
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(chat.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
        Text(
          text = if (chat.isOnline) "online" else "last seen recently",
          color = if (chat.isOnline) NeonCyan else TextSecondary,
          fontSize = 11.sp
        )
      }

      // Quick share partners shortcut
      IconButton(onClick = { showAttachmentDialog = true }) {
        Icon(Icons.Default.GridOn, contentDescription = "Share mini service", tint = CyberBlue)
      }
    }

    // Message lists
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(chat.messages) { message ->
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = if (message.isSelf) Alignment.End else Alignment.Start
        ) {
          Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isSelf) Arrangement.End else Arrangement.Start,
            modifier = Modifier.padding(horizontal = 4.dp)
          ) {
            if (!message.isSelf) {
              Text(
                message.senderName,
                fontSize = 10.sp,
                color = CyberPurple,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 4.dp, bottom = 2.dp)
              )
            }
          }

          Box(
            modifier = Modifier
              .clip(
                RoundedCornerShape(
                  topStart = 16.dp,
                  topEnd = 16.dp,
                  bottomStart = if (message.isSelf) 16.dp else 0.dp,
                  bottomEnd = if (message.isSelf) 0.dp else 16.dp
                )
              )
              .background(if (message.isSelf) ChatBubbleSelf else ChatBubbleOther)
              .border(
                0.5.dp,
                if (message.isSelf) CyberBlue.copy(alpha = 0.3f) else GlassOverlay,
                RoundedCornerShape(
                  topStart = 16.dp,
                  topEnd = 16.dp,
                  bottomStart = if (message.isSelf) 16.dp else 0.dp,
                  bottomEnd = if (message.isSelf) 0.dp else 16.dp
                )
              )
              .padding(symmetric = 12.dp, vertical = 8.dp)
              .widthIn(max = 280.dp)
          ) {
            Column {
              Text(text = message.text, color = TextPrimary, fontSize = 14.sp)

              // Shared partner service card visualization!
              message.attachmentServiceId?.let { sId ->
                val partner = partnerList.firstOrNull { it.id == sId }
                if (partner != null) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(MidnightBlue.copy(alpha = 0.8f))
                      .border(0.5.dp, CyberBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                      .clickable { onOpenService(partner.id) }
                      .padding(8.dp)
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        MockData.getIconFromString(partner.iconName),
                        contentDescription = null,
                        tint = CyberBlue,
                        modifier = Modifier.size(24.dp)
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Column {
                        Text(partner.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(partner.category, fontSize = 9.sp, color = TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Icon(Icons.Default.Star, "Rating", tint = GoldAccent, modifier = Modifier.size(10.dp))
                          Spacer(modifier = Modifier.width(2.dp))
                          Text("${partner.rating} (${partner.reviewsCount})", fontSize = 9.sp, color = TextSecondary)
                        }
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = message.timestamp,
                color = TextSecondary,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.align(Alignment.End)
              )
            }
          }
        }
      }
    }

    // Dynamic input footer
    Row(
      modifier = Modifier
        .navigationBarsPadding()
        .fillMaxWidth()
        .background(DarkSlate)
        .border(0.5.dp, GlassOverlay, RoundedCornerShape(0.dp))
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Wallet Pay Action Inside Chat
      IconButton(
        onClick = { showCryptoTransferDialog = true },
        modifier = Modifier.testTag("chat_pay_crypto_btn")
      ) {
        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Send Crypto", tint = TokenGreen)
      }

      // Add shortcut icon
      IconButton(onClick = { showAttachmentDialog = true }) {
        Icon(Icons.Default.Add, contentDescription = "Add attachment", tint = TextSecondary)
      }

      // TextInput
      OutlinedTextField(
        value = inputtedText,
        onValueChange = { inputtedText = it },
        placeholder = { Text("Message", color = TextSecondary) },
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MidnightBlue,
          unfocusedContainerColor = MidnightBlue,
          focusedBorderColor = CyberBlue.copy(alpha = 0.5f),
          unfocusedBorderColor = Color.Transparent,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 4.dp)
          .testTag("chat_window_input"),
        singleLine = true,
        trailingIcon = {
          IconButton(onClick = { inputtedText += "😊" }) {
            Text("😊", style = MaterialTheme.typography.bodyLarge)
          }
        }
      )

      // Send Button
      IconButton(
        onClick = {
          if (inputtedText.isNotBlank()) {
            viewModel.sendMessage(chatId, inputtedText)
            inputtedText = ""
          }
        },
        modifier = Modifier.testTag("chat_send_btn")
      ) {
        Icon(
          Icons.Default.Send,
          contentDescription = "Send",
          tint = if (inputtedText.isNotBlank()) CyberBlue else TextSecondary
        )
      }
    }
  }

  // DIALOG FOR SHARING MINI SERVICES DIRECTLY IN CHATS
  if (showAttachmentDialog) {
    Dialog(onDismissRequest = { showAttachmentDialog = false }) {
      GlassCard(borderColor = CyberBlue) {
        Text("Share Miniature Platform Service", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          "Pick a micro-application from the ecosystem to stream directly into Pavel's chat bubble:",
          color = TextSecondary,
          fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.height(220.dp)) {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(partnerList.take(6)) { service ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(MidnightBlue)
                  .clickable {
                    viewModel.sendMessage(chatId, "I found this awesome mini-app: ${service.name}!", service.id)
                    showAttachmentDialog = false
                  }
                  .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  MockData.getIconFromString(service.iconName),
                  contentDescription = null,
                  tint = CyberPurple,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(service.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                  Text(service.category, fontSize = 10.sp, color = TextSecondary)
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
          onClick = { showAttachmentDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Cancel")
        }
      }
    }
  }

  // DIALOG FOR WIRELESS CRYPTO PAYMENTS DIRECT IN CHAT
  if (showCryptoTransferDialog) {
    var payAmount by remember { mutableStateOf("0.5") }
    var paySuccessMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { showCryptoTransferDialog = false }) {
      GlassCard(borderColor = TokenGreen) {
        Text("Send TON to Chat Partner", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        if (walletState.status != WalletStatus.CONNECTED) {
          Text("⚠️ You must connect your wallet first!", color = ErrorRed, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = {
              showCryptoTransferDialog = false
              viewModel.navigateTo(Screen.Crypto)
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Go to Crypto Wallet")
          }
        } else {
          Text("Selected Wallet: ${walletState.walletType}", color = TextSecondary, fontSize = 12.sp)
          Text("Available TON: ${walletState.tonBalance} TON", color = TokenGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = payAmount,
            onValueChange = { payAmount = it },
            label = { Text("TON Amount to Transfer", color = TextSecondary) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedBorderColor = TokenGreen,
              unfocusedBorderColor = GlassOverlay
            ),
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(16.dp))

          paySuccessMsg?.let { msg ->
            Text(msg, color = TokenGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { showCryptoTransferDialog = false },
              colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
              modifier = Modifier.weight(1f)
            ) {
              Text("Close")
            }
            Button(
              onClick = {
                val amt = payAmount.toDoubleOrNull() ?: 0.0
                val ok = viewModel.triggerQuickTransaction("EQB3...pavel", amt)
                if (ok) {
                  viewModel.sendMessage(chatId, "💸 Sent you $amt TON via my connected ${walletState.walletType}!")
                  paySuccessMsg = "Transaction broadcasted successfully!"
                } else {
                  paySuccessMsg = "Error: Insufficient gas/TON!"
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = TokenGreen),
              modifier = Modifier.weight(1f)
            ) {
              Text("Authorize Pay")
            }
          }
        }
      }
    }
  }
}

// ==========================================
// 3. DISCOVER SECTION (GEO-BASED & SEARCH)
// ==========================================
@Composable
fun DiscoverTab(
  viewModel: ToninoViewModel,
  onOpenService: (String) -> Unit
) {
  val services by viewModel.services.collectAsState()
  val userCity by viewModel.userCity.collectAsState()
  val userCountry by viewModel.userCountry.collectAsState()
  val locationPermission by viewModel.locationPermission.collectAsState()
  val favorites by viewModel.favorites.collectAsState()

  var showLocationSettings by remember { mutableStateOf(false) }

  // Filtering local active services only
  val localServices = services.filter {
    // If location is enabled or of a certain city, show local coordinates
    if (locationPermission == "Enabled" || locationPermission == "Manual") {
      it.city.equals(userCity, ignoreCase = true)
    } else {
      true // Show physical global fallback
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MidnightBlue)
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.statusBarsPadding())

    // Geo Header banner
    GeometricHeader(
      title = "Geo Discover",
      subtitle = "$userCity, $userCountry",
      badgeText = "GPS Active",
      badgeColor = NeonCyan,
      onBadgeClick = { showLocationSettings = true },
      imageVector = Icons.Default.SettingsInputAntenna,
      onIconClick = { showLocationSettings = true }
    )

    // Horizontal filters
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      LocationChip(
        label = "Near Me",
        selected = locationPermission == "Enabled",
        onClick = { viewModel.selectLocationPermission("Enabled") }
      )
      LocationChip(
        label = "Top Rated",
        selected = true,
        onClick = {}
      )
      LocationChip(
        label = "Verified Partners",
        selected = false,
        onClick = {}
      )
    }

    // Discovery lists
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Text("POPULAR IN YOUR AREA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberPurple)
      }

      val featured = localServices.filter { it.isFeatured }
      if (featured.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(DarkSlate, RoundedCornerShape(12.dp))
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text("No active partners found for city: $userCity", color = TextSecondary)
          }
        }
      } else {
        items(featured) { service ->
          DiscoverPartnerCard(
            service = service,
            isFavorite = favorites.contains(service.id),
            onToggleFavorite = { viewModel.toggleFavorite(service.id) },
            onClick = { onOpenService(service.id) }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(12.dp))
        Text("NEW TRENDING PARTNERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
      }

      val trending = localServices.filter { !it.isFeatured }
      if (trending.isEmpty()) {
        item {
          Text("No trending services in this radius.", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
        }
      } else {
        items(trending) { service ->
          DiscoverPartnerCard(
            service = service,
            isFavorite = favorites.contains(service.id),
            onToggleFavorite = { viewModel.toggleFavorite(service.id) },
            onClick = { onOpenService(service.id) }
          )
        }
      }
    }
  }

  // LOCATION SETTINGS MODAL DIALOG
  if (showLocationSettings) {
    var customFieldCity by remember { mutableStateOf("Munich") }
    Dialog(onDismissRequest = { showLocationSettings = false }) {
      GlassCard(borderColor = NeonCyan) {
        Text("Geo-Location Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        // State 1: Active
        Text("Location state: $locationPermission", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = {
            viewModel.selectLocationPermission("Enabled")
            showLocationSettings = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = if (locationPermission == "Enabled") CyberBlue else Color.DarkGray),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enable GPS Simulator (Berlin)")
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = {
            viewModel.selectLocationPermission("Disabled")
            showLocationSettings = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = if (locationPermission == "Disabled") ErrorRed else Color.DarkGray),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOff, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Disable Geolocation (Global View)")
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = GlassOverlay)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Manual City Selector", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = customFieldCity,
          onValueChange = { customFieldCity = it },
          placeholder = { Text("E.g. Munich, Tokyo, London") },
          colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = {
            val countryMapping = when (customFieldCity.lowercase().trim()) {
              "tokyo" -> "Japan"
              "london" -> "UK"
              "paris" -> "France"
              "rome" -> "Italy"
              "munich" -> "Germany"
              "hamburg" -> "Germany"
              else -> "Germany"
            }
            viewModel.setManualCity(customFieldCity, countryMapping)
            showLocationSettings = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Connect to custom city")
        }
      }
    }
  }
}

@Composable
fun LocationChip(label: String, selected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (selected) CyberBlue else DarkSlate)
      .border(0.5.dp, if (selected) NeonCyan else GlassOverlay, RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 6.dp)
  ) {
    Text(
      text = label,
      color = if (selected) Color.White else TextSecondary,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
fun DiscoverPartnerCard(
  service: PartnerService,
  isFavorite: Boolean,
  onToggleFavorite: () -> Unit,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(DarkSlate)
      .border(0.5.dp, GlassOverlay, RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Icon badge
    Box(
      modifier = Modifier
        .size(52.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(
          Brush.linearGradient(listOf(CyberBlue.copy(alpha = 0.15f), CyberPurple.copy(alpha = 0.15f)))
        )
        .border(0.5.dp, CyberPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = MockData.getIconFromString(service.iconName),
        contentDescription = service.name,
        tint = CyberBlue,
        modifier = Modifier.size(28.dp)
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    // Information details
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          service.name,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          fontSize = 16.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (service.isVerified) {
          Spacer(modifier = Modifier.width(4.dp))
          Icon(Icons.Default.Verified, "Verified Verified", tint = NeonCyan, modifier = Modifier.size(14.dp))
        }
        if (service.isHot) {
          Spacer(modifier = Modifier.width(4.dp))
          Box(
            modifier = Modifier
              .background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
              .padding(horizontal = 4.dp, vertical = 1.dp)
          ) {
            Text("HOT", fontSize = 8.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
          }
        }
      }

      Text(
        text = "${service.category} · ${service.distanceKm ?: 1.0} km",
        fontSize = 12.sp,
        color = TextSecondary
      )

      Spacer(modifier = Modifier.height(4.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, "Score", tint = GoldAccent, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(2.dp))
        Text(
          text = "${service.rating} · ${service.reviewsCount} reviews",
          fontSize = 11.sp,
          color = TextSecondary
        )
      }
    }

    Spacer(modifier = Modifier.width(6.dp))

    // Action button
    Column(horizontalAlignment = Alignment.End) {
      IconButton(onClick = onToggleFavorite) {
        Icon(
          imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
          contentDescription = "Favorite",
          tint = if (isFavorite) ErrorRed else TextSecondary,
          modifier = Modifier.size(20.dp)
        )
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(CyberBlue.copy(alpha = 0.15f))
          .clickable(onClick = onClick)
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text("Open", color = CyberBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

// ==========================================
// 4. CRYPTO WALLET SECTION
// ==========================================
@Composable
fun CryptoTab(viewModel: ToninoViewModel) {
  val walletState by viewModel.walletState.collectAsState()
  var showConnectDialog by remember { mutableStateOf(false) }
  var showReceiveDialog by remember { mutableStateOf(false) }
  var showSendDialog by remember { mutableStateOf(false) }
  var showSwapDialog by remember { mutableStateOf(false) }

  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MidnightBlue)
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.statusBarsPadding())

    // Header
    if (walletState.status == WalletStatus.CONNECTED) {
      GeometricHeader(
        title = "Web3 Client",
        subtitle = walletState.walletType ?: "",
        badgeText = "DISCONNECT TON",
        badgeColor = TokenGreen,
        onBadgeClick = { viewModel.disconnectWallet() }
      )
    } else {
      GeometricHeader(
        title = "Web3 Client",
        subtitle = "Disconnected",
        badgeText = "CONNECT WALLET",
        badgeColor = CyberBlue,
        onBadgeClick = { showConnectDialog = true }
      )
    }

    // Dynamic state representation
    when (walletState.status) {
      WalletStatus.NOT_CONNECTED -> {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.AccountBalanceWallet, "Wallet connect", modifier = Modifier.size(70.dp), tint = CyberBlue)
              Spacer(modifier = Modifier.height(16.dp))
              Text("TON Wallet Disconnected", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                "Connect your Tonkeeper, MyTonWallet, or Telegram Wallet to pay physical partners, tip in chats, swap Tonino tokens, and view transactions.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(20.dp))
              Button(
                onClick = { showConnectDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("wallet_connect_cta_btn")
              ) {
                Text("Connect TON Account", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      WalletStatus.CONNECTING -> {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          GlassCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = CyberBlue, modifier = Modifier.size(50.dp))
              Spacer(modifier = Modifier.height(16.dp))
              Text("Connecting securely...", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
              Text("Please authorize connection inside ${walletState.walletType}", color = TextSecondary, fontSize = 12.sp)
              Spacer(modifier = Modifier.height(16.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                  onClick = { viewModel.finalizeWalletConnection(true) },
                  colors = ButtonDefaults.buttonColors(containerColor = TokenGreen)
                ) {
                  Text("SIMULATE ACCEPT")
                }
                Button(
                  onClick = { viewModel.finalizeWalletConnection(false) },
                  colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                  Text("SIMULATE REJECT")
                }
              }
            }
          }
        }
      }

      WalletStatus.ERROR -> {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          GlassCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = ErrorRed)
              Spacer(modifier = Modifier.height(12.dp))
              Text("Connection Rejected", fontWeight = FontWeight.Bold, color = TextPrimary)
              Text("User declined to associate TON keypair with Tonino client.", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
              Spacer(modifier = Modifier.height(16.dp))
              Button(onClick = { viewModel.disconnectWallet() }) {
                Text("Retry Connection")
              }
            }
          }
        }
      }

      WalletStatus.CONNECTED -> {
        // Balanced dashboard UI
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // 1. Balance Portfolio Card
          item {
            GlassCard(
              borderColor = CyberBlue.copy(alpha = 0.5f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Total Crypto Portfolio", color = TextSecondary, fontSize = 12.sp)
                Text(
                  "$${"%,.2f".format(walletState.portfolioValueUsd)}",
                  fontWeight = FontWeight.Bold,
                  fontSize = 28.sp,
                  color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MidnightBlue)
                    .clickable {
                      walletState.address?.let { addr ->
                        clipboardManager.setText(AnnotatedString(addr))
                      }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = walletState.address ?: "",
                    color = CyberBlue,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(Icons.Default.ContentCopy, "copy", tint = CyberBlue, modifier = Modifier.size(10.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = GlassOverlay)
                Spacer(modifier = Modifier.height(16.dp))

                // Assets break up
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceAround
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TON Balance", color = TextSecondary, fontSize = 11.sp)
                    Text("${walletState.tonBalance} TON", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                  }
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TONI Tokens", color = TextSecondary, fontSize = 11.sp)
                    Text("${walletState.toniBalance} TONI", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                  }
                }
              }
            }
          }

          // 2. Action buttons
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .background(CyberBlue)
                  .clickable { showSendDialog = true }
                  .padding(12.dp),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.ArrowOutward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Send TON", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .background(DarkSlate)
                  .border(0.5.dp, GlassOverlay, RoundedCornerShape(12.dp))
                  .clickable { showReceiveDialog = true }
                  .padding(12.dp),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.QrCode, null, tint = CyberBlue, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Receive", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .background(DarkSlate)
                  .border(0.5.dp, GlassOverlay, RoundedCornerShape(12.dp))
                  .clickable { showSwapDialog = true }
                  .padding(12.dp),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.SwapCalls, null, tint = CyberPurple, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Swap", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
              }
            }
          }

          // 3. Transactions List
          item {
            Text("RECENT WEB3 ACTIVITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberPurple)
          }

          items(walletState.txHistory) { tx ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSlate)
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(
                    if (tx.type == "Receive") TokenGreen.copy(alpha = 0.15f)
                    else if (tx.type == "Swap") CyberPurple.copy(alpha = 0.15f)
                    else CyberBlue.copy(alpha = 0.15f)
                  ),
                contentAlignment = Alignment.Center
              ) {
                val txIcon = when (tx.type) {
                  "Receive" -> Icons.Default.ArrowDownward
                  "Swap" -> Icons.Default.SwapHoriz
                  else -> Icons.Default.ArrowUpward
                }
                Icon(
                  txIcon,
                  contentDescription = null,
                  tint = if (tx.type == "Receive") TokenGreen else if (tx.type == "Swap") CyberPurple else CyberBlue,
                  modifier = Modifier.size(18.dp)
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${tx.type} ${tx.amount} ${tx.unit}",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = TextPrimary
                )
                Text(
                  text = "${tx.address} · ${tx.date} at ${tx.time}",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(TokenGreen.copy(alpha = 0.15f))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text("Success", fontSize = 9.sp, color = TokenGreen, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }

  // DIALOG FOR CONNECT SELECTOR
  if (showConnectDialog) {
    Dialog(onDismissRequest = { showConnectDialog = false }) {
      GlassCard(borderColor = CyberPurple) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Connect Wallet Provider", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
          IconButton(onClick = { showConnectDialog = false }) {
            Icon(Icons.Default.Close, null, tint = TextSecondary)
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          "We support real TON Wallet standards. Select your prefered app below to begin handshake authentication.",
          color = TextSecondary,
          fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        listOf(
          Pair("Tonkeeper", "Popular, customizable client"),
          Pair("MyTonWallet", "Highly precise ledger integrations"),
          Pair("Telegram Wallet", "Integrated Telegram platform native"),
          Pair("WalletConnect", "Access via QR code scanner link")
        ).forEach { (provider, desc) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MidnightBlue)
              .clickable {
                viewModel.selectWalletProviderAndConnect(provider)
                showConnectDialog = false
              }
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(CyberBlue.copy(alpha = 0.15f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Link, null, tint = CyberBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(provider, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
              Text(desc, color = TextSecondary, fontSize = 11.sp)
            }
          }
        }
      }
    }
  }

  // DIALOG FOR RECEIVE TOKEN
  if (showReceiveDialog) {
    Dialog(onDismissRequest = { showReceiveDialog = false }) {
      GlassCard(borderColor = CyberBlue) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("Your TON Deposit Address", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
          IconButton(onClick = { showReceiveDialog = false }) {
            Icon(Icons.Default.Close, null, tint = TextSecondary)
          }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Large simulated QR code placeholder
        Box(
          modifier = Modifier
            .size(160.dp)
            .align(Alignment.CenterHorizontally)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(2.dp, CyberBlue, RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(110.dp), tint = Color.Black)
            Text("TONINO TON PAY", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = walletState.address ?: "",
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          color = CyberBlue,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = {
            walletState.address?.let { clipboardManager.setText(AnnotatedString(it)) }
            showReceiveDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Copy Wallet Address")
        }
      }
    }
  }

  // DIALOG FOR SEND TOKEN
  if (showSendDialog) {
    var rawAddress by remember { mutableStateOf("EQB3...wXyZ9") }
    var rawAmount by remember { mutableStateOf("1.5") }
    var opSuccessMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { showSendDialog = false }) {
      GlassCard(borderColor = CyberPurple) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("Send TON outbound", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
          IconButton(onClick = { showSendDialog = false }) {
            Icon(Icons.Default.Close, null, tint = TextSecondary)
          }
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = rawAddress,
          onValueChange = { rawAddress = it },
          label = { Text("Receiver Address", color = TextSecondary) },
          colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = rawAmount,
          onValueChange = { rawAmount = it },
          label = { Text("Amount in TON", color = TextSecondary) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (opSuccessMsg.isNotEmpty()) {
          Text(opSuccessMsg, color = TokenGreen, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
          Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
          onClick = {
            val amt = rawAmount.toDoubleOrNull() ?: 0.0
            val done = viewModel.triggerQuickTransaction(rawAddress, amt)
            if (done) {
              opSuccessMsg = "Transferred $amt TON outbound!"
            } else {
              opSuccessMsg = "Fail: Insufficient funds!"
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Broadcast Transaction")
        }
      }
    }
  }

  // DIALOG FOR SWAP TOKEN
  if (showSwapDialog) {
    var swapAmount by remember { mutableStateOf("1.0") }
    var simulatedGained by remember { mutableStateOf(100.0) }

    Dialog(onDismissRequest = { showSwapDialog = false }) {
      GlassCard(borderColor = CyberPurple) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("Unified DEX Liquidity Pool", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
          IconButton(onClick = { showSwapDialog = false }) {
            Icon(Icons.Default.Close, null, tint = TextSecondary)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Current Pool Ratio: 1 TON = 100 TONI", fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = swapAmount,
          onValueChange = {
            swapAmount = it
            val dblVal = it.toDoubleOrNull() ?: 0.0
            simulatedGained = dblVal * 100.0
          },
          label = { Text("Swap (Sell) TON", color = TextSecondary) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        Icon(Icons.Default.SwapVert, null, tint = CyberPurple, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(8.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MidnightBlue)
            .padding(12.dp)
        ) {
          Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Receive TONI (estimated)", color = TextSecondary, fontSize = 12.sp)
            Text("$simulatedGained TONI", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            val amt = swapAmount.toDoubleOrNull() ?: 0.0
            viewModel.triggerSimulatedSwap(amt)
            showSwapDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Connect Pool & Execute Swap")
        }
      }
    }
  }
}

// ==========================================
// 5. SERVICES GRID TAB
// ==========================================
@Composable
fun ServicesTab(
  viewModel: ToninoViewModel,
  onOpenService: (String) -> Unit
) {
  val services by viewModel.services.collectAsState()
  val searchQuery by viewModel.servicesSearchQuery.collectAsState()
  val selectedCat by viewModel.selectedCategory.collectAsState()

  // Extract exclusive category strings
  val categories = listOf("All", "Dating", "Marketplace", "Crypto", "Restaurants", "Jobs", "Real Estate", "Transport", "Games", "Local Services")

  val filteredGrid = services.filter {
    (selectedCat == "All" || it.category == selectedCat) &&
      (it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true))
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MidnightBlue)
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.statusBarsPadding())

    // Direct header
    GeometricHeader(
      title = "Shortcuts",
      subtitle = "Services",
      badgeText = "20+ Apps",
      badgeColor = CyberPurple
    )

    // Dynamic search inputs
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { viewModel.searchServices(it) },
      placeholder = { Text("Search services...", color = TextSecondary) },
      leadingIcon = { Icon(Icons.Default.FilterList, "Filter", tint = TextSecondary) },
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DarkSlate,
        unfocusedContainerColor = DarkSlate,
        focusedBorderColor = CyberPurple,
        unfocusedBorderColor = GlassOverlay,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
      ),
      shape = RoundedCornerShape(12.dp),
      singleLine = true
    )

    // Category chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      categories.forEach { categ ->
        val isSelected = selectedCat == categ
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CyberPurple else DarkSlate)
            .border(0.5.dp, if (isSelected) NeonCyan else GlassOverlay, RoundedCornerShape(12.dp))
            .clickable { viewModel.selectCategory(categ) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Text(
            text = categ,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }

    // Dynamic grid displaying as mobile apps icons!
    Box(modifier = Modifier.weight(1f)) {
      if (filteredGrid.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Apps, "grid", modifier = Modifier.size(64.dp), tint = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("No integrated services match current filter.", color = TextSecondary, fontSize = 14.sp)
          }
        }
      } else {
        LazyVerticalGrid(
          columns = GridCells.Fixed(3),
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(filteredGrid) { service ->
            ServiceAppIconFormat(
              service = service,
              onClick = { onOpenService(service.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun getGradientForCategory(category: String): Brush {
  return when (category.lowercase()) {
    "dating" -> Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFE11D48)))
    "jobs" -> Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFEA580C)))
    "games" -> Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF9333EA)))
    "transport", "travel" -> Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF0D9488)))
    "real estate", "housing" -> Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB)))
    "marketplace", "ads" -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)))
    "crypto" -> Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF06B6D4))) // indigo to cyan
    "local services", "restaurants", "local" -> Brush.linearGradient(listOf(Color(0xFF22D3EE), Color(0xFF2563EB)))
    else -> Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF334155))) // slate slate
  }
}

@Composable
fun ServiceAppIconFormat(service: PartnerService, onClick: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(contentAlignment = Alignment.TopEnd) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(getGradientForCategory(service.category))
          .border(0.5.dp, GlassOverlay, RoundedCornerShape(16.dp))
          .border(
            1.dp,
            if (service.isFeatured) Color.White.copy(alpha = 0.5f) else Color.Transparent,
            RoundedCornerShape(16.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        val iVector = MockData.getIconFromString(service.iconName)
        Icon(
          imageVector = iVector,
          contentDescription = service.name,
          tint = Color.White,
          modifier = Modifier.size(24.dp)
        )
      }

      // Hot Badge Overlay
      if (service.isHot) {
        Box(
          modifier = Modifier
            .offset(x = 4.dp, y = (-4).dp)
            .background(ErrorRed, CircleShape)
            .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
          Text("HOT", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
      } else if (service.isVerified) {
        Box(
          modifier = Modifier
            .offset(x = 4.dp, y = (-4).dp)
            .background(NeonCyan, CircleShape)
            .padding(horizontal = 3.dp, vertical = 2.dp)
        ) {
          Icon(Icons.Default.Check, null, tint = MidnightBlue, modifier = Modifier.size(8.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Rounded Title Text representation
    Text(
      text = service.name,
      fontWeight = FontWeight.Bold,
      color = TextPrimary,
      fontSize = 11.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center
    )

    Text(
      text = service.category,
      color = TextSecondary,
      fontSize = 9.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(2.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.Star, null, tint = GoldAccent, modifier = Modifier.size(9.dp))
      Spacer(modifier = Modifier.width(2.dp))
      Text(
        text = "${service.rating}",
        color = TextSecondary,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

// ==========================================
// 6. DETAILED SERVICE DIALOG WINDOW
// ==========================================
@Composable
fun ServiceDetailDialog(
  serviceId: String,
  viewModel: ToninoViewModel,
  onDismiss: () -> Unit
) {
  val services by viewModel.services.collectAsState()
  val reviews by viewModel.reviews.collectAsState()
  val currentFavorites by viewModel.favorites.collectAsState()
  val walletState by viewModel.walletState.collectAsState()

  val partner = services.firstOrNull { it.id == serviceId } ?: return
  val relatedReviews = reviews.filter { it.serviceId == serviceId }
  val isFav = currentFavorites.contains(partner.id)

  var userCommentText by remember { mutableStateOf("") }
  var userRatingScore by remember { mutableIntStateOf(5) }
  var ratingSubmittedNotice by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 520.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(DarkSlate)
        .border(1.dp, CyberBlue.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(18.dp)
      ) {
        // Core HEADER (Image placeholder + description labels)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MidnightBlue),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = MockData.getIconFromString(partner.iconName),
                contentDescription = null,
                tint = CyberBlue,
                modifier = Modifier.size(28.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(partner.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (partner.isVerified) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(Icons.Default.Verified, "Verified badge partner", tint = NeonCyan, modifier = Modifier.size(16.dp))
                }
              }
              Text("${partner.category} · Platform Mini App", fontSize = 12.sp, color = TextSecondary)
              Text("Available in ${partner.city ?: "Global"}", fontSize = 11.sp, color = CyberPurple, fontWeight = FontWeight.SemiBold)
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, null, tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Descriptions & Stats Grid row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MidnightBlue)
            .padding(8.dp),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Rating Score", color = TextSecondary, fontSize = 9.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Star, null, tint = GoldAccent, modifier = Modifier.size(12.dp))
              Text("${partner.rating}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Reviews", color = TextSecondary, fontSize = 9.sp)
            Text("${partner.reviewsCount} users", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Distance", color = TextSecondary, fontSize = 9.sp)
            Text("${partner.distanceKm ?: 1.0} km away", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Inner scrollable logic for descriptions + reviews
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
        ) {
          Text("SERVICE DESCRIPTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberPurple)
          Spacer(modifier = Modifier.height(4.dp))
          Text(partner.description, color = TextPrimary, fontSize = 13.sp, lineHeight = 18.sp)

          Spacer(modifier = Modifier.height(16.dp))

          // Key primary action buttons (Open, Save/Favorite, Share)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Button(
              onClick = {
                // Instantly simulated launching as a mini-client app
                onDismiss()
                viewModel.navigateTo(Screen.Chats)
                // Add message to concatenation
                viewModel.sendMessage("chat_lovematch", "👋 Just opened mini-app: ${partner.name}. Let me browse items!")
              },
              colors = ButtonDefaults.buttonColors(containerColor = CyberBlue),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1.5f)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RocketLaunch, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open App", fontSize = 13.sp, fontWeight = FontWeight.Bold)
              }
            }

            Box(
              modifier = Modifier
                .size(width = 44.dp, height = 40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isFav) ErrorRed.copy(alpha = 0.2f) else MidnightBlue)
                .clickable { viewModel.toggleFavorite(partner.id) }
                .padding(8.dp),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isFav) ErrorRed else TextSecondary
              )
            }

            Box(
              modifier = Modifier
                .size(width = 44.dp, height = 40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MidnightBlue)
                .clickable {
                  onDismiss()
                  viewModel.navigateTo(Screen.Chats)
                }
                .padding(8.dp),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Share, "Share in chat bubble", tint = CyberBlue)
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // REVIEWS SECTION
          Text("USER RATINGS (${relatedReviews.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
          Spacer(modifier = Modifier.height(4.dp))

          if (relatedReviews.isEmpty()) {
            Text("No user reviews yet. Be the first to leave review feedback!", color = TextSecondary, fontSize = 11.sp)
          } else {
            relatedReviews.forEach { rev ->
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MidnightBlue)
                  .padding(10.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(rev.username, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                    if (rev.isVerifiedBadge) {
                      Spacer(modifier = Modifier.width(4.dp))
                      Icon(Icons.Default.VerifiedUser, "verified user", tint = TokenGreen, modifier = Modifier.size(10.dp))
                    }
                  }
                  Row {
                    repeat(rev.rating) {
                      Icon(Icons.Default.Star, null, tint = GoldAccent, modifier = Modifier.size(10.dp))
                    }
                  }
                }
                Text(rev.comment, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                Text(rev.date, color = TextSecondary, fontSize = 9.sp, modifier = Modifier.align(Alignment.End))

                // Reply from partners logic
                rev.replyContent?.let { rep ->
                  Spacer(modifier = Modifier.height(4.dp))
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(4.dp))
                      .background(DarkSlate)
                      .padding(6.dp)
                  ) {
                    Column {
                      Text("Partner Reply Match:", color = CyberBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                      Text(rep, color = TextPrimary, fontSize = 11.sp)
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // NEW REVIEW SUBMISSION FORM
          Text("WRITE A REVIEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberPurple)
          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Score:", fontSize = 11.sp, color = TextSecondary)
            (1..5).forEach { num ->
              val active = num <= userRatingScore
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (active) GoldAccent else Color.DarkGray,
                modifier = Modifier
                  .size(24.dp)
                  .clickable { userRatingScore = num }
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          OutlinedTextField(
            value = userCommentText,
            onValueChange = { userCommentText = it },
            placeholder = { Text("Write verified review comment...", color = TextSecondary) },
            textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CyberPurple,
              unfocusedBorderColor = GlassOverlay
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(64.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          if (ratingSubmittedNotice.isNotEmpty()) {
            Text(ratingSubmittedNotice, color = TokenGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
          }

          Button(
            onClick = {
              if (userCommentText.trim().isNotEmpty()) {
                viewModel.submitReview(
                  serviceId = partner.id,
                  username = walletState.address?.take(6) ?: "anonymous_toni",
                  rating = userRatingScore,
                  comment = userCommentText
                )
                ratingSubmittedNotice = "Review submitted live!"
                userCommentText = ""
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Publish Verified Review", fontSize = 12.sp)
          }
        }
      }
    }
  }
}

// ==========================================
// 7. PROFILE TAB
// ==========================================
@Composable
fun ProfileTab(viewModel: ToninoViewModel) {
  val profile by viewModel.userProfile.collectAsState()
  val walletState by viewModel.walletState.collectAsState()
  val favs by viewModel.favorites.collectAsState()
  val isAdminSimulated by viewModel.isAdminSimulated.collectAsState()

  val clipboardManager = LocalClipboardManager.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MidnightBlue)
      .padding(horizontal = 16.dp)
      .verticalScroll(rememberScrollState())
  ) {
    Spacer(modifier = Modifier.statusBarsPadding())

    GeometricHeader(
      title = "Member Portal",
      subtitle = "Profile",
      badgeText = if (isAdminSimulated) "ADMIN SIMULATED" else "VERIFIED TONINO",
      badgeColor = if (isAdminSimulated) ErrorRed else GoldAccent
    )

    // Profile top card centering
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(80.dp)
          .background(
            Brush.linearGradient(listOf(CyberBlue, CyberPurple)),
            CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        Text("CN", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text("@${profile.username}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
      Text("User ID: ${profile.userId} · Verified", color = TextSecondary, fontSize = 12.sp)

      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(CyberPurple.copy(alpha = 0.2f))
          .padding(horizontal = 12.dp, vertical = 4.dp)
      ) {
        Text("Rank Level: ${profile.rankLevel}", color = CyberPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }

    // Referral and Rewards card
    Text("REFERRAL & TONI POINTS REWARDS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
    Spacer(modifier = Modifier.height(6.dp))

    GlassCard(borderColor = NeonCyan) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("TONI Rewards Balance", color = TextSecondary, fontSize = 11.sp)
          Text("${profile.rewardsBalance} TONI", fontWeight = FontWeight.Bold, color = TokenGreen, fontSize = 20.sp)
        }

        Button(
          onClick = { viewModel.claimAdBonusReward() },
          colors = ButtonDefaults.buttonColors(containerColor = TokenGreen)
        ) {
          Text("Claim Daily", fontSize = 11.sp)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Divider(color = GlassOverlay)
      Spacer(modifier = Modifier.height(12.dp))

      Text("Your Referral invite link:", color = TextSecondary, fontSize = 12.sp)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(MidnightBlue)
          .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "https://tonino.me/join?ref=${profile.referralCode}",
          color = CyberBlue,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )

        IconButton(
          onClick = { clipboardManager.setText(AnnotatedString("https://tonino.me/join?ref=${profile.referralCode}")) },
          modifier = Modifier.size(28.dp)
        ) {
          Icon(Icons.Default.ContentCopy, null, tint = CyberBlue, modifier = Modifier.size(14.dp))
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Invited Friends Count:", fontSize = 12.sp, color = TextSecondary)
        Text("${profile.invitedCount} members", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Preferences Logic & Saved features
    Text("SAVED SERVICES & SYSTEM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberPurple)
    Spacer(modifier = Modifier.height(6.dp))

    GlassCard {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Favorite, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Favorited partners:", color = TextPrimary, fontSize = 13.sp)
        }
        Text("${favs.size} loaded", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
      }

      Divider(color = GlassOverlay, modifier = Modifier.padding(vertical = 8.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Security, null, tint = CyberBlue, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Account Security:", color = TextPrimary, fontSize = 13.sp)
        }
        Text("2FA Engaged", color = TokenGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Admin Toggle options (for demonstration/evaluation)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(CyberPurple.copy(alpha = 0.1f))
        .border(1.dp, CyberPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Simulate Partner Admin Privileges", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
        Text("Toggle to preview the partner verification controls.", color = TextSecondary, fontSize = 11.sp)
      }

      Switch(
        checked = isAdminSimulated,
        onCheckedChange = { viewModel.toggleAdminMock(it) },
        colors = SwitchDefaults.colors(
          checkedThumbColor = CyberPurple,
          checkedTrackColor = CyberPurple.copy(alpha = 0.39f)
        )
      )
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

// ==========================================
// 8. ADMIN / PARTNER MANAGEMENT ROW PANEL
// ==========================================
@Composable
fun PartnerAdminPanel(viewModel: ToninoViewModel) {
  val services by viewModel.services.collectAsState()

  var isFormOpen by remember { mutableStateOf(false) }

  // Form input variables
  var newName by remember { mutableStateOf("") }
  var newCategory by remember { mutableStateOf("Dating") }
  var newDesc by remember { mutableStateOf("") }
  var newIcon by remember { mutableStateOf("Favorite") }
  var newCity by remember { mutableStateOf("Berlin") }
  var isFeatured by remember { mutableStateOf(true) }
  var isHot by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MidnightBlue)
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.statusBarsPadding())

    // Admin headers
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text("Administrative Portal", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
        Text("Authorized partner control workspace", color = TextSecondary, fontSize = 12.sp)
      }

      Button(
        onClick = { isFormOpen = !isFormOpen },
        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text(if (isFormOpen) "Close Add" else "+ Add Partner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }

    // FORM ACCORDION TRANSITION
    AnimatedVisibility(
      visible = isFormOpen,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut()
    ) {
      GlassCard(borderColor = ErrorRed) {
        Text("REGISTER NEW MINI PARTNER SERVICE", fontWeight = FontWeight.Bold, color = ErrorRed, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = newName,
          onValueChange = { newName = it },
          label = { Text("Service/App name", color = TextSecondary) },
          colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = newCategory,
            onValueChange = { newCategory = it },
            label = { Text("Category", color = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = newCity,
            onValueChange = { newCity = it },
            label = { Text("Local City", color = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = newDesc,
          onValueChange = { newDesc = it },
          label = { Text("Brief Service description details", color = TextSecondary) },
          colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Checkbox(checked = isFeatured, onCheckedChange = { isFeatured = it })
          Text("Featured Partner status", fontSize = 12.sp, color = TextPrimary)
          Spacer(modifier = Modifier.width(16.dp))
          Checkbox(checked = isHot, onCheckedChange = { isHot = it })
          Text("Hot Deal highlight", fontSize = 12.sp, color = TextPrimary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = {
            if (newName.isNotBlank() && newDesc.isNotBlank()) {
              // Map Icon name dynamically
              val iconsList = listOf("Favorite", "ShoppingCart", "Campaign", "Wallet", "Work", "Home", "Restaurant", "DirectionsCar", "Gamepad", "Event", "Laptop", "Sell")
              val mappedIcon = iconsList.random()
              viewModel.addPartnerService(
                name = newName,
                category = newCategory,
                description = newDesc,
                iconName = mappedIcon,
                city = newCity,
                country = "Germany",
                isFeatured = isFeatured,
                isHot = isHot
              )
              // Reset values
              newName = ""
              newDesc = ""
              isFormOpen = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = TokenGreen),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Commit Registration to ecosystem", fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text("CURRENT REGISTERED ACTIVE NETWORK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberPurple)
    Spacer(modifier = Modifier.height(6.dp))

    // List of existing partners to toggle active/disabled states
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(services) { partner ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSlate)
            .border(
              0.5.dp,
              if (partner.status == "active") GlassOverlay else ErrorRed.copy(alpha = 0.5f),
              RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = MockData.getIconFromString(partner.iconName),
              contentDescription = null,
              tint = if (partner.status == "active") CyberBlue else TextSecondary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(partner.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
              Text("Category: ${partner.category} · City: ${partner.city ?: "Global"}", color = TextSecondary, fontSize = 11.sp)
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (partner.status == "active") TokenGreen.copy(0.15f) else Color.DarkGray)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                  Text(partner.status.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (partner.status == "active") TokenGreen else Color.Gray)
                }
                Spacer(modifier = Modifier.width(4.dp))
                if (partner.isFeatured) {
                  Text("· FEATURED", fontSize = 8.sp, color = CyberPurple, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          // Management actions toggle
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (partner.status == "active") {
              Button(
                onClick = { viewModel.togglePartnerStatus(partner.id, "disabled") },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(0.15f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
              ) {
                Text("Disable", color = ErrorRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
            } else {
              Button(
                onClick = { viewModel.togglePartnerStatus(partner.id, "active") },
                colors = ButtonDefaults.buttonColors(containerColor = TokenGreen.copy(0.15f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
              ) {
                Text("Activate", color = TokenGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

// Utility spacer extension for standard padding
fun Modifier.padding(symmetric: androidx.compose.ui.unit.Dp, vertical: androidx.compose.ui.unit.Dp): Modifier {
  return this.padding(horizontal = symmetric, vertical = vertical)
}

// ==========================================
// 9. TELEGRAM MINI APP SIMULATION WRAPPER & CONSOLE
// ==========================================

data class TmaMainButtonConfig(
  val label: String,
  val onClick: () -> Unit
)

fun getTmaMainButtonConfig(
  viewModel: ToninoViewModel,
  screen: Screen,
  selectedServiceId: String?,
  selectedChatId: String?
): TmaMainButtonConfig? {
  // Swap screens logic trigger
  if (screen is Screen.Crypto && selectedChatId == null && selectedServiceId == null) {
    val walletState = viewModel.walletState.value
    if (walletState.status == WalletStatus.CONNECTED) {
      return TmaMainButtonConfig(
        label = "Execute Simulated Token Swap",
        onClick = {
          viewModel.triggerSimulatedSwap(1.0)
          viewModel.logTmaAction("MainButton triggered: Swap 1.0 TON to 100.0 TONI")
        }
      )
    } else {
      return TmaMainButtonConfig(
        label = "Authorize TON Connect API",
        onClick = {
          viewModel.selectWalletProviderAndConnect("Telegram Wallet")
          viewModel.finalizeWalletConnection(true)
          viewModel.logTmaAction("MainButton triggered: TON Connect request parsed successfully")
        }
      )
    }
  }

  // Favorites bookmarker button inside details
  if (selectedServiceId != null) {
    return TmaMainButtonConfig(
      label = "Bookmark Service & Sync (TMA Cloud)",
      onClick = {
        viewModel.toggleFavorite(selectedServiceId)
        viewModel.logTmaAction("MainButton callback: Bookmark toggle saved")
        viewModel.saveToTgCloudStorage("fav_$selectedServiceId", "true")
      }
    )
  }

  return null
}

@Composable
fun TelegramSimulatorWrapper(
  viewModel: ToninoViewModel,
  content: @Composable () -> Unit
) {
  val miniAppEnabled by viewModel.tgMiniAppModeEnabled.collectAsState()
  val themeMode by viewModel.tgThemeMode.collectAsState()
  val viewportExpanded by viewModel.tgViewportExpanded.collectAsState()
  val safeInsetsEnabled by viewModel.tgSafeInsetsEnabled.collectAsState()
  val showConsole by viewModel.tgShowConsole.collectAsState()
  val currentScreen by viewModel.currentScreen.collectAsState()
  val selectedChatId by viewModel.selectedChatId.collectAsState()
  val selectedServiceId by viewModel.selectedServiceId.collectAsState()

  if (!miniAppEnabled) {
    Box(modifier = Modifier.fillMaxSize()) {
      content()
      TelegramConsoleTrigger(viewModel = viewModel)
    }
    return
  }

  // Background frame styling dynamically matched
  val tgBgColor = when (themeMode) {
    "cyber_midnight" -> Color(0xFF030814)
    "tg_dark" -> Color(0xFF0F1720)
    "tg_light" -> Color(0xFFE3E6E9)
    else -> Color(0xFF18222D)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(tgBgColor)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // 1. Simulated Device Status Bar / Notch
      if (safeInsetsEnabled) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color.Black.copy(0.3f)),
          contentAlignment = Alignment.Center
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("09:41", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Box(
              modifier = Modifier
                .width(110.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(Color.Black)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.Wifi, null, tint = Color.White, modifier = Modifier.size(12.dp))
              Icon(Icons.Default.BatteryChargingFull, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
          }
        }
      }

      // 2. Simulated Telegram Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(if (themeMode == "tg_light") Color(0xFFFFFFFF) else Color(0xFF1A2734))
          .border(0.5.dp, Color.White.copy(0.08f), RoundedCornerShape(0.dp))
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              viewModel.logTmaAction("BackButton clicked: popping nested WebApp hierarchy")
              viewModel.triggerHapticImpact("medium")
              viewModel.selectChat(null)
              viewModel.selectService(null)
            },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              Icons.Default.ArrowBack,
              "BackButton",
              tint = if (themeMode == "tg_light") Color(0xFF2481CC) else Color.White,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              "Tonino Hub",
              fontWeight = FontWeight.Bold,
              color = if (themeMode == "tg_light") Color.Black else Color.White,
              fontSize = 15.sp
            )
            Text(
              "bot",
              color = if (themeMode == "tg_light") Color.Gray else Color(0xFF50B2FF),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(if (themeMode == "tg_light") Color(0xFFF1F1F1) else Color(0x33FFFFFF))
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.MoreHoriz,
              null,
              tint = if (themeMode == "tg_light") Color.DarkGray else Color.White,
              modifier = Modifier.size(16.dp).clickable {
                viewModel.logTmaAction("TMA Menu context modal opened")
              }
            )
            Box(
              modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .border(2.dp, if (themeMode == "tg_light") Color.DarkGray else Color.White, CircleShape)
                .clickable {
                  viewModel.logTmaAction("TMA onCloseApp command signals sent")
                }
            )
          }
        }
      }

      // Viewport constraint framework
      val heightFraction = if (viewportExpanded) 1f else 0.72f
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(heightFraction)
          .clip(
            if (viewportExpanded) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
          )
          .background(MidnightBlue)
      ) {
        content()
      }

      // Simulated backdrop background chats
      if (!viewportExpanded) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(0.28f)
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable {
              viewModel.toggleTgViewport()
            },
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.KeyboardDoubleArrowUp, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text("TAP TO EXPAND TONINO WEBAPP", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Dynamic Native Main Button simulation inside the active view context!
      val mainButtonConfig = getTmaMainButtonConfig(viewModel, currentScreen, selectedServiceId, selectedChatId)
      mainButtonConfig?.let { config ->
        Button(
          onClick = {
            viewModel.triggerHapticNotification("success")
            config.onClick()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2481CC)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(50.dp)
            .testTag("tma_main_button")
        ) {
          Text(config.label.uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
        }
      }

      // Simulated Device Bottom Safe Area Home Bar
      if (safeInsetsEnabled) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(Color.Black.copy(0.3f)),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .width(140.dp)
              .height(5.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(Color.White)
          )
        }
      }
    }

    // Toggle overlay triggers
    TelegramConsoleTrigger(viewModel = viewModel)
    if (showConsole) {
      TelegramConsoleDrawer(viewModel = viewModel)
    }
  }
}

@Composable
fun TelegramConsoleTrigger(viewModel: ToninoViewModel) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(bottom = 80.dp, end = 16.dp),
    contentAlignment = Alignment.BottomEnd
  ) {
    Box(
      modifier = Modifier
        .size(54.dp)
        .clip(CircleShape)
        .background(
          Brush.verticalGradient(
            colors = listOf(CyberBlue, CyberPurple)
          )
        )
        .border(1.5.dp, Color.White, CircleShape)
        .clickable {
          viewModel.setTgShowConsole(true)
        },
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Bolt,
        contentDescription = "Open TMA SDK Console",
        tint = Color.White,
        modifier = Modifier.size(26.dp)
      )
    }
  }
}

@Composable
fun TelegramConsoleDrawer(viewModel: ToninoViewModel) {
  val hapticLogs by viewModel.tgHapticLogs.collectAsState()
  val themeMode by viewModel.tgThemeMode.collectAsState()
  val viewportExpanded by viewModel.tgViewportExpanded.collectAsState()
  val safeInsetsEnabled by viewModel.tgSafeInsetsEnabled.collectAsState()
  val cloudData by viewModel.tgCloudStorage.collectAsState()
  val tmaActive by viewModel.tgMiniAppModeEnabled.collectAsState()

  var activeTab by remember { mutableStateOf("controls") }

  Dialog(onDismissRequest = { viewModel.setTgShowConsole(false) }) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.88f)
        .clip(RoundedCornerShape(24.dp))
        .background(Color(0xFF0C101B))
        .border(1.dp, GlassOverlay, RoundedCornerShape(24.dp))
        .padding(20.dp)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Layers, null, tint = CyberBlue, modifier = Modifier.size(22.dp))
            Column {
              Text("Tonino TMA SDK Simulator", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
              Text("Telegram Mini App Bridge Runtime", color = TextSecondary, fontSize = 10.sp)
            }
          }
          IconButton(onClick = { viewModel.setTgShowConsole(false) }) {
            Icon(Icons.Default.Close, null, tint = Color.Gray)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(0.05f))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          listOf(
            Pair("controls", "TMA API"),
            Pair("initData", "initData"),
            Pair("cloud", "Cloud"),
            Pair("telemetry", "Logs")
          ).forEach { (id, label) ->
            val selected = activeTab == id
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (selected) CyberPurple else Color.Transparent)
                .clickable { activeTab = id }
                .padding(vertical = 6.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                label,
                color = if (selected) Color.White else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
          when (activeTab) {
            "controls" -> {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                item {
                  Text("SIMULATED HOST ENVIRONMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberBlue)
                  Spacer(modifier = Modifier.height(6.dp))
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .background(Color.White.copy(0.04f))
                      .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text("Telegram WebApp Viewport", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                      Text("Toggles simulation wrapper frame", color = TextSecondary, fontSize = 10.sp)
                    }
                    Switch(
                      checked = tmaActive,
                      onCheckedChange = { viewModel.setTgMiniAppModeEnabled(it) },
                      colors = SwitchDefaults.colors(checkedThumbColor = CyberBlue)
                    )
                  }
                }

                item {
                  Text("THEME PARAMETERS (Dynamic SDK adaptation)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberBlue)
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                      Pair("cyber_midnight", "Cyber Slate"),
                      Pair("tg_dark", "TG Dark"),
                      Pair("tg_light", "TG Light")
                    ).forEach { (themeId, label) ->
                      val selected = themeMode == themeId
                      Box(
                        modifier = Modifier
                          .weight(1f)
                          .clip(RoundedCornerShape(10.dp))
                          .background(if (selected) CyberBlue.copy(0.15f) else Color.White.copy(0.04f))
                          .border(
                            1.dp,
                            if (selected) CyberBlue else Color.Transparent,
                            RoundedCornerShape(10.dp)
                          )
                          .clickable { viewModel.setTgThemeMode(themeId) }
                          .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(label, color = if (selected) CyberBlue else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }

                item {
                  Text("VIEWPORT RESIZING & INSETS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberBlue)
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.04f))
                        .clickable { viewModel.toggleTgViewport() }
                        .padding(12.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                          if (viewportExpanded) Icons.Default.AspectRatio else Icons.Default.ZoomOutMap,
                          null,
                          tint = Color.White,
                          modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                          if (viewportExpanded) "Collapse WebApp" else "Expand WebApp",
                          color = Color.White,
                          fontSize = 11.sp,
                          textAlign = TextAlign.Center
                        )
                      }
                    }

                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.04f))
                        .clickable { viewModel.toggleTgSafeInsets() }
                        .padding(12.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                          if (safeInsetsEnabled) Icons.Default.Smartphone else Icons.Default.PhonelinkOff,
                          null,
                          tint = Color.White,
                          modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                          if (safeInsetsEnabled) "Disable Notch Insets" else "Enable Notch Insets",
                          color = Color.White,
                          fontSize = 11.sp,
                          textAlign = TextAlign.Center
                        )
                      }
                    }
                  }
                }

                item {
                  Text("HAPTIC FEEDBACK TEST HARNESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberBlue)
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("light", "medium", "heavy").forEach { impact ->
                      Box(
                        modifier = Modifier
                          .weight(1f)
                          .clip(RoundedCornerShape(8.dp))
                          .background(Color.White.copy(0.04f))
                          .clickable {
                            viewModel.triggerHapticImpact(impact)
                          }
                          .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(impact, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("success", "error", "warning").forEach { notification ->
                      Box(
                        modifier = Modifier
                          .weight(1f)
                          .clip(RoundedCornerShape(8.dp))
                          .background(Color.White.copy(0.04f))
                          .clickable {
                            viewModel.triggerHapticNotification(notification)
                          }
                          .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(notification, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }
            }

            "initData" -> {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                item {
                  Text("SECURE LAUNCH INITIALIZATION PARAMETERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberBlue)
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    "Telegram WebApp launch parameters are passed as secure URL hashes containing encrypted properties verifyable using client signatures.",
                    color = TextSecondary,
                    fontSize = 11.sp
                  )
                }

                item {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .background(Color.Black)
                      .padding(12.dp)
                  ) {
                    Text(
                      text = "tgWebAppStartParam: ${viewModel.tgStartParam}\n\ninitDataRaw:\n${viewModel.tgInitDataRaw}",
                      fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                      fontSize = 10.sp,
                      color = TokenGreen
                    )
                  }
                }

                item {
                  var isHashVerified by remember { mutableStateOf<Boolean?>(null) }
                  var isVerifying by remember { mutableStateOf(false) }

                  Button(
                    onClick = {
                      isVerifying = true
                      viewModel.logTmaAction("Executing WebAppData cryptography check: HMAC-SHA256...")
                      isHashVerified = true
                      isVerifying = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(if (isVerifying) "Verifying HMAC Check..." else "Run Cryptographic Check")
                  }

                  isHashVerified?.let { verified ->
                    Box(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (verified) TokenGreen.copy(0.15f) else ErrorRed.copy(0.15f))
                        .border(1.dp, if (verified) TokenGreen else ErrorRed, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                    ) {
                      Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(
                          if (verified) Icons.Default.CheckCircle else Icons.Default.Error,
                          null,
                          tint = if (verified) TokenGreen else ErrorRed
                        )
                        Column {
                          Text(
                            if (verified) "Integrity check: SUCCESS" else "Integrity check: FAILURE",
                            fontWeight = FontWeight.Bold,
                            color = if (verified) TokenGreen else ErrorRed,
                            fontSize = 13.sp
                          )
                          Text(
                            "Verified with server-side SHA256 bot key sequence integrity: 100% compliant.",
                            color = TextSecondary,
                            fontSize = 10.sp
                          )
                        }
                      }
                    }
                  }
                }
              }
            }

            "cloud" -> {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                item {
                  Text("TELEGRAM CLOUDSTORAGE PERSISTENT SAVES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberBlue)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    "Saves state variables directly to Telegram's cloud storage databases so details remain retained in subsequent app restarts.",
                    color = TextSecondary,
                    fontSize = 11.sp
                  )
                }

                item {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .background(Color.White.copy(0.04f))
                      .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Text("ACTIVE STORAGE KEYS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    if (cloudData.isEmpty()) {
                      Text("Cloud storage database empty", color = TextSecondary, fontSize = 11.sp)
                    } else {
                      cloudData.forEach { (k, v) ->
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Text(k, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = CyberBlue, fontSize = 11.sp)
                          Text(v, color = Color.White, fontSize = 11.sp)
                        }
                      }
                    }
                  }
                }

                item {
                  var newKey by remember { mutableStateOf("") }
                  var newValue by remember { mutableStateOf("") }

                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SET CLOUD STORAGE SANDBOX KEY-VALUE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Key") },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                          unfocusedLabelColor = Color.Gray,
                          focusedLabelColor = CyberBlue,
                          focusedBorderColor = CyberBlue
                        )
                      )
                      OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Value") },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                          unfocusedLabelColor = Color.Gray,
                          focusedLabelColor = CyberBlue,
                          focusedBorderColor = CyberBlue
                        )
                      )
                    }
                    Button(
                      onClick = {
                        if (newKey.trim().isNotEmpty() && newValue.trim().isNotEmpty()) {
                          viewModel.saveToTgCloudStorage(newKey, newValue)
                          newKey = ""
                          newValue = ""
                        }
                      },
                      modifier = Modifier.fillMaxWidth(),
                      colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)
                    ) {
                      Text("Save Key-Value")
                    }
                  }
                }
              }
            }

            "telemetry" -> {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                item {
                  Text("TELEGRAM MiniApp API CALL AUDIT LOGS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberBlue)
                  Spacer(modifier = Modifier.height(4.dp))
                }

                items(hapticLogs) { log ->
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .background(Color.Black.copy(0.4f))
                      .padding(horizontal = 10.dp, vertical = 6.dp)
                  ) {
                    Text(
                      text = log,
                      fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                      fontSize = 11.sp,
                      color = if (log.contains("impact") || log.contains("notification")) NeonCyan else Color.White
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
