package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import com.example.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h780dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppUiTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
  private fun capture(name: String) {
    val file = File("build/qa/$name.png"); file.parentFile!!.mkdirs()
    compose.runOnIdle {
      val view = compose.activity.window.decorView
      val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
      view.draw(android.graphics.Canvas(bitmap))
      file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
  }
  @Test fun setupRequiresRegistrationDetailsAndPreservesTheEnteredServer() {
    var entered: List<String>? = null
    compose.setContent { MyApplicationTheme { Surface(Modifier.fillMaxSize(), color = MidnightBlue) {
      SetupScreen(UiState(loading = false)) { server, name, code -> entered = listOf(server, name, code) }
    } } }
    compose.onNodeWithText("Создать защищённый профиль").performScrollTo().assertIsNotEnabled()
    compose.onNodeWithText("Адрес сервера").performTextReplacement("https://relay.example")
    compose.onNodeWithText("Ваше имя").performTextInput("Борис")
    compose.onNodeWithText("Код подключения").performTextInput("test-code-only")
    compose.onNodeWithText("Создать защищённый профиль").performScrollTo().assertIsEnabled().performClick()
    assertEquals(listOf("https://relay.example", "Борис", "test-code-only"), entered)
    capture("setup")
  }
  @Test fun emptyContactListOffersTheRealInvitationFlow() {
    var adding = false
    compose.setContent { MyApplicationTheme { Surface(Modifier.fillMaxSize(), color = MidnightBlue) {
      EmptyContacts { adding = true }
    } } }
    compose.onNodeWithText("Здесь начинается общение").assertIsDisplayed()
    compose.onNodeWithText("Добавить собеседника").performClick()
    assertTrue(adding)
    capture("empty-chats")
  }
}
