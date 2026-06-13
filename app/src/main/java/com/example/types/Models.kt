package com.example.types

// 1. Partner / Service Data Models
data class PartnerService(
  val id: String,
  val name: String,
  val category: String,
  val description: String,
  val iconName: String,               // Will map to Material Symbols dynamically
  val logo: String? = null,
  val rating: Float,
  val reviewsCount: Int,
  val city: String? = null,
  val country: String? = null,
  val distanceKm: Float? = null,
  val isVerified: Boolean = false,
  val isFeatured: Boolean = false,
  val isHot: Boolean = false,
  val status: String = "active",      // "active" | "pending" | "disabled"
  val url: String? = null,
  val createdAt: String
)

// 2. Reviews Logic Data Model
data class ServiceReview(
  val id: String,
  val serviceId: String,
  val username: String,
  val userAvatar: String? = null,
  val rating: Int,
  val comment: String,
  val date: String,
  val isVerifiedBadge: Boolean = true,
  val replyContent: String? = null
)

// 3. Messenger Chat & Message Models
data class Message(
  val id: String,
  val senderId: String,
  val senderName: String,
  val text: String,
  val timestamp: String,
  val isSelf: Boolean,
  val attachmentServiceId: String? = null // For sharing Partner Service cards directly in chats
)

data class Chat(
  val id: String,
  val name: String,
  val lastMessage: String,
  val lastMessageTime: String,
  val avatarUrl: String? = null,
  val isOnline: Boolean = false,
  val unreadCount: Int = 0,
  val isPinned: Boolean = false,
  val isGroup: Boolean = false,
  val messages: List<Message> = emptyList()
)

// 4. Wallet States & Logic Models
enum class WalletStatus {
  NOT_CONNECTED,
  CONNECTING,
  CONNECTED,
  ERROR
}

data class WalletTx(
  val id: String,
  val type: String, // "send" | "receive" | "swap" | "stake"
  val amount: Double,
  val unit: String = "TON",
  val address: String,
  val date: String,
  val time: String,
  val status: String = "Completed"
)

data class WalletState(
  val status: WalletStatus = WalletStatus.NOT_CONNECTED,
  val walletType: String? = null, // "Tonkeeper" | "MyTonWallet" | "Telegram" | "WalletConnect"
  val address: String? = null,
  val tonBalance: Double = 0.0,
  val nexBalance: Double = 0.0, // Internal platform token NEX token
  val portfolioValueUsd: Double = 0.0,
  val txHistory: List<WalletTx> = emptyList()
)

// 5. User Profile info
data class UserProfile(
  val username: String,
  val userId: String,
  val city: String,
  val country: String,
  val referralCode: String,
  val invitedCount: Int,
  val rewardsBalance: Double, // NEX points/tokens
  val rankLevel: String, // Bronze, Silver, Gold, Platinum, Obsidian
  val originalInviterId: String? = null
)
