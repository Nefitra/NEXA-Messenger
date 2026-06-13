package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.types.*

object MockData {

  // Map icon names to ImageVectors safely
  fun getIconFromString(name: String): ImageVector {
    return when (name) {
      "Favorite" -> Icons.Default.Favorite
      "ShoppingCart" -> Icons.Default.ShoppingCart
      "Campaign" -> Icons.Default.Campaign
      "Wallet" -> Icons.Default.Wallet
      "Work" -> Icons.Default.Work
      "Home" -> Icons.Default.Home
      "Restaurant" -> Icons.Default.Restaurant
      "DirectionsCar" -> Icons.Default.DirectionsCar
      "Gamepad" -> Icons.Default.Gamepad
      "Event" -> Icons.Default.Event
      "Laptop" -> Icons.Default.Laptop
      "Sell" -> Icons.Default.Sell
      "Flight" -> Icons.Default.Flight
      "Rocket" -> Icons.Default.RocketLaunch
      "Build" -> Icons.Default.Build
      "Face" -> Icons.Default.Face
      "MedicalServices" -> Icons.Default.MedicalServices
      "School" -> Icons.Default.School
      "Tv" -> Icons.Default.Tv
      "Map" -> Icons.Default.Map
      else -> Icons.Default.Layers
    }
  }

  // At least 20 initial services as requested:
  val initialServices = listOf(
    PartnerService(
      id = "lovematch",
      name = "LoveMatch",
      category = "Dating",
      description = "Find people near you based on interests, hobbies, and mutual location. Clean chat integration and security verified.",
      iconName = "Favorite",
      logo = null,
      rating = 4.7f,
      reviewsCount = 12400,
      city = "Berlin",
      country = "Germany",
      distanceKm = 1.8f,
      isVerified = true,
      isFeatured = true,
      isHot = true,
      status = "active",
      createdAt = "2026-01-10T12:00:00Z"
    ),
    PartnerService(
      id = "markethub",
      name = "MarketHub",
      category = "Marketplace",
      description = "Fast, locally focused peer-to-peer marketplace. Buy, sell or trade electronics, clothing, vehicles, and furniture securely.",
      iconName = "ShoppingCart",
      logo = null,
      rating = 4.8f,
      reviewsCount = 2430,
      city = "Berlin",
      country = "Germany",
      distanceKm = 1.2f,
      isVerified = true,
      isFeatured = true,
      isHot = false,
      status = "active",
      createdAt = "2026-02-15T15:30:00Z"
    ),
    PartnerService(
      id = "adboost",
      name = "AdBoost",
      category = "Advertising Platform",
      description = "Promote your own channels, groups or mini-services to target regions instantly. Pay with system NEX tokens and track performance.",
      iconName = "Campaign",
      logo = null,
      rating = 4.5f,
      reviewsCount = 890,
      city = "London",
      country = "UK",
      distanceKm = 3.5f,
      isVerified = true,
      isFeatured = false,
      isHot = false,
      status = "active",
      createdAt = "2026-03-01T09:12:00Z"
    ),
    PartnerService(
      id = "tonwallet_pro",
      name = "TonWallet Pro",
      category = "Crypto Wallet",
      description = "Advanced, highly intuitive Web3 secure custody wallet. Supports direct staking, cross-chain swaps and NFT showcases with low gas.",
      iconName = "Wallet",
      logo = null,
      rating = 4.9f,
      reviewsCount = 45010,
      city = "Zürich",
      country = "Switzerland",
      distanceKm = 0.5f,
      isVerified = true,
      isFeatured = true,
      isHot = true,
      status = "active",
      createdAt = "2025-11-20T11:45:00Z"
    ),
    PartnerService(
      id = "cityjobs",
      name = "CityJobs",
      category = "Jobs",
      description = "Direct matching portal for local physical, hybrid, and remote occupations. Apply with one unified professional resume profile.",
      iconName = "Work",
      logo = null,
      rating = 4.4f,
      reviewsCount = 1350,
      city = "Berlin",
      country = "Germany",
      distanceKm = 2.4f,
      isVerified = false,
      isFeatured = false,
      isHot = false,
      status = "active",
      createdAt = "2026-01-20T14:40:00Z"
    ),
    PartnerService(
      id = "realestate_now",
      name = "RealEstate Now",
      category = "Real Estate",
      description = "Verified rental listings, high contrast interior view overlays, short leases, and smart contract deposits directly in-app.",
      iconName = "Home",
      logo = null,
      rating = 4.6f,
      reviewsCount = 3710,
      city = "Paris",
      country = "France",
      distanceKm = 4.1f,
      isVerified = true,
      isFeatured = false,
      isHot = false,
      status = "active",
      createdAt = "2026-02-18T10:00:00Z"
    ),
    PartnerService(
      id = "foodnear",
      name = "FoodNear",
      category = "Restaurants",
      description = "Reserve fine tables or order lightning fast ecosystem-coupled delivery from local premium kitchens. Earn solid 5% NEX cashback on orders.",
      iconName = "Restaurant",
      logo = null,
      rating = 4.7f,
      reviewsCount = 8990,
      city = "Berlin",
      country = "Germany",
      distanceKm = 0.8f,
      isVerified = true,
      isFeatured = true,
      isHot = true,
      status = "active",
      createdAt = "2026-04-05T18:20:00Z"
    ),
    PartnerService(
      id = "taxiline",
      name = "TaxiLine",
      category = "Transport",
      description = "Order professional private taxis, clean electric micro-mobility scooters, or shared rides on demand. Pay with card or direct crypto TON standard.",
      iconName = "DirectionsCar",
      logo = null,
      rating = 4.8f,
      reviewsCount = 14320,
      city = "Berlin",
      country = "Germany",
      distanceKm = 1.1f,
      isVerified = true,
      isFeatured = false,
      isHot = true,
      status = "active",
      createdAt = "2026-03-12T08:00:00Z"
    ),
    PartnerService(
      id = "gamezone",
      name = "GameZone",
      category = "Games",
      description = "Instant play mini-games center. Battle globally in PvP, climb decentralized seasonal leaderboards, and win platform tickets and tokens.",
      iconName = "Gamepad",
      logo = null,
      rating = 4.3f,
      reviewsCount = 6120,
      city = "Tokyo",
      country = "Japan",
      distanceKm = 6.2f,
      isVerified = true,
      isFeatured = true,
      isHot = false,
      status = "active",
      createdAt = "2026-02-28T21:00:00Z"
    ),
    PartnerService(
      id = "eventradar",
      name = "EventRadar",
      category = "Events",
      description = "Your active guide to music festivals, technological workshops, artistic galleries, and spontaneous open-air events in current regions.",
      iconName = "Event",
      logo = null,
      rating = 4.5f,
      reviewsCount = 2100,
      city = "Berlin",
      country = "Germany",
      distanceKm = 3.2f,
      isVerified = false,
      isFeatured = false,
      isHot = false,
      status = "active",
      createdAt = "2026-03-24T13:15:00Z"
    ),
    PartnerService(
      id = "freelancedesk",
      name = "FreelanceDesk",
      category = "Freelance",
      description = "Decentralized job matching for developers, UI artists, editors, copywriters and marketers. Safe escrow contract protection on tasks.",
      iconName = "Laptop",
      logo = null,
      rating = 4.6f,
      reviewsCount = 1750,
      city = "Berlin",
      country = "Germany",
      distanceKm = 2.1f,
      isVerified = true,
      isFeatured = false,
      isHot = false,
      status = "active",
      createdAt = "2026-01-15T09:45:00Z"
    ),
    PartnerService(
      id = "localdeals",
      name = "Discounts",
      description = "Unlock premium physical vouchers and coupons. Claim up to 50% discount codes at partnering coffee spots, fitness lounges, and stores near current location.",
      iconName = "Sell",
      category = "Local Deals",
      rating = 4.7f,
      reviewsCount = 3804,
      city = "Berlin",
      country = "Germany",
      distanceKm = 0.9f,
      isVerified = true,
      isFeatured = true,
      isHot = true,
      status = "active",
      createdAt = "2026-05-10T16:00:00Z"
    ),
    PartnerService(
      id = "travelgo",
      name = "TravelGo",
      category = "Travel",
      description = "Compare budget airline connections, cozy holiday apartments, global hotels, and comprehensive car rental deals. Full roundtrip ticket manager.",
      iconName = "Flight",
      logo = null,
      rating = 4.4f,
      reviewsCount = 940,
      city = "Rome",
      country = "Italy",
      distanceKm = 5.6f,
      isVerified = false,
      isFeatured = false,
      isHot = false,
      status = "active",
      createdAt = "2026-02-12T10:30:00Z"
    ),
    PartnerService(
      id = "cryptolaunch",
      name = "Token Launchpad",
      category = "Crypto",
      description = "Get early allocation into audited TON ecosystems and secure crypto projects. Verify liquidity status and earn dynamic rewards.",
      iconName = "Rocket",
      logo = null,
      rating = 4.8f,
      reviewsCount = 1420,
      city = "Berlin",
      country = "Germany",
      distanceKm = 1.5f,
      isVerified = true,
      isFeatured = true,
      isHot = true,
      status = "active",
      createdAt = "2026-05-01T15:00:00Z"
    ),
    PartnerService(
      id = "repairmaster",
      name = "Local Services",
      description = "Professional electrical troubleshooting, verified residential plumbing, wooden furniture assembly, and custom repairs on demand.",
      iconName = "Build",
      category = "Local Services",
      rating = 4.5f,
      reviewsCount = 890,
      city = "Munich",
      country = "Germany",
      distanceKm = 8.4f,
      isVerified = false,
      isFeatured = false,
      isHot = false,
      status = "active",
      createdAt = "2026-03-10T11:00:00Z"
    ),
    PartnerService(
      id = "beautybook",
      name = "BeautyBook",
      category = "Beauty",
      description = "Instantly book dynamic schedules at verified salons, barber studios, tattoo experts, and relaxation massage centers with reviews.",
      iconName = "Face",
      logo = null,
      rating = 4.6f,
      reviewsCount = 2050,
      city = "Berlin",
      country = "Germany",
      distanceKm = 1.7f,
      isVerified = true,
      isFeatured = false,
      isHot = false,
      status = "active",
      createdAt = "2026-02-05T13:00:00Z"
    ),
    PartnerService(
      id = "doctornear",
      name = "DoctorNear",
      category = "Health",
      description = "Check nearby licensed general practitioners, child clinics, professional dentists, and pharmacies with 24/7 priority emergency services.",
      iconName = "MedicalServices",
      logo = null,
      rating = 4.3f,
      reviewsCount = 590,
      city = "Hamburg",
      country = "Germany",
      distanceKm = 12.0f,
      isVerified = true,
      isFeatured = false,
      isHot = false,
      status = "active",
      createdAt = "2026-01-30T08:30:00Z"
    ),
    PartnerService(
      id = "eduspace",
      name = "EduSpace",
      category = "Education",
      description = "Learn modern software engineering, visual graphic design, financial literacy, or linguistics from native instructors with certificates.",
      iconName = "School",
      logo = null,
      rating = 4.7f,
      reviewsCount = 1860,
      city = "Berlin",
      country = "Germany",
      distanceKm = 2.9f,
      isVerified = true,
      isFeatured = false,
      isHot = false,
      status = "active",
      createdAt = "2026-04-18T14:00:00Z"
    ),
    PartnerService(
      id = "streamclub",
      name = "StreamClub",
      category = "Entertainment",
      description = "Watch independent creative series, premium gaming competitions, live musical shows, or connect with active micro-podcasters directly.",
      iconName = "Tv",
      logo = null,
      rating = 4.5f,
      reviewsCount = 3780,
      city = "Los Angeles",
      country = "USA",
      distanceKm = 9.5f,
      isVerified = true,
      isFeatured = false,
      isHot = false,
      status = "active",
      createdAt = "2026-03-15T19:00:00Z"
    ),
    PartnerService(
      id = "businessmap",
      name = "BusinessMap",
      category = "Business Directory",
      description = "Consistently updated physical listings of administrative headquarters, coworking hubs, registration agents, and legal support.",
      iconName = "Map",
      logo = null,
      rating = 4.6f,
      reviewsCount = 480,
      city = "Berlin",
      country = "Germany",
      distanceKm = 2.5f,
      isVerified = true,
      isFeatured = false,
      isHot = false,
      status = "active",
      createdAt = "2026-01-25T11:20:00Z"
    )
  )

  // Mock Reviews matching LoveMatch & MarketHub
  val mockReviews = listOf(
    ServiceReview(
      id = "rev_1",
      serviceId = "lovematch",
      username = "cryptokid_de",
      rating = 5,
      comment = "Highly refined dating experience! Found someone 2km away just by filtering. We met at a local cafe and paid via TON. Amazing integration!",
      date = "Yesterday",
      isVerifiedBadge = true,
      replyContent = "We are thrilled! Thanks for sharing and supporting our ecosystem."
    ),
    ServiceReview(
      id = "rev_2",
      serviceId = "lovematch",
      username = "alice_wonder",
      rating = 4,
      comment = "Very clean matching mechanics. Feels premium and secure compared to standard saturated dating apps. No bots detected.",
      date = "3 days ago",
      isVerifiedBadge = true
    ),
    ServiceReview(
      id = "rev_3",
      serviceId = "markethub",
      username = "berlin_seller",
      rating = 5,
      comment = "Sold my old monitor within 2 hours of posting! High target traffic. Standard and simple chat coordinate setup.",
      date = "1 week ago",
      isVerifiedBadge = true,
      replyContent = "Awesome! Quick liquid turnover is exactly our primary goal."
    ),
    ServiceReview(
      id = "rev_4",
      serviceId = "markethub",
      username = "bobby_ton",
      rating = 3,
      comment = "The user layout is great, but filtering by specific minor sub-categories could be slightly better formatted. Solid release anyway.",
      date = "2 weeks ago",
      isVerifiedBadge = false
    )
  )

  // Mock Chats with realistic chats, pins, unread, etc.
  val mockChats = listOf(
    Chat(
      id = "chat_lovematch",
      name = "💕 LoveMatch Concierge",
      lastMessage = "You have 3 new premium matches around Berlin Mitte!",
      lastMessageTime = "13:42",
      isOnline = true,
      unreadCount = 2,
      isPinned = true,
      isGroup = false,
      messages = listOf(
        Message("m1", "lovematch", "LoveMatch Concierge", "Welcome to LoveMatch! Your profile is 100% complete and verified.", "10:30", false),
        Message("m2", "lovematch", "LoveMatch Concierge", "You have 3 new premium matches around Berlin Mitte!", "13:42", false)
      )
    ),
    Chat(
      id = "chat_pavel",
      name = "Pavel Durov",
      lastMessage = "Let me know your thoughts on our TON Wallet Connect SDK update. Looks extremely fast.",
      lastMessageTime = "12:15",
      isOnline = true,
      unreadCount = 0,
      isPinned = true,
      isGroup = false,
      messages = listOf(
        Message("m3", "user", "Me", "Hey Pavel! We are launching NEXA today, combining chats and physical partners, using TON wallet connections.", "12:05", true),
        Message("m4", "pavel", "Pavel Durov", "Sounds fantastic. Keep it simple and fluid. Telegram style with local discovery is highly effective.", "12:10", false),
        Message("m5", "pavel", "Pavel Durov", "Let me know your thoughts on our TON Wallet Connect SDK update. Looks extremely fast.", "12:15", false)
      )
    ),
    Chat(
      id = "chat_nexa_announcements",
      name = "📢 NEXA News & Rewards",
      lastMessage = "Staking TON is now live with 8.5% APY! 🚀 Click below to verify or share card.",
      lastMessageTime = "Yesterday",
      isOnline = false,
      unreadCount = 0,
      isPinned = true,
      isGroup = true,
      messages = listOf(
        Message("m6", "announcer", "NEXA News", "Welcome to NEXA - The Decentralized Service Messenger!", "May 20", false),
        Message("m7", "announcer", "NEXA News", "We have partnered with 20 key local services across Europe and Germany.", "June 01", false),
        Message("m8", "announcer", "NEXA News", "Staking TON is now live with 8.5% APY! 🚀 Click below to verify or share card.", "Yesterday", false)
      )
    ),
    Chat(
      id = "chat_cryptokid",
      name = "Alex (TON Trader)",
      lastMessage = "Just sent you 2.5 TON. Confirm receipt inside the wallet section!",
      lastMessageTime = "Yesterday",
      isOnline = true,
      unreadCount = 0,
      isPinned = false,
      isGroup = false,
      messages = listOf(
        Message("m9", "user", "Me", "Alex, do you have some TON for gas? I need to test swap logic.", "15:40", true),
        Message("m10", "cryptokid", "Alex", "Sure thing, sending 2.5 TON right now to your NEXA account! Let me know when it arrives.", "15:42", false),
        Message("m11", "cryptokid", "Alex", "Just sent you 2.5 TON. Confirm receipt inside the wallet section!", "15:45", false)
      )
    ),
    Chat(
      id = "chat_tonkeeper",
      name = "💎 Tonkeeper Support Forum",
      lastMessage = "Tonkeeper connected accounts can now directly stream transactions into custom dApps.",
      lastMessageTime = "June 10",
      isOnline = false,
      unreadCount = 0,
      isPinned = false,
      isGroup = true,
      messages = listOf(
        Message("ms1", "tk", "Tonkeeper Bot", "Connecting to NEXA completed successfully. Address shortened.", "June 10", false),
        Message("ms2", "tk", "Tonkeeper Bot", "Tonkeeper connected accounts can now directly stream transactions into custom dApps.", "June 10", false)
      )
    )
  )

  // Initial user settings / profile
  val defaultUser = UserProfile(
    username = "cosmic_nexian",
    userId = "8618331744", // Belongs to admin user as defined by user requested ADMIN_USER_IDS = ["8618331744"]
    city = "Berlin",
    country = "Germany",
    referralCode = "NEXA-8618-99X",
    invitedCount = 14,
    rewardsBalance = 425.0, // NEX Points
    rankLevel = "Platinum"
  )
}
