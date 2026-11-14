package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Run the first method on installed 0.1.1, install -r 0.1.2, then run the second. */
@androidx.annotation.RequiresApi(28)
@RunWith(AndroidJUnit4::class)
class UpgradePersistenceTest {
  @Test fun seedOldInstall() {
    val context=InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("com.tonino.secure.preview",context.packageName)
    assertEquals(101L,context.packageManager.getPackageInfo(context.packageName,0).longVersionCode)
    assertNull(EncryptedVault(context).read())
    val value="CI-only encrypted history, keys and ratchet persistence sentinel"
    EncryptedVault(context).write(value)
    context.getSharedPreferences("upgrade-proof",0).edit().putString("expected",value).commit()
    println("OLD_PREVIEW_ENCRYPTED_VAULT_SEEDED")
  }
  @Test fun verifyUpdatedInstall() {
    val context=InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals(102L,context.packageManager.getPackageInfo(context.packageName,0).longVersionCode)
    val prefs=context.getSharedPreferences("upgrade-proof",0)
    val expected=checkNotNull(prefs.getString("expected",null))
    assertEquals(expected,EncryptedVault(context).read())
    // Only this disposable emulator's CI fixture is removed. No real user data is accessed.
    assertTrue(context.noBackupFilesDir.resolve("tonino-state.bin").delete())
    prefs.edit().clear().commit()
    println("PREVIEW_101_TO_102_UPGRADE_RETAINED_ENCRYPTED_VAULT_AND_KEYSTORE")
  }
}
