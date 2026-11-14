package com.example

import android.content.Context
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import com.google.gson.JsonObject
import com.tonino.core.*
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class VoiceState(val id: String = "", val peer: String = "", val phase: String = "idle",
  val muted: Boolean = false, val speaker: Boolean = false)

/** Audio only, non-trickle ICE. The complete SDP (including the DTLS fingerprint) is transported
 * as a Signal-encrypted frame. This first build intentionally supports foreground calls only. */
class VoiceSession(private val context: Context, private val dispatch: (() -> Unit) -> Unit,
  private val send: (String, String, JsonObject) -> Unit, private val onState: (VoiceState) -> Unit,
  private val onError: (String) -> Unit) {
  private companion object {
    val initializationLock = Any()
    var initialized = false
  }
  private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val timer = Executors.newSingleThreadScheduledExecutor()
  private var adm: JavaAudioDeviceModule? = null
  private var factory: PeerConnectionFactory? = null
  private var peer: PeerConnection? = null
  private var source: AudioSource? = null
  private var track: AudioTrack? = null
  private var state = VoiceState()
  private var remoteOffer = ""
  private var pendingKind = ""
  private var localSent = false
  private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
      .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).build()
  private fun audioFactory(): PeerConnectionFactory {
    factory?.let { return it }
    synchronized(initializationLock) {
      if (!initialized) {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions())
        initialized = true
      }
    }
    adm = JavaAudioDeviceModule.builder(context).createAudioDeviceModule()
    return PeerConnectionFactory.builder().setAudioDeviceModule(adm).createPeerConnectionFactory().also { factory = it }
  }
  private fun update(s: VoiceState) { state = s; onState(s) }
  fun start(who: String, name: String, turn: JsonObject) {
    require(state.phase == "idle") { "Сначала завершите текущий звонок" }
    update(VoiceState(UUID.randomUUID().toString(), who, "outgoing"))
    try { createPeer(turn); pendingKind = "call_offer"; createDescription(false); timeout(state.id) }
    catch (e: Throwable) { end(false); throw e }
  }
  fun accept(turn: JsonObject) {
    require(state.phase == "incoming")
    update(state.copy(phase = "connecting"))
    try {
      createPeer(turn); pendingKind = "call_answer"
      setRemote(SessionDescription.Type.OFFER, remoteOffer) { createDescription(true) }
      timeout(state.id)
    } catch (e: Throwable) { end(true); throw e }
  }
  fun receive(frame: Frame) {
    if (!frame.kind.startsWith("call_")) return
    if (kotlin.math.abs(System.currentTimeMillis() - frame.time) > 120000) return
    val data = frame.data
    val id = data.text("callId")
    runCatching { UUID.fromString(id) }.getOrElse { return }
    when (frame.kind) {
      "call_offer" -> {
        if (state.phase != "idle") {
          if (state.id != id) runCatching { send(frame.from, "call_end", json("callId" to id, "reason" to "busy")) }
          return
        }
        require(data.text("sdp").length in 50..30000)
        remoteOffer = data.text("sdp")
        update(VoiceState(id, frame.from, "incoming")); timeout(id)
      }
      "call_answer" -> if (state.id == id && state.peer == frame.from && state.phase == "outgoing") {
        require(data.text("sdp").length in 50..30000)
        update(state.copy(phase = "connecting"))
        setRemote(SessionDescription.Type.ANSWER, data.text("sdp")) {}
      }
      "call_end" -> if (state.id == id && state.peer == frame.from) end(false)
    }
  }
  private fun timeout(id: String) {
    timer.schedule({ dispatch {
      if (state.id == id && state.phase != "active" && state.phase != "idle") {
        onError("Собеседник не ответил или соединение не установлено."); end(true)
      }
    } }, 60, TimeUnit.SECONDS)
  }
  private fun createPeer(turn: JsonObject) {
    val factory = audioFactory()
    val urls = turn.array("urls").map { it.asString }
    require(urls.isNotEmpty() && urls.all { it.startsWith("turn:") || it.startsWith("turns:") })
    val ice = PeerConnection.IceServer.builder(urls).setUsername(turn.text("username")).setPassword(turn.text("credential")).createIceServer()
    val config = PeerConnection.RTCConfiguration(listOf(ice)).apply {
      sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
      iceTransportsType = PeerConnection.IceTransportsType.RELAY
      continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
    }
    val callId = state.id
    localSent = false
    peer = factory.createPeerConnection(config, object : PeerConnection.Observer {
      override fun onSignalingChange(s: PeerConnection.SignalingState?) {}
      override fun onIceConnectionChange(s: PeerConnection.IceConnectionState?) { dispatch {
        if (state.id != callId) return@dispatch
        when (s) {
          PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> update(state.copy(phase = "active"))
          PeerConnection.IceConnectionState.FAILED -> { onError("Не удалось установить голосовую связь через ретранслятор."); end(true) }
          PeerConnection.IceConnectionState.DISCONNECTED -> {
            update(state.copy(phase = "connecting"))
            timer.schedule({ dispatch { if (state.id == callId && state.phase == "connecting") end(true) } }, 15, TimeUnit.SECONDS)
          }
          else -> {}
        }
      } }
      override fun onIceConnectionReceivingChange(receiving: Boolean) {}
      override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?) { if (s == PeerConnection.IceGatheringState.COMPLETE) dispatch { if (state.id == callId) sendLocal() } }
      override fun onIceCandidate(candidate: IceCandidate?) {}
      override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
      override fun onAddStream(stream: MediaStream?) {}
      override fun onRemoveStream(stream: MediaStream?) {}
      override fun onDataChannel(channel: DataChannel?) {}
      override fun onRenegotiationNeeded() {}
      override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
    }) ?: error("WebRTC недоступен")
    audio.requestAudioFocus(focus)
    audio.mode = AudioManager.MODE_IN_COMMUNICATION
    @Suppress("DEPRECATION")
    audio.isSpeakerphoneOn = state.speaker
    source = factory.createAudioSource(MediaConstraints())
    track = factory.createAudioTrack("tonino-audio", source).also { it.setEnabled(!state.muted) }
    peer!!.addTrack(track, listOf("tonino"))
  }
  private fun createDescription(answer: Boolean) {
    val callId = state.id
    val observer = object : SdpObserver {
      override fun onCreateSuccess(sdp: SessionDescription?) { dispatch {
        if (state.id != callId || sdp == null) return@dispatch
        peer?.setLocalDescription(object : SdpObserver {
          override fun onSetSuccess() { dispatch { if (state.id == callId && peer?.iceGatheringState() == PeerConnection.IceGatheringState.COMPLETE) sendLocal() } }
          override fun onSetFailure(message: String?) { failure(callId) }
          override fun onCreateSuccess(sdp: SessionDescription?) {}
          override fun onCreateFailure(message: String?) {}
        }, sdp)
      } }
      override fun onCreateFailure(message: String?) { failure(callId) }
      override fun onSetSuccess() {}
      override fun onSetFailure(message: String?) {}
    }
    val constraints = MediaConstraints()
    if (answer) peer?.createAnswer(observer, constraints) else peer?.createOffer(observer, constraints)
  }
  private fun sendLocal() {
    if (localSent || state.phase == "idle") return
    val sdp = peer?.localDescription ?: return
    localSent = true
    try { send(state.peer, pendingKind, json("callId" to state.id, "sdp" to sdp.description)) }
    catch (_: Exception) { onError("Параметры звонка сохранены, но сервер пока недоступен.") }
  }
  private fun setRemote(type: SessionDescription.Type, description: String, success: () -> Unit) {
    val callId = state.id
    peer?.setRemoteDescription(object : SdpObserver {
      override fun onSetSuccess() { dispatch { if (state.id == callId) success() } }
      override fun onSetFailure(message: String?) { failure(callId) }
      override fun onCreateSuccess(sdp: SessionDescription?) {}
      override fun onCreateFailure(message: String?) {}
    }, SessionDescription(type, description))
  }
  private fun failure(callId: String) { dispatch { if (state.id == callId) { onError("Не удалось согласовать защищённый звонок."); end(true) } } }
  fun toggleMute() { update(state.copy(muted = !state.muted)); track?.setEnabled(!state.muted) }
  internal fun collectStats(callback: RTCStatsCollectorCallback) {
    checkNotNull(peer) { "Нет активного соединения" }.getStats(callback)
  }
  @Suppress("DEPRECATION") fun toggleSpeaker() { update(state.copy(speaker = !state.speaker)); audio.isSpeakerphoneOn = state.speaker }
  fun end(notify: Boolean) {
    if (state.phase == "idle") return
    val old = state
    update(VoiceState())
    peer?.close(); peer?.dispose(); peer = null
    track?.dispose(); track = null; source?.dispose(); source = null
    audio.abandonAudioFocusRequest(focus); audio.mode = AudioManager.MODE_NORMAL
    remoteOffer = ""; pendingKind = ""; localSent = false
    if (notify) runCatching { send(old.peer, "call_end", json("callId" to old.id, "reason" to "ended")) }
  }
  fun close() { end(false); timer.shutdownNow(); factory?.dispose(); adm?.release() }
}
