package com.example

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonParser
import com.tonino.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.webrtc.RTCStatsCollectorCallback
import org.webrtc.RTCStatsReport
import java.util.Base64
import java.security.SecureRandom
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Native libsignal + native WebRTC on Android, with a real external TURN server.
 * No user account or conversation is accessed: both Signal identities exist only in this test. */
@RunWith(AndroidJUnit4::class)
class VoiceCallIntegrationTest {
  @Test fun encryptedCallConnectsAndExchangesAudioThroughTurn() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("WebRTC requires ACCESS_NETWORK_STATE", PackageManager.PERMISSION_GRANTED,
      context.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE))
    assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(Manifest.permission.RECORD_AUDIO))
    val probe = context.filesDir.resolve("voice-probe.json")
    require(probe.isFile) { "Private short-lived TURN probe settings must be supplied explicitly" }
    val config = JsonParser.parseString(probe.readText()).asJsonObject
    fun token() = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
    val a = SecureEngine.create("https://voice-test.invalid", "Проверка А") {}
    val b = SecureEngine.create("https://voice-test.invalid", "Проверка Б") {}
    a.addContact(SecureEngine.parseInvitation(b.invitation(token())))
    b.addContact(SecureEngine.parseInvitation(a.invitation(token())))
    a.establish(b.id, b.bundle())
    val actorA = Executors.newSingleThreadExecutor()
    val actorB = Executors.newSingleThreadExecutor()
    val error = AtomicReference<String?>(null)
    val active = CountDownLatch(2)
    val activeA = AtomicBoolean(false)
    val activeB = AtomicBoolean(false)
    val accepted = AtomicBoolean(false)
    val phaseA = AtomicReference("idle")
    val phaseB = AtomicReference("idle")
    lateinit var voiceA: VoiceSession
    lateinit var voiceB: VoiceSession
    var createdA=false
    var createdB=false
    ActivityScenario.launch(MainActivity::class.java).use {
      try {
        actorA.submit {
          voiceA = VoiceSession(context, { work -> actorA.execute(work) }, { peer,kind,data ->
            val p = a.signal(peer,kind,data)
            actorB.execute { b.receive(p.copy(from=a.id))?.let(voiceB::receive) }
            a.accepted(p.id)
          }, { state ->
            phaseA.set(state.phase)
            if (state.phase == "active" && activeA.compareAndSet(false,true)) active.countDown()
          }, { error.compareAndSet(null,it) })
          createdA=true
        }.get(15,TimeUnit.SECONDS)
        actorB.submit {
          voiceB = VoiceSession(context, { work -> actorB.execute(work) }, { peer,kind,data ->
            val p = b.signal(peer,kind,data)
            actorA.execute { a.receive(p.copy(from=b.id))?.let(voiceA::receive) }
            b.accepted(p.id)
          }, { state ->
            phaseB.set(state.phase)
            if (state.phase == "incoming" && accepted.compareAndSet(false,true)) {
              actorB.execute { voiceB.accept(config.obj("b")) }
            }
            if (state.phase == "active" && activeB.compareAndSet(false,true)) active.countDown()
          }, { error.compareAndSet(null,it) })
          createdB=true
        }.get(15,TimeUnit.SECONDS)
        actorA.submit { voiceA.start(b.id,"Проверка Б",config.obj("a")) }.get(20,TimeUnit.SECONDS)
        assertTrue("Call did not connect: A=${phaseA.get()}, B=${phaseB.get()}, error=${error.get()}", active.await(75,TimeUnit.SECONDS))
        assertNull(error.get())
        fun report(voice: VoiceSession, actor: ExecutorService): RTCStatsReport {
          val value = CompletableFuture<RTCStatsReport>()
          actor.execute { voice.collectStats(RTCStatsCollectorCallback { value.complete(it) }) }
          return value.get(10,TimeUnit.SECONDS)
        }
        fun inbound(r: RTCStatsReport): Long = r.statsMap.values.filter {
          it.type == "inbound-rtp" && (it.members["kind"] == "audio" || it.members["mediaType"] == "audio")
        }.sumOf { (it.members["packetsReceived"] as? Number)?.toLong() ?: 0 }
        var packetsA=0L; var packetsB=0L
        repeat(12) {
          if (packetsA == 0L || packetsB == 0L) {
            Thread.sleep(1000)
            packetsA=inbound(report(voiceA,actorA));packetsB=inbound(report(voiceB,actorB))
          }
        }
        assertTrue("No audio RTP reached peer A",packetsA>0)
        assertTrue("No audio RTP reached peer B",packetsB>0)
        actorA.submit { voiceA.toggleMute(); voiceA.toggleMute(); voiceA.end(true) }.get(10,TimeUnit.SECONDS)
        val ended=System.nanoTime()+TimeUnit.SECONDS.toNanos(5)
        while(phaseB.get()!="idle" && System.nanoTime()<ended) Thread.sleep(50)
        assertEquals("idle",phaseB.get())
        println("VOICE_CALL_ACTIVE_BOTH_AND_AUDIO_RTP_OK: receivedA=$packetsA receivedB=$packetsB; encrypted hangup propagated")
      } finally {
        runCatching { actorA.submit { if (createdA) voiceA.close() }.get(10,TimeUnit.SECONDS) }
        runCatching { actorB.submit { if (createdB) voiceB.close() }.get(10,TimeUnit.SECONDS) }
        actorA.shutdownNow();actorB.shutdownNow();probe.delete()
      }
    }
  }
}
