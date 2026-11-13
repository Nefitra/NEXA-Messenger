package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h780dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppStartupTest {
  @get:Rule val compose = createAndroidComposeRule<MainActivity>()
  @Test fun freshInstallOpensTheRealOnboardingWithoutDemoConversations() {
    compose.waitUntil(10000) { compose.onAllNodesWithText("Ближе друг к другу.").fetchSemanticsNodes().isNotEmpty() }
    compose.onNodeWithText("Ближе друг к другу.").assertIsDisplayed()
    compose.onNodeWithText("Создать защищённый профиль").performScrollTo().assertIsNotEnabled()
  }
}
