package com.tonino.core

import com.google.gson.*
import org.junit.Assert.*
import org.junit.Test
import java.net.URI
import java.net.http.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/** Real libsignal JNI clients and the actual Node/SQLite relay, with no crypto or transport stubs. */
class RelayIntegrationTest {
  @Test fun encryptedConversationCrossesRelayAndSurvivesRelayRestart() {
    val folder = Files.createTempDirectory("tonino-integration-")
    val database = folder.resolve("relay.sqlite")
    val source = Path.of("../server/src/server.mjs").toAbsolutePath().normalize().toUri().toString()
    val joinCode = "integration-test-registration-code-1234567890"
    fun launch(): Pair<Process, String> {
      val script = "import {createRelay} from '$source'; const r=createRelay({database:process.env.TEST_DATABASE,joinCode:process.env.TEST_JOIN,pollMs:30}); r.server.listen(0,'127.0.0.1',()=>console.log(r.server.address().port)); process.on('SIGTERM',()=>r.close().then(()=>process.exit(0)));"
      val builder = ProcessBuilder("node", "--input-type=module", "-e", script)
      builder.environment()["TEST_DATABASE"] = database.toString(); builder.environment()["TEST_JOIN"] = joinCode
      val process = builder.start()
      val port = process.inputStream.bufferedReader().readLine().toInt()
      return process to "http://127.0.0.1:$port"
    }
    var (process, base) = launch()
    val client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build()
    fun request(e: SecureEngine, path: String, method: String = "GET", body: JsonObject? = null): JsonObject {
      val req = HttpRequest.newBuilder(URI(base + path)).header("Content-Type", "application/json")
        .header("Authorization", "Bearer ${e.id}:${e.token}").header("X-Join-Code", joinCode)
      req.method(method, if (body == null) HttpRequest.BodyPublishers.noBody() else HttpRequest.BodyPublishers.ofString(body.toString()))
      val r = client.send(req.build(), HttpResponse.BodyHandlers.ofString())
      assertTrue("Request $path failed: ${r.statusCode()}", r.statusCode() in 200..299)
      return JsonParser.parseString(r.body()).asJsonObject
    }
    try {
      val a = SecureEngine.create("https://test.example", "Алиса") {}
      val b = SecureEngine.create("https://test.example", "Борис") {}
      for (e in listOf(a, b)) request(e, "/v1/register", "POST", json("id" to e.id, "token" to e.token, "bundle" to e.bundle()))
      // Simulate a lost registration response: retrying with the same saved key/token is safe.
      request(a, "/v1/register", "POST", json("id" to a.id, "token" to a.token, "bundle" to a.bundle()))
      for ((owner, other) in listOf(a to b, b to a)) {
        val i = request(owner, "/v1/invites", "POST", JsonObject())
        val invite = SecureEngine.parseInvitation(owner.invitation(i.text("token")))
        val accepted = request(other, "/v1/invites/redeem", "POST", json("token" to invite.token, "owner" to invite.id))
        assertEquals(invite.identity, accepted.text("identity")); other.addContact(invite)
      }
      a.establish(b.id, request(a, "/v1/peers/${b.id}/bundle"))
      val p = a.sendText(b.id, "Секретный текст двух реальных клиентов")
      request(a, "/v1/messages", "POST", p.outgoing()); a.accepted(p.id)
      process.destroy(); assertTrue(process.waitFor(5, TimeUnit.SECONDS))
      val restarted = launch(); process = restarted.first; base = restarted.second
      val packets = request(b, "/v1/inbox").array("packets")
      assertEquals(1, packets.size())
      assertFalse(packets.toString().contains("Секретный"))
      val received = Gson().fromJson(packets[0], Packet::class.java)
      assertEquals("Секретный текст двух реальных клиентов", b.receive(received)!!.data.text("text"))
      request(b, "/v1/inbox/ack", "POST", json("packets" to listOf(json("from" to a.id, "id" to p.id))))
      for (receipt in b.pending()) request(b, "/v1/messages", "POST", receipt.outgoing())
      for (receipt in request(a, "/v1/inbox").array("packets")) a.receive(Gson().fromJson(receipt, Packet::class.java))
      assertEquals("delivered", a.messages().single().status)
      val response = b.sendText(a.id, "Ответ после перезапуска")
      request(b, "/v1/messages", "POST", response.outgoing())
      val incoming = request(a, "/v1/inbox").array("packets").map { Gson().fromJson(it, Packet::class.java) }
      incoming.forEach { a.receive(it) }
      assertEquals("Ответ после перезапуска", a.messages().last().text)
    } finally {
      process.destroy(); process.waitFor(5, TimeUnit.SECONDS)
      folder.toFile().deleteRecursively()
    }
  }
}
