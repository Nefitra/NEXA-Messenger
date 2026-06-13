package com.example

import androidx.lifecycle.ViewModel
import com.example.data.MockData
import com.example.types.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

sealed class Screen {
  object Chats : Screen()
  object Discover : Screen()
  object Crypto : Screen()
  object Services : Screen()
  object Profile : Screen()
}

class ToninoViewModel : ViewModel() {

  // ==========================================
  // TELEGRAM MINI APP (TMA) SIMULATION ENGINE
  // ==========================================
  private val _tgMiniAppModeEnabled = MutableStateFlow<Boolean>(true)
  val tgMiniAppModeEnabled: StateFlow<Boolean> = _tgMiniAppModeEnabled.asStateFlow()

  private val _tgThemeMode = MutableStateFlow<String>("cyber_midnight") // "cyber_midnight" | "tg_dark" | "tg_light"
  val tgThemeMode: StateFlow<String> = _tgThemeMode.asStateFlow()

  private val _tgViewportExpanded = MutableStateFlow<Boolean>(true)
  val tgViewportExpanded: StateFlow<Boolean> = _tgViewportExpanded.asStateFlow()

  private val _tgSafeInsetsEnabled = MutableStateFlow<Boolean>(true)
  val tgSafeInsetsEnabled: StateFlow<Boolean> = _tgSafeInsetsEnabled.asStateFlow()

  private val _tgHapticLogs = MutableStateFlow<List<String>>(listOf("TMA Engine initialized: telegram-web-app.js (v7.0)"))
  val tgHapticLogs: StateFlow<List<String>> = _tgHapticLogs.asStateFlow()

  private val _tgCloudStorage = MutableStateFlow<Map<String, String>>(mapOf("user_pref_theme" to "cyber_midnight"))
  val tgCloudStorage: StateFlow<Map<String, String>> = _tgCloudStorage.asStateFlow()

  private val _tgShowConsole = MutableStateFlow<Boolean>(false)
  val tgShowConsole: StateFlow<Boolean> = _tgShowConsole.asStateFlow()

  val tgInitDataRaw = "query_id=AAH4MH0IAwAAAPgwfQgD&user=%7B%22id%22%3A8618331744%2C%22first_name%22%3A%22Boris%22%2C%22last_name%22%3A%22%22%2C%22username%22%3A%22cosmic_tonino%22%2C%22language_code%22%3A%22de%22%2C%22is_premium%22%3Atrue%2C%22allows_write_to_pm%22%3Atrue%7D&auth_date=1718306000&hash=f5be077f19b22e11e8a9fac359487c6ca7e94e775f0a06bd4f2f01fbf9dfbe42"
  val tgStartParam = "ref_8618331744"

  fun setTgMiniAppModeEnabled(enabled: Boolean) {
    _tgMiniAppModeEnabled.value = enabled
    logTmaAction("TMA Mode: ${if (enabled) "ENABLED" else "DISABLED"}")
  }

  fun setTgThemeMode(mode: String) {
    _tgThemeMode.value = mode
    logTmaAction("Theme changed callback: '$mode'")
    saveToTgCloudStorage("user_pref_theme", mode)
  }

  fun toggleTgViewport() {
    _tgViewportExpanded.value = !_tgViewportExpanded.value
    logTmaAction("Viewport: ${_tgViewportExpanded.value.let { if (it) "Expanded (100% height)" else "Minimized (70% height)" }}")
    triggerHapticImpact("medium")
  }

  fun toggleTgSafeInsets() {
    _tgSafeInsetsEnabled.value = !_tgSafeInsetsEnabled.value
    logTmaAction("Safe-area insets: ${_tgSafeInsetsEnabled.value.let { if (it) "BOUNDS_NOTCH (Active)" else "BOUNDS_FLAT (Raw)" }}")
  }

  fun setTgShowConsole(visible: Boolean) {
    _tgShowConsole.value = visible
  }

  fun logTmaAction(message: String) {
    _tgHapticLogs.update { (listOf("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $message") + it).take(30) }
  }

  fun triggerHapticImpact(style: String) {
    logTmaAction("WebApp.HapticFeedback.impactOccurred('$style')")
  }

  fun triggerHapticNotification(type: String) {
    logTmaAction("WebApp.HapticFeedback.notificationOccurred('$type')")
  }

  fun saveToTgCloudStorage(key: String, value: String) {
    _tgCloudStorage.update { it + (key to value) }
    logTmaAction("WebApp.CloudStorage.setItem('$key', '$value')")
  }

  // Current Screen State
  private val _currentScreen = MutableStateFlow<Screen>(Screen.Chats)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  // Primary Partner Services List
  private val _services = MutableStateFlow<List<PartnerService>>(MockData.initialServices)
  val services: StateFlow<List<PartnerService>> = _services.asStateFlow()

  // Reviews list
  private val _reviews = MutableStateFlow<List<ServiceReview>>(MockData.mockReviews)
  val reviews: StateFlow<List<ServiceReview>> = _reviews.asStateFlow()

  // Chats list
  private val _chats = MutableStateFlow<List<Chat>>(MockData.mockChats)
  val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

  // Active chat selected (for detailed view)
  private val _selectedChatId = MutableStateFlow<String?>(null)
  val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

  // Active Partner Service selected (for detail showcase)
  private val _selectedServiceId = MutableStateFlow<String?>(null)
  val selectedServiceId: StateFlow<String?> = _selectedServiceId.asStateFlow()

  // Logged-in Profile
  private val _userProfile = MutableStateFlow<UserProfile>(MockData.defaultUser)
  val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  // Simulated Admin mode toggle (allows testing admin restrictions easily)
  private val _isAdminSimulated = MutableStateFlow<Boolean>(true)
  val isAdminSimulated: StateFlow<Boolean> = _isAdminSimulated.asStateFlow()

  // Wallet State
  private val _walletState = MutableStateFlow<WalletState>(WalletState())
  val walletState: StateFlow<WalletState> = _walletState.asStateFlow()

  // Geo Logic State: Enabled, Disabled, Ask, Manual
  private val _locationPermission = MutableStateFlow<String>("Ask") // "Enabled" | "Disabled" | "Ask" | "Manual"
  val locationPermission: StateFlow<String> = _locationPermission.asStateFlow()

  private val _userCity = MutableStateFlow<String>("Berlin")
  val userCity: StateFlow<String> = _userCity.asStateFlow()

  private val _userCountry = MutableStateFlow<String>("Germany")
  val userCountry: StateFlow<String> = _userCountry.asStateFlow()

  // Search filter query inside Services Grid
  private val _servicesSearchQuery = MutableStateFlow<String>("")
  val servicesSearchQuery: StateFlow<String> = _servicesSearchQuery.asStateFlow()

  // Selected Services Category
  private val _selectedCategory = MutableStateFlow<String>("All")
  val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

  // Favorite/Saved Service IDs set
  private val _favorites = MutableStateFlow<Set<String>>(setOf("lovematch", "foodnear"))
  val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

  // Navigation utilities
  fun navigateTo(screen: Screen) {
    _currentScreen.value = screen
    // When switching top-level screen, close individual detail states safely
    if (screen != Screen.Chats) {
      _selectedChatId.value = null
    }
  }

  fun selectChat(chatId: String?) {
    _selectedChatId.value = chatId
    // Clear unread count when clicking a chat
    if (chatId != null) {
      _chats.update { list ->
        list.map { chat ->
          if (chat.id == chatId) chat.copy(unreadCount = 0) else chat
        }
      }
    }
  }

  fun selectService(serviceId: String?) {
    _selectedServiceId.value = serviceId
  }

  fun searchServices(query: String) {
    _servicesSearchQuery.value = query
  }

  fun selectCategory(category: String) {
    _selectedCategory.value = category
  }

  fun toggleFavorite(serviceId: String) {
    _favorites.update { set ->
      if (set.contains(serviceId)) set - serviceId else set + serviceId
    }
  }

  // Messenger logic: send message
  fun sendMessage(chatId: String, text: String, attachmentServiceId: String? = null) {
    if (text.trim().isEmpty() && attachmentServiceId == null) return

    val newMessage = Message(
      id = UUID.randomUUID().toString(),
      senderId = "user",
      senderName = "Me",
      text = text,
      timestamp = "13:45",
      isSelf = true,
      attachmentServiceId = attachmentServiceId
    )

    _chats.update { list ->
      list.map { chat ->
        if (chat.id == chatId) {
          val updatedMsg = chat.messages + newMessage
          chat.copy(
            messages = updatedMsg,
            lastMessage = if (attachmentServiceId != null) "[Shared Service Card]" else text,
            lastMessageTime = "13:45"
          )
        } else {
          chat
        }
      }
    }

    // Interactive reply simulations to keep prototype alive!
    if (chatId == "chat_pavel") {
      // Simulate quick auto reply from Pavel
      val pavelReply = Message(
        id = UUID.randomUUID().toString(),
        senderId = "pavel",
        senderName = "Pavel Durov",
        text = "Awesome concept! The TON integration looks solid.",
        timestamp = "13:46",
        isSelf = false
      )
      // Delay response simulation
      _chats.update { list ->
        list.map { chat ->
          if (chat.id == chatId) {
            chat.copy(
              messages = chat.messages + pavelReply,
              lastMessage = pavelReply.text,
              lastMessageTime = "13:46"
            )
          } else chat
        }
      }
    }
  }

  // Wallet Logic Simulation Flow
  fun selectWalletProviderAndConnect(provider: String) {
    _walletState.update { it.copy(status = WalletStatus.CONNECTING, walletType = provider) }
    // Simulated successful connection after brief delay (in UI can trigger quick state)
  }

  fun finalizeWalletConnection(success: Boolean) {
    if (success) {
      val mockAddress = "EQC4...9nZ_PX"
      val mockTxs = listOf(
        WalletTx("tx_1", "Receive", 2.5, "TON", "EQB3...q2xL", "Today", "15:45"),
        WalletTx("tx_2", "Swap", 10.0, "TON -> 400 TONI", "Tonino Swap", "Yesterday", "10:14"),
        WalletTx("tx_3", "Send", 1.2, "TON", "EQF0...a_3D", "June 10", "18:22")
      )
      _walletState.update {
        it.copy(
          status = WalletStatus.CONNECTED,
          address = mockAddress,
          tonBalance = 15.68,
          toniBalance = 1250.0,
          portfolioValueUsd = 142.50,
          txHistory = mockTxs
        )
      }
    } else {
      _walletState.update { it.copy(status = WalletStatus.ERROR) }
    }
  }

  fun disconnectWallet() {
    _walletState.value = WalletState()
  }

  fun triggerQuickTransaction(toAddress: String, amount: Double): Boolean {
    val current = _walletState.value
    if (current.status != WalletStatus.CONNECTED || current.tonBalance < amount) return false

    val newTx = WalletTx(
      id = "tx_" + UUID.randomUUID().toString().take(6),
      type = "Send",
      amount = amount,
      unit = "TON",
      address = toAddress.take(12) + "...",
      date = "Today",
      time = "Just now"
    )

    _walletState.update {
      it.copy(
        tonBalance = it.tonBalance - amount,
        portfolioValueUsd = it.portfolioValueUsd - (amount * 7.20),
        txHistory = listOf(newTx) + it.txHistory
      )
    }
    return true
  }

  fun triggerSimulatedSwap(tonAmount: Double) {
    val current = _walletState.value
    if (current.status != WalletStatus.CONNECTED || current.tonBalance < tonAmount) return
    val toniGained = tonAmount * 100.0

    val newTx = WalletTx(
      id = "tx_" + UUID.randomUUID().toString().take(6),
      type = "Swap",
      amount = tonAmount,
      unit = "TON -> $toniGained TONI",
      address = "Tonino Pool",
      date = "Today",
      time = "Just now"
    )

    _walletState.update {
      it.copy(
        tonBalance = it.tonBalance - tonAmount,
        toniBalance = it.toniBalance + toniGained,
        txHistory = listOf(newTx) + it.txHistory
      )
    }
  }

  // Geo Logic Operations
  fun selectLocationPermission(state: String) {
    _locationPermission.value = state
    if (state == "Enabled") {
      _userCity.value = "Berlin"
      _userCountry.value = "Germany"
    } else if (state == "Disabled") {
      _userCity.value = "Default"
      _userCountry.value = "Global"
    }
  }

  fun setManualCity(city: String, country: String) {
    _userCity.value = city
    _userCountry.value = country
    _locationPermission.value = "Manual"
  }

  // Ratings & Reviews Operations
  fun submitReview(serviceId: String, username: String, rating: Int, comment: String) {
    val newReview = ServiceReview(
      id = "rev_" + UUID.randomUUID().toString().take(6),
      serviceId = serviceId,
      username = if (username.trim().isEmpty()) "anonymous_toni" else username,
      rating = rating,
      comment = comment,
      date = "Just now",
      isVerifiedBadge = _walletState.value.status == WalletStatus.CONNECTED
    )

    _reviews.update { it + newReview }

    // Recalculate average rating of the target PartnerService
    _services.update { list ->
      list.map { service ->
        if (service.id == serviceId) {
          val currentTotalScore = service.rating * service.reviewsCount
          val newCount = service.reviewsCount + 1
          val newRating = (currentTotalScore + rating) / newCount
          service.copy(
            rating = Math.round(newRating * 10f) / 10f,
            reviewsCount = newCount
          )
        } else {
          service
        }
      }
    }
  }

  // Partner Administration Operations (Requires check matching ADMIN_USER_IDS)
  fun checkIsAdminUser(): Boolean {
    // Check if user ID matches 8618331744 or Admin simulated mode is on
    return _userProfile.value.userId == "8618331744" && _isAdminSimulated.value
  }

  fun toggleAdminMock(isEnabled: Boolean) {
    _isAdminSimulated.value = isEnabled
  }

  fun addPartnerService(
    name: String,
    category: String,
    description: String,
    iconName: String,
    city: String,
    country: String,
    isFeatured: Boolean,
    isHot: Boolean
  ) {
    val newService = PartnerService(
      id = name.lowercase().replace(" ", "_"),
      name = name,
      category = category,
      description = description,
      iconName = iconName,
      logo = null,
      rating = 5.0f,
      reviewsCount = 1,
      city = city,
      country = country,
      distanceKm = 0.1f,
      isVerified = true,
      isFeatured = isFeatured,
      isHot = isHot,
      status = "active",
      createdAt = "2026-06-13T13:45:00Z"
    )
    _services.update { listOf(newService) + it }
  }

  fun editPartnerService(id: String, updated: PartnerService) {
    _services.update { list ->
      list.map { service ->
        if (service.id == id) updated else service
      }
    }
  }

  fun togglePartnerStatus(id: String, newStatus: String) {
    _services.update { list ->
      list.map { service ->
        if (service.id == id) service.copy(status = newStatus) else service
      }
    }
  }

  // Referral and claim rewards
  fun claimAdBonusReward() {
    _userProfile.update {
      it.copy(
        rewardsBalance = it.rewardsBalance + 25.0,
        rankLevel = if (it.rewardsBalance + 25.0 > 500) "Obsidian" else "Platinum"
      )
    }
  }
}
