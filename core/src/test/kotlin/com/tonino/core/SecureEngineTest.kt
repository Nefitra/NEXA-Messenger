package com.tonino.core

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import java.util.UUID

class SecureEngineTest {
  private fun pair(): Pair<SecureEngine, SecureEngine> {
    val a = SecureEngine.create("https://relay.example", "Алиса") {}
    val b = SecureEngine.create("https://relay.example", "Борис") {}
    a.addContact(invite(b)); b.addContact(invite(a)); a.establish(b.id, b.bundle())
    return a to b
  }
  private fun invite(e: SecureEngine): Invitation = SecureEngine.parseInvitation(e.invitation("x".repeat(43)))
  private fun delivered(p: Packet, sender: SecureEngine): Packet = p.copy(from = sender.id)
  private fun rejected(block: () -> Unit) { try { block(); fail("Expected rejection") } catch (e: Exception) { /* expected */ } }

  @Test fun twoRealSignalClientsExchangeTextAndAuthenticatedReceipts() {
    val (a, b) = pair()
    val p = a.sendText(b.id, "Привет, Борис! 🔐")
    assertFalse(p.body.contains("Привет"))
    assertEquals("Привет, Борис! 🔐", b.receive(delivered(p, a))!!.data.text("text"))
    a.accepted(p.id)
    assertEquals("sent", a.messages().single().status)
    a.receive(delivered(b.pending().single(), b))
    assertEquals("delivered", a.messages().single().status)
    assertEquals(a.safetyCode(b.id), b.safetyCode(a.id))
  }
  @Test fun modifiedCiphertextIsRejectedWithoutConsumingTheValidMessage() {
    val (a, b) = pair()
    val p = a.sendText(b.id, "Подлинное сообщение")
    val bytes = Base64.getDecoder().decode(p.body)
    bytes[bytes.lastIndex - 8] = (bytes[bytes.lastIndex - 8].toInt() xor 1).toByte()
    rejected { b.receive(delivered(p.copy(body = Base64.getEncoder().encodeToString(bytes)), a)) }
    assertTrue(b.messages().isEmpty())
    assertNotNull(b.receive(delivered(p, a)))
  }
  @Test fun externalMessageIdIsBoundToTheEncryptedHeader() {
    val (a, b) = pair()
    val p = a.sendText(b.id, "Заголовок защищён")
    rejected { b.receive(delivered(p.copy(id = UUID.randomUUID().toString()), a)) }
    assertNotNull(b.receive(delivered(p, a)))
    assertNull(b.receive(delivered(p, a)))
    rejected { b.receive(delivered(p.copy(id = UUID.randomUUID().toString()), a)) }
    assertEquals(1, b.messages().size)
  }
  @Test fun serverCannotSubstituteAnIdentityOrSilentlyReplaceAPin() {
    val (a, b) = pair()
    val attacker = SecureEngine.create("https://relay.example", "Подмена") {}
    rejected { a.establish(b.id, attacker.bundle()) }
    rejected { a.addContact(invite(b).copy(identity = attacker.identity)) }
    val p = a.sendText(b.id, "Старый ключ остался")
    assertNotNull(b.receive(delivered(p, a)))
  }
  @Test fun savedRatchetsSurviveRestartAndOutOfOrderDelivery() {
    var (a, b) = pair()
    val first = a.sendText(b.id, "Начало")
    b.receive(delivered(first, a)); a.receive(delivered(b.pending().single(), b))
    a = SecureEngine.restore(a.snapshot()) {}; b = SecureEngine.restore(b.snapshot()) {}
    val packets = (0..2).map { a.sendText(b.id, "Сообщение $it") }
    for (i in listOf(2, 0, 1)) assertEquals("Сообщение $i", b.receive(delivered(packets[i], a))!!.data.text("text"))
    assertEquals(4, b.messages().size)
  }
  @Test fun failedDiskWriteDoesNotReleaseCiphertextOrAdvanceRatchet() {
    val (original, b) = pair()
    var failWrite = true
    val a = SecureEngine.restore(original.snapshot()) { if (failWrite) throw java.io.IOException("disk full") }
    rejected { a.sendText(b.id, "Не сохранено") }
    assertTrue(a.pending().isEmpty()); assertTrue(a.messages().isEmpty())
    failWrite = false
    assertNotNull(b.receive(delivered(a.sendText(b.id, "Сохранено"), a)))
  }
  @Test fun failedReceiveCommitCanBeRetriedAfterRestart() {
    val (a, original) = pair()
    val p = a.sendText(original.id, "Нельзя потерять")
    var failWrite = true
    val b = SecureEngine.restore(original.snapshot()) { if (failWrite) throw java.io.IOException("disk full") }
    rejected { b.receive(delivered(p, a)) }
    assertTrue(b.messages().isEmpty())
    failWrite = false
    assertNotNull(b.receive(delivered(p, a)))
    assertEquals(1, b.messages().size)
  }
  @Test fun callFingerprintsAndCandidatesUseTheSameAuthenticatedChannel() {
    val (a, b) = pair()
    val p = a.signal(b.id, "call_offer", json("callId" to UUID.randomUUID().toString(), "sdp" to "a=fingerprint:sha-256 TEST"))
    val frame = b.receive(delivered(p, a))!!
    assertEquals("call_offer", frame.kind)
    assertEquals("a=fingerprint:sha-256 TEST", frame.data.text("sdp"))
    assertTrue(b.messages().isEmpty())
  }
  @Test fun publicCleartextUrlsAndCredentialUrlsAreRejected() {
    rejected { canonicalServer("http://relay.example", true) }
    rejected { canonicalServer("https://user:password@relay.example") }
    rejected { canonicalServer("https://relay.example?token=leak") }
    assertEquals("http://192.168.1.3:8080", canonicalServer("http://192.168.1.3:8080/", true))
  }
  @Test fun simultaneousFirstMessagesRemainDecryptable() {
    val (a, b) = pair()
    b.establish(a.id, a.bundle())
    val ap = a.sendText(b.id, "Одновременно от Алисы")
    val bp = b.sendText(a.id, "Одновременно от Бориса")
    assertNotNull(b.receive(delivered(ap, a)))
    assertNotNull(a.receive(delivered(bp, b)))
    assertNotNull(b.receive(delivered(a.sendText(b.id, "Следующее"), a)))
    assertNotNull(a.receive(delivered(b.sendText(a.id, "Ответ"), b)))
  }
  @Test fun spentOneTimeKeysCannotRecreateAnInitialSessionFromLaterState() {
    val (a, b) = pair()
    val p = a.sendText(b.id, "Первая сессия")
    b.receive(delivered(p, a))
    val capturedLater = com.google.gson.JsonParser.parseString(b.snapshot()).asJsonObject
    capturedLater.add("sessions", com.google.gson.JsonObject())
    capturedLater.add("seen", com.google.gson.JsonArray())
    val later = SecureEngine.restore(capturedLater.toString()) {}
    rejected { later.receive(delivered(p, a)) }
  }
}
