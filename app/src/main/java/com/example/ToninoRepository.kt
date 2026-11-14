package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.gson.*
import com.tonino.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class UiState(val loading: Boolean = true, val ready: Boolean = false, val busy: Boolean = false,
  val name: String = "", val server: String = "", val online: Boolean = false,
  val contacts: List<Contact> = emptyList(), val messages: List<ChatMessage> = emptyList(),
  val invitation: String = "", val error: String? = null, val call: VoiceState = VoiceState())

class ApiFailure(val status: Int) : IOException("HTTP $status")

private class RelayApi(private val engine: SecureEngine) {
  private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(25, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
    .followRedirects(false).followSslRedirects(false).build()
  @Volatile private var poll: Call? = null
  fun request(path: String, method: String = "GET", body: JsonObject? = null, joinCode: String? = null): JsonObject {
    val builder = Request.Builder().url(engine.server + path)
      .header("Authorization", "Bearer ${engine.id}:${engine.token}")
    if (joinCode != null) builder.header("X-Join-Code", joinCode)
    builder.method(method, if (method == "GET") null else (body ?: JsonObject()).toString().toRequestBody("application/json".toMediaType()))
    val call = client.newCall(builder.build())
    if (path == "/v1/inbox") poll = call
    try {
      return call.execute().use { r ->
        if (!r.isSuccessful) throw ApiFailure(r.code)
        val text = r.body?.string() ?: throw IOException("Empty response")
        require(text.length <= 6000000)
        JsonParser.parseString(text).asJsonObject
      }
    } finally { if (path == "/v1/inbox") poll = null }
  }
  fun cancelPoll() { poll?.cancel() }
  fun close() { client.dispatcher.cancelAll(); client.connectionPool.evictAll(); client.dispatcher.executorService.shutdown() }
}

class ToninoRepository(app: Application) : AndroidViewModel(app) {
  private val vault = EncryptedVault(app)
  private val actor = Executors.newSingleThreadExecutor()
  private val polling = AtomicBoolean(false)
  @Volatile private var visible = false
  @Volatile private var closed = false
  private var engine: SecureEngine? = null
  private var api: RelayApi? = null
  private var storageBlocked = false
  private val mutable = MutableStateFlow(UiState())
  val ui = mutable.asStateFlow()
  private var voice: VoiceSession? = null

  init {
    actor.execute {
      try {
        vault.read()?.let { engine = SecureEngine.restore(it, vault::write) }
        engine?.let { api = RelayApi(it) }
        voice = VoiceSession(app, { work -> if (!closed) runCatching { actor.execute(work) }; Unit },
          { peer, kind, data -> requireNotNull(engine).signal(peer, kind, data); publish(); flush() },
          { state -> mutable.update { it.copy(call = state) } },
          { error -> mutable.update { it.copy(error = error) } })
        publish(); mutable.update { it.copy(loading = false) }
        startPoll()
      } catch (e: Throwable) {
        storageBlocked = true
        mutable.update { it.copy(loading = false, error = "Не удалось открыть защищённое хранилище. Не удаляйте данные приложения.") }
      }
    }
  }
  private fun publish() {
    val e = engine ?: return
    mutable.update { it.copy(ready = e.registered, name = e.name, server = e.server, contacts = e.contacts(), messages = e.messages()) }
  }
  private fun error(e: Throwable) {
    val message = when (e) {
      is ApiFailure -> when (e.status) {
        401 -> "Устройство не авторизовано на сервере."
        403 -> "Проверьте код подключения. Для переписки оба собеседника должны добавить друг друга."
        404 -> "Приглашение уже использовано или истекло. Создайте новое."
        409 -> "Собеседник должен добавить ваше приглашение. Затем повторите действие."
        429 -> "Слишком много запросов. Подождите немного."
        503 -> "Ретранслятор звонков ещё не настроен на сервере."
        else -> "Сервер ответил ошибкой ${e.status}."
      }
      is IllegalArgumentException -> e.message ?: "Некорректные данные."
      is IOException -> "Нет связи с сервером. Сохранённые сообщения останутся в очереди."
      else -> "Действие не выполнено. Проверьте соединение и ключ собеседника."
    }
    mutable.update { it.copy(error = message) }
  }
  private fun command(work: () -> Unit) {
    if (closed) return
    mutable.update { it.copy(busy = true, error = null) }
    actor.execute {
      try { work() } catch (e: Throwable) { error(e) }
      finally { publish(); mutable.update { it.copy(busy = false) } }
    }
  }
  fun clearError() { mutable.update { it.copy(error = null) } }
  fun connect(server: String, name: String, joinCode: String) = command {
    require(!storageBlocked) { "Хранилище недоступно. Создание нового профиля заблокировано, чтобы сохранить прежние данные." }
    val normalized = canonicalServer(server, BuildConfig.DEBUG)
    require(name.trim().length in 1..60) { "Введите имя длиной до 60 символов" }
    val existing = engine
    require(existing == null || !existing.registered) {
      "На устройстве уже зарегистрирован профиль. Его ключи и сервер не изменены."
    }
    // Before registration there are no contacts or conversations. A corrected address/name
    // creates fresh credentials; a retry with unchanged fields reuses the saved token.
    val reuse = existing != null && existing.server == normalized && existing.name == name.trim()
    val e = if (reuse) existing!! else SecureEngine.create(normalized, name.trim(), vault::write).also {
      api?.close(); api = null; engine = it
    }
    val a = api ?: RelayApi(e).also { api = it }
    a.request("/v1/register", "POST", json("id" to e.id, "token" to e.token, "bundle" to e.bundle()), joinCode.trim())
    e.markRegistered(); publish(); startPoll()
  }
  fun createInvitation() = command {
    val e = requireNotNull(engine)
    val token = requireNotNull(api).request("/v1/invites", "POST").text("token")
    mutable.update { it.copy(invitation = e.invitation(token)) }
  }
  fun addContact(text: String) = command {
    val e = requireNotNull(engine)
    val invite = SecureEngine.parseInvitation(text.trim(), BuildConfig.DEBUG)
    require(invite.server == e.server && invite.id != e.id) { "Используйте приглашение другого человека с того же сервера" }
    val old = e.contacts().find { it.id == invite.id }
    require(old == null || old.identity == invite.identity) { "Ключ собеседника изменился. Связь заблокирована." }
    val result = requireNotNull(api).request("/v1/invites/redeem", "POST", json("token" to invite.token, "owner" to invite.id))
    require(result.text("id") == invite.id && result.text("identity") == invite.identity) { "Сервер вернул другой ключ. Приглашение отклонено." }
    e.addContact(invite)
  }
  private fun ensureSession(peer: String) {
    val e = requireNotNull(engine)
    if (!e.hasSession(peer)) e.establish(peer, requireNotNull(api).request("/v1/peers/$peer/bundle"))
  }
  fun send(peer: String, text: String) = command {
    ensureSession(peer)
    requireNotNull(engine).sendText(peer, text)
    publish(); flush()
  }
  private fun flush() {
    val e = engine ?: return
    val a = api ?: return
    for (packet in e.pending()) {
      val r = a.request("/v1/messages", "POST", packet.outgoing())
      require(r.text("accepted") == packet.id)
      e.accepted(packet.id)
    }
    publish()
  }
  fun resume() {
    visible = true
    if (!closed) actor.execute { startPoll() }
  }
  fun pause() {
    visible = false
    api?.cancelPoll()
    if (!closed) actor.execute {
      try { voice?.end(true) } catch (_: Exception) { }
    }
  }
  private fun startPoll() {
    val e = engine ?: return
    val a = api ?: return
    if (!visible || !e.registered || !polling.compareAndSet(false, true)) return
    Thread({
      try {
        while (visible && !closed) {
          try {
            if (!ui.value.online) {
              a.request("/v1/status")
              mutable.update { it.copy(online = true) }
              runCatching { actor.submit { flush() }.get() }
            }
            val response = a.request("/v1/inbox")
            mutable.update { it.copy(online = true) }
            val packets = response.array("packets").map { Gson().fromJson(it, Packet::class.java) }
            if (packets.isNotEmpty()) {
              actor.submit {
                for (packet in packets) {
                  try { e.receive(packet)?.let { if (visible) voice?.receive(it) } }
                  catch (ex: IOException) { throw ex } // Disk failure: do not acknowledge.
                  catch (ex: Exception) { mutable.update { it.copy(error = "Отклонён повторный или неподтверждённый пакет. Ключи не изменены.") } }
                }
                publish()
              }.get()
              a.request("/v1/inbox/ack", "POST", json("packets" to packets.map { json("from" to it.from, "id" to it.id) }))
            }
            actor.submit { flush() }.get()
          } catch (ex: Exception) {
            mutable.update { it.copy(online = false) }
            // A full/disk-blocked outbox must not prevent retrying already committed packets.
            if (visible && !closed) runCatching { actor.submit { flush() }.get() }
            if (visible && !closed) Thread.sleep(1500)
          }
        }
      } finally {
        polling.set(false); mutable.update { it.copy(online = false) }
        if (visible && !closed) actor.execute { startPoll() }
      }
    }, "tonino-inbox").apply { isDaemon = true; start() }
  }
  fun call(peer: String) = command {
    ensureSession(peer)
    val turn = requireNotNull(api).request("/v1/turn")
    requireNotNull(voice).start(peer, ui.value.contacts.first { it.id == peer }.name, turn)
  }
  fun acceptCall() = command { requireNotNull(voice).accept(requireNotNull(api).request("/v1/turn")) }
  fun endCall() = command { voice?.end(true) }
  fun toggleMute() = command { voice?.toggleMute() }
  fun toggleSpeaker() = command { voice?.toggleSpeaker() }
  fun safetyCode(peer: String): String = engine?.safetyCode(peer).orEmpty()
  override fun onCleared() {
    closed = true; visible = false; api?.cancelPoll()
    actor.execute { voice?.close(); api?.close() }
    actor.shutdown()
  }
}
