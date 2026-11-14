package com.example

import android.Manifest
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.*
import com.tonino.core.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.webrtc.RTCStatsCollectorCallback
import org.webrtc.RTCStatsReport
import java.util.concurrent.*
import java.util.concurrent.atomic.*

/** Production HTTP/SQLite signaling + libsignal + native Android WebRTC + external TURN.
 * The HTTP fixture and all identities are disposable; no production chat account is used. */
@RunWith(AndroidJUnit4::class)
class VoiceCallIntegrationTest {
  private val base="http://10.0.2.2:18080"
  private val join="tonino-native-test-only-fixture"
  private val http=OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).readTimeout(5,TimeUnit.SECONDS).build()
  private fun request(e:SecureEngine,path:String,method:String="GET",body:JsonObject?=null):JsonObject {
    val r=Request.Builder().url(base+path).header("Authorization","Bearer ${e.id}:${e.token}")
      .header("X-Join-Code",join).method(method,if(method=="GET")null else (body?:JsonObject()).toString().toRequestBody("application/json".toMediaType())).build()
    return http.newCall(r).execute().use { response ->
      check(response.isSuccessful){"Fixture HTTP ${response.code}"}
      JsonParser.parseString(checkNotNull(response.body).string()).asJsonObject
    }
  }
  @Test fun encryptedHttpCallsSurviveBackgroundAndRapidRedialTcpAndUdp() {
    val instrumentation=InstrumentationRegistry.getInstrumentation()
    val context=instrumentation.targetContext
    assertEquals(PackageManager.PERMISSION_GRANTED,context.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE))
    assertEquals(PackageManager.PERMISSION_GRANTED,context.checkSelfPermission(Manifest.permission.RECORD_AUDIO))
    val probe=context.filesDir.resolve("voice-probe.json")
    val config=JsonParser.parseString(probe.readText()).asJsonObject
    ActivityScenario.launch(MainActivity::class.java).use { screen ->
      try {
        for(protocol in listOf("tcp","udp")) {
          fun turn(name:String):JsonObject = config.obj(name).deepCopy().apply {
            val filtered=array("urls").filter{it.asString.endsWith("transport=$protocol")}
            check(filtered.isNotEmpty());add("urls",Gson().toJsonTree(filtered))
          }
          val a=SecureEngine.create(base,"Проверка А"){}
          val b=SecureEngine.create(base,"Проверка Б"){}
          for(e in listOf(a,b)) request(e,"/v1/register","POST",json("id" to e.id,"token" to e.token,"bundle" to e.bundle()))
          fun invite(owner:SecureEngine,other:SecureEngine) {
            val token=request(owner,"/v1/invites","POST").text("token")
            request(other,"/v1/invites/redeem","POST",json("token" to token,"owner" to owner.id))
            other.addContact(SecureEngine.parseInvitation(owner.invitation(token),true))
          }
          invite(a,b);invite(b,a)
          a.establish(b.id,request(a,"/v1/peers/${b.id}/bundle"))
          val actorA=Executors.newSingleThreadExecutor();val actorB=Executors.newSingleThreadExecutor()
          val error=AtomicReference<Throwable?>(null);val running=AtomicBoolean(true)
          val phaseA=AtomicReference("idle");val phaseB=AtomicReference("idle")
          val accepts=AtomicInteger(0)
          lateinit var voiceA:VoiceSession;lateinit var voiceB:VoiceSession
          var createdA=false;var createdB=false
          fun deliver(e:SecureEngine,other:String,kind:String,data:JsonObject) {
            val p=e.signal(other,kind,data)
            assertEquals(p.id,request(e,"/v1/messages","POST",p.outgoing()).text("accepted"));e.accepted(p.id)
          }
          fun poll(e:SecureEngine,actor:ExecutorService,voice:()->VoiceSession)=Thread({
            try {
              while(running.get()) {
                val packets=request(e,"/v1/inbox").array("packets").map{Gson().fromJson(it,Packet::class.java)}
                if(packets.isNotEmpty()) {
                  actor.submit {packets.forEach{p->e.receive(p)?.let{voice().receive(it)}}}.get(15,TimeUnit.SECONDS)
                  request(e,"/v1/inbox/ack","POST",json("packets" to packets.map{json("from" to it.from,"id" to it.id)}))
                }
              }
            } catch(t:Throwable) {if(running.get())error.compareAndSet(null,t)}
          },"voice-http-poll").apply{isDaemon=true;start()}
          var pollA:Thread?=null;var pollB:Thread?=null
          fun waitFor(message:String,seconds:Long,condition:()->Boolean) {
            val end=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds)
            while(!condition() && error.get()==null && System.nanoTime()<end) Thread.sleep(100)
            error.get()?.let{throw AssertionError(message,it)}
            assertTrue("$message A=${phaseA.get()} B=${phaseB.get()}",condition())
          }
          fun report(voice:VoiceSession,actor:ExecutorService):RTCStatsReport {
            val value=CompletableFuture<RTCStatsReport>()
            actor.execute{voice.collectStats(RTCStatsCollectorCallback{value.complete(it)})}
            return value.get(10,TimeUnit.SECONDS)
          }
          fun packets(r:RTCStatsReport):Long=r.statsMap.values.filter{
            it.type=="inbound-rtp" && (it.members["kind"]=="audio"||it.members["mediaType"]=="audio")
          }.sumOf{(it.members["packetsReceived"] as? Number)?.toLong()?:0}
          try {
            actorA.submit{
              voiceA=VoiceSession(context,{actorA.execute(it)},{peer,kind,data->deliver(a,peer,kind,data)},
                {phaseA.set(it.phase)},{error.compareAndSet(null,IllegalStateException(it))});createdA=true
            }.get(10,TimeUnit.SECONDS)
            actorB.submit{
              voiceB=VoiceSession(context,{actorB.execute(it)},{peer,kind,data->deliver(b,peer,kind,data)},
                {s->
                  phaseB.set(s.phase)
                  if(s.phase=="incoming")actorB.execute{
                    try {
                      assertTrue(voiceB.beginAccept());assertFalse("Second answer must not start a second call",voiceB.beginAccept())
                      accepts.incrementAndGet();voiceB.accept(turn("b"));voiceB.accept(turn("b"))
                    }catch(t:Throwable){error.compareAndSet(null,t)}
                  }
                },{error.compareAndSet(null,IllegalStateException(it))});createdB=true
            }.get(10,TimeUnit.SECONDS)
            pollA=poll(a,actorA){voiceA};pollB=poll(b,actorB){voiceB}
            val rounds=if(protocol=="tcp")2 else 1
            repeat(rounds){round->
              screen.moveToState(Lifecycle.State.RESUMED)
              actorA.submit{voiceA.start(b.id,"Проверка Б",turn("a"))}.get(15,TimeUnit.SECONDS)
              waitFor("Native $protocol call did not connect",75){phaseA.get()=="active"&&phaseB.get()=="active"}
              val firstA=packets(report(voiceA,actorA));val firstB=packets(report(voiceB,actorB))
              if(protocol=="tcp"&&round==0) {
                screen.moveToState(Lifecycle.State.CREATED)
                instrumentation.uiAutomation.executeShellCommand("input keyevent 223").close()
                Thread.sleep(70000)
                assertEquals("Call A ended after leaving foreground","active",phaseA.get())
                assertEquals("Call B ended after leaving foreground","active",phaseB.get())
                instrumentation.uiAutomation.executeShellCommand("input keyevent 224").close()
                instrumentation.uiAutomation.executeShellCommand("input keyevent 82").close()
                screen.moveToState(Lifecycle.State.RESUMED)
              } else Thread.sleep(2500)
              val endA=packets(report(voiceA,actorA));val endB=packets(report(voiceB,actorB))
              assertTrue("No new audio reached A over $protocol",endA>firstA)
              assertTrue("No new audio reached B over $protocol",endB>firstB)
              actorA.submit{voiceA.toggleMute();voiceA.toggleMute();voiceA.end(true)}.get(10,TimeUnit.SECONDS)
              waitFor("Encrypted hangup did not arrive",10){phaseB.get()=="idle"}
              assertEquals(round+1,accepts.get())
              println("VOICE_CALL_HTTP_${protocol.uppercase()}_ROUND_${round+1}_RTP_OK: receivedA=$endA receivedB=$endB; encrypted hangup; background70s=${protocol=="tcp"&&round==0}")
            }
          } finally {
            running.set(false);pollA?.join(6000);pollB?.join(6000)
            runCatching{actorA.submit{if(createdA)voiceA.close()}.get(10,TimeUnit.SECONDS)}
            runCatching{actorB.submit{if(createdB)voiceB.close()}.get(10,TimeUnit.SECONDS)}
            actorA.shutdownNow();actorB.shutdownNow()
          }
        }
        println("VOICE_CALL_ACTIVE_BOTH_AND_AUDIO_RTP_OK: HTTP signaling TCP+UDP; background70s; duplicate answer; rapid redial")
      } finally {
        instrumentation.uiAutomation.executeShellCommand("input keyevent 224").close()
        probe.delete();http.dispatcher.cancelAll();http.connectionPool.evictAll()
      }
    }
  }
}
