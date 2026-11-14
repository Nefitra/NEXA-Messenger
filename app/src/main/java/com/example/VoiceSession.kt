package com.example

import android.content.Context
import android.media.*
import android.os.Build
import android.util.Log
import com.google.gson.JsonObject
import com.tonino.core.*
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.UUID
import java.util.concurrent.*

data class VoiceState(val id: String = "", val peer: String = "", val phase: String = "idle",
  val muted: Boolean = false, val speaker: Boolean = false)

/** Audio only. SDP and incremental relay candidates use the authenticated Signal channel. */
class VoiceSession(private val context: Context, private val dispatch: (() -> Unit) -> Unit,
  private val send: (String, String, JsonObject) -> Unit, private val onState: (VoiceState) -> Unit,
  private val onError: (String) -> Unit) {
  private companion object { val initializationLock = Any(); var initialized = false }
  private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val timer = Executors.newSingleThreadScheduledExecutor()
  private var setupTimer: ScheduledFuture<*>? = null
  private var disconnectTimer: ScheduledFuture<*>? = null
  private var adm: JavaAudioDeviceModule? = null
  private var factory: PeerConnectionFactory? = null
  private var peer: PeerConnection? = null
  private var source: AudioSource? = null
  private var track: AudioTrack? = null
  private var state = VoiceState()
  private var remoteOffer = ""
  private var pendingKind = ""
  private var localSent = false
  private var localRelays = 0
  private var remoteReady = false
  private val pendingIce = ArrayList<IceCandidate>()
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
    try { pendingKind = "call_offer"; createPeer(turn); createDescription(false); timeout(state.id) }
    catch (e: Throwable) { end(false); throw e }
  }
  /** Move the UI immediately, before fetching TURN; double taps become harmless. */
  fun beginAccept(): Boolean {
    if (state.phase != "incoming") return false
    update(state.copy(phase = "accepting")); timeout(state.id)
    return true
  }
  fun cancelAccept() { if (state.phase == "accepting") { update(state.copy(phase = "incoming")); timeout(state.id) } }
  fun accept(turn: JsonObject) {
    if (state.phase != "incoming" && state.phase != "accepting") return
    update(state.copy(phase = "connecting"))
    try {
      pendingKind = "call_answer"; createPeer(turn)
      setRemote(SessionDescription.Type.OFFER, remoteOffer) { createDescription(true) }
      timeout(state.id)
    } catch (e: Throwable) { end(true); throw e }
  }
  fun receive(frame: Frame) {
    if (!frame.kind.startsWith("call_") || kotlin.math.abs(System.currentTimeMillis() - frame.time) > 120000) return
    val data = frame.data; val id = data.text("callId")
    runCatching { UUID.fromString(id) }.getOrElse { return }
    when (frame.kind) {
      "call_offer" -> {
        if (state.phase != "idle") {
          if (state.id != id) runCatching { send(frame.from, "call_end", json("callId" to id, "reason" to "busy")) }
          return
        }
        require(data.text("sdp").length in 50..30000)
        remoteOffer = data.text("sdp"); remoteReady = false; pendingIce.clear()
        update(VoiceState(id, frame.from, "incoming")); timeout(id)
      }
      "call_answer" -> if (state.id == id && state.peer == frame.from && state.phase == "outgoing") {
        require(data.text("sdp").length in 50..30000)
        update(state.copy(phase = "connecting"))
        setRemote(SessionDescription.Type.ANSWER, data.text("sdp")) {}
      }
      "call_ice" -> if (state.id == id && state.peer == frame.from) {
        val candidate = data.text("candidate"); val mid = data.text("mid"); val line = data.number("line")
        require(candidate.length in 10..3000 && !candidate.contains('\n') && mid.length <= 32 && line in 0..8)
        val ice = IceCandidate(mid.ifEmpty { null }, line.toInt(), candidate)
        if (remoteReady) peer?.addIceCandidate(ice) else if (pendingIce.size < 128) pendingIce.add(ice)
      }
      "call_end" -> if (state.id == id && state.peer == frame.from) end(false)
    }
  }
  private fun timeout(id: String) {
    setupTimer?.cancel(false)
    setupTimer = timer.schedule({ dispatch {
      if (state.id == id && state.phase != "active" && state.phase != "idle") {
        onError("Собеседник не ответил или соединение не установлено."); end(true)
      }
    } }, 60, TimeUnit.SECONDS)
  }
  private fun createPeer(turn: JsonObject) {
    CallAudioService.start(context) { dispatch { end(true) } }
    val factory = audioFactory()
    val all = turn.array("urls").map { it.asString }
    require(all.isNotEmpty() && all.all { it.startsWith("turn:") || it.startsWith("turns:") })
    // A TCP connection to TURN works in mobile/Wi-Fi networks that filter UDP.
    // TURN-to-TURN media still stays relayed and DTLS/SRTP protected.
    val reliable = all.filter { it.startsWith("turns:") || it.contains("transport=tcp") }
    val urls = reliable.ifEmpty { all }
    val ice = PeerConnection.IceServer.builder(urls).setUsername(turn.text("username")).setPassword(turn.text("credential")).createIceServer()
    val config = PeerConnection.RTCConfiguration(listOf(ice)).apply {
      sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
      iceTransportsType = PeerConnection.IceTransportsType.RELAY
      continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
    }
    val callId = state.id
    localSent = false; localRelays = 0; remoteReady = false
    peer = factory.createPeerConnection(config, object : PeerConnection.Observer {
      override fun onSignalingChange(s: PeerConnection.SignalingState?) {}
      override fun onConnectionChange(s: PeerConnection.PeerConnectionState?) { dispatch {
        if (state.id != callId) return@dispatch
        when (s) {
          PeerConnection.PeerConnectionState.CONNECTED -> {
            setupTimer?.cancel(false); disconnectTimer?.cancel(false)
            update(state.copy(phase = "active"))
          }
          PeerConnection.PeerConnectionState.FAILED -> transportFailure(callId)
          else -> {}
        }
      } }
      override fun onIceConnectionChange(s: PeerConnection.IceConnectionState?) { dispatch {
        if (state.id != callId) return@dispatch
        Log.i("ToninoCall", "ICE=$s relayCandidates=$localRelays")
        when (s) {
          PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> disconnectTimer?.cancel(false)
          PeerConnection.IceConnectionState.FAILED -> transportFailure(callId)
          PeerConnection.IceConnectionState.DISCONNECTED -> {
            update(state.copy(phase = "connecting")); disconnectTimer?.cancel(false)
            disconnectTimer = timer.schedule({ dispatch {
              if (state.id == callId && state.phase == "connecting") transportFailure(callId)
            } }, 30, TimeUnit.SECONDS)
          }
          else -> {}
        }
      } }
      override fun onIceConnectionReceivingChange(receiving: Boolean) {}
      override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?) { if (s == PeerConnection.IceGatheringState.COMPLETE) dispatch {
        if (state.id == callId && !localSent) {
          if (localRelays == 0) { onError("Ретранслятор не выдал адрес для звонка. Проверьте подключение к сети."); end(true) }
          else sendLocal()
        }
      } }
      override fun onIceCandidate(candidate: IceCandidate?) { if (candidate != null) dispatch {
        if (state.id != callId || !candidate.sdp.contains(" typ relay")) return@dispatch
        localRelays++
        if (!localSent) sendLocal()
        if (localSent) runCatching { send(state.peer, "call_ice", json("callId" to callId, "candidate" to candidate.sdp,
          "mid" to candidate.sdpMid.orEmpty(), "line" to candidate.sdpMLineIndex)) }
      } }
      override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
      override fun onAddStream(stream: MediaStream?) {}
      override fun onRemoveStream(stream: MediaStream?) {}
      override fun onDataChannel(channel: DataChannel?) {}
      override fun onRenegotiationNeeded() {}
      override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
    }) ?: error("WebRTC недоступен")
    audio.requestAudioFocus(focus); audio.mode = AudioManager.MODE_IN_COMMUNICATION
    routeAudio()
    source = factory.createAudioSource(MediaConstraints())
    track = factory.createAudioTrack("tonino-audio", source).also { it.setEnabled(!state.muted) }
    peer!!.addTrack(track, listOf("tonino"))
  }
  private fun transportFailure(callId: String) {
    if (state.id != callId || state.phase == "idle") return
    onError("Не удалось установить голосовую связь через ретранслятор. Попробуйте другую сеть и повторите звонок.")
    end(true)
  }
  private fun createDescription(answer: Boolean) {
    val callId = state.id
    val observer = object : SdpObserver {
      override fun onCreateSuccess(sdp: SessionDescription?) { dispatch {
        if (state.id != callId || sdp == null) return@dispatch
        peer?.setLocalDescription(object : SdpObserver {
          override fun onSetSuccess() { dispatch { if (state.id == callId && localRelays > 0) sendLocal() } }
          override fun onSetFailure(message: String?) { failure(callId) }
          override fun onCreateSuccess(sdp: SessionDescription?) {}
          override fun onCreateFailure(message: String?) {}
        }, sdp)
      } }
      override fun onCreateFailure(message: String?) { failure(callId) }
      override fun onSetSuccess() {}
      override fun onSetFailure(message: String?) {}
    }
    if (answer) peer?.createAnswer(observer, MediaConstraints()) else peer?.createOffer(observer, MediaConstraints())
  }
  private fun sendLocal() {
    if (localSent || state.phase == "idle" || localRelays == 0) return
    val sdp = peer?.localDescription ?: return
    localSent = true
    try { send(state.peer, pendingKind, json("callId" to state.id, "sdp" to sdp.description)) }
    catch (_: Exception) { onError("Параметры звонка сохранены, но сервер пока недоступен.") }
  }
  private fun setRemote(type: SessionDescription.Type, description: String, success: () -> Unit) {
    val callId = state.id
    peer?.setRemoteDescription(object : SdpObserver {
      override fun onSetSuccess() { dispatch { if (state.id == callId) {
        remoteReady = true; pendingIce.forEach { peer?.addIceCandidate(it) }; pendingIce.clear(); success()
      } } }
      override fun onSetFailure(message: String?) { failure(callId) }
      override fun onCreateSuccess(sdp: SessionDescription?) {}
      override fun onCreateFailure(message: String?) {}
    }, SessionDescription(type, description))
  }
  private fun failure(callId: String) { dispatch { if (state.id == callId) { onError("Не удалось согласовать защищённый звонок."); end(true) } } }
  fun toggleMute() { update(state.copy(muted = !state.muted)); track?.setEnabled(!state.muted) }
  internal fun collectStats(callback: RTCStatsCollectorCallback) { checkNotNull(peer) { "Нет активного соединения" }.getStats(callback) }
  @Suppress("DEPRECATION") private fun routeAudio() {
    if (Build.VERSION.SDK_INT >= 31) {
      val type = if (state.speaker) AudioDeviceInfo.TYPE_BUILTIN_SPEAKER else AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
      audio.availableCommunicationDevices.firstOrNull { it.type == type }?.let { audio.setCommunicationDevice(it) }
    } else audio.isSpeakerphoneOn = state.speaker
  }
  fun toggleSpeaker() { update(state.copy(speaker = !state.speaker)); routeAudio() }
  fun end(notify: Boolean) {
    if (state.phase == "idle") return
    val old = state; update(VoiceState())
    setupTimer?.cancel(false); disconnectTimer?.cancel(false)
    peer?.close(); peer?.dispose(); peer = null
    track?.dispose(); track = null; source?.dispose(); source = null
    if (Build.VERSION.SDK_INT >= 31) audio.clearCommunicationDevice()
    audio.abandonAudioFocusRequest(focus); audio.mode = AudioManager.MODE_NORMAL
    CallAudioService.stop(context)
    remoteOffer = ""; pendingKind = ""; localSent = false; remoteReady = false; pendingIce.clear()
    if (notify) runCatching { send(old.peer, "call_end", json("callId" to old.id, "reason" to "ended")) }
  }
  fun close() { end(false); timer.shutdownNow(); factory?.dispose(); adm?.release() }
}
