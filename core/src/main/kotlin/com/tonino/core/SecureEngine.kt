package com.tonino.core

import com.google.gson.*
import org.signal.libsignal.protocol.*
import org.signal.libsignal.protocol.ecc.*
import org.signal.libsignal.protocol.kem.*
import org.signal.libsignal.protocol.message.*
import org.signal.libsignal.protocol.state.*
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore
import org.signal.libsignal.protocol.util.KeyHelper
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

internal fun ByteArray.b64(): String = Base64.getEncoder().encodeToString(this)
internal fun String.bytes(): ByteArray = Base64.getDecoder().decode(this)
fun json(vararg values: Pair<String, Any?>): JsonObject = JsonObject().apply {
  for ((key, value) in values) add(key, Gson().toJsonTree(value))
}
fun JsonObject.text(key: String): String = get(key)?.takeUnless { it.isJsonNull }?.asString ?: ""
fun JsonObject.number(key: String): Long = get(key)?.asLong ?: 0L
fun JsonObject.obj(key: String): JsonObject = getAsJsonObject(key)
fun JsonObject.array(key: String): JsonArray = getAsJsonArray(key)

data class Packet(val id: String, val to: String, val type: Int, val body: String, val from: String = "") {
  fun outgoing(): JsonObject = json("id" to id, "to" to to, "type" to type, "body" to body)
}
data class Frame(val id: String, val from: String, val kind: String, val time: Long, val data: JsonObject)
data class Contact(val id: String, val name: String, val identity: String)
data class ChatMessage(val id: String, val peer: String, val text: String, val time: Long, val self: Boolean, val status: String)
data class Invitation(val server: String, val id: String, val identity: String, val token: String, val name: String)

fun canonicalServer(input: String, allowLocalHttp: Boolean = false): String {
  val uri = URI(input.trim().trimEnd('/'))
  val h = uri.host?.lowercase() ?: error("Укажите полный адрес сервера")
  val local = h == "localhost" || h == "127.0.0.1" || h == "10.0.2.2" ||
    Regex("10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}").matches(h) ||
    Regex("192\\.168\\.\\d{1,3}\\.\\d{1,3}").matches(h) ||
    Regex("172\\.(1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.\\d{1,3}").matches(h)
  require(uri.scheme == "https" || (allowLocalHttp && local && uri.scheme == "http")) { "Нужен HTTPS. HTTP разрешён только для локального теста." }
  require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null && uri.path.orEmpty().isEmpty()) { "Адрес должен содержать только схему, сервер и порт" }
  return URI(uri.scheme, null, h, uri.port, null, null, null).toString()
}

/** One serialized state contains ratchets, history and the retry outbox. The Android caller encrypts
 * and atomically replaces it before any ciphertext is released to the network or acknowledged. */
class SecureEngine private constructor(private var state: JsonObject, private val persist: (String) -> Unit) {
  private val store = PinnedStore { state }
  val id: String get() = state.text("id")
  val server: String get() = state.text("server")
  val name: String get() = state.text("name")
  val token: String get() = state.text("token")
  val registered: Boolean get() = state.get("registered")?.asBoolean ?: false
  val identity: String get() = store.identityKeyPair.publicKey.serialize().b64()
  private val address get() = SignalProtocolAddress(id, 1)

  companion object {
    fun create(server: String, name: String, persist: (String) -> Unit): SecureEngine {
      require(name.isNotBlank() && name.length <= 60)
      val identity = IdentityKeyPair.generate()
      val signed = ECKeyPair.generate()
      val kyber = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
      val signedRecord = SignedPreKeyRecord(1, System.currentTimeMillis(), signed,
        identity.privateKey.calculateSignature(signed.publicKey.serialize()))
      val kyberRecord = KyberPreKeyRecord(1, System.currentTimeMillis(), kyber,
        identity.privateKey.calculateSignature(kyber.publicKey.serialize()))
      val root = json("version" to 1, "id" to UUID.randomUUID().toString(), "server" to server,
        "name" to name, "token" to Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) }),
        "registered" to false, "identityPair" to identity.serialize().b64(),
        "registrationId" to KeyHelper.generateRegistrationId(false), "signed" to signedRecord.serialize().b64(),
        "kyber" to kyberRecord.serialize().b64())
      for (key in listOf("contacts", "sessions", "prekeys", "kyberKeys")) root.add(key, JsonObject())
      for (keyId in 2..17) {
        val ec = ECKeyPair.generate()
        val pq = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
        root.obj("prekeys").addProperty(keyId.toString(), PreKeyRecord(keyId, ec).serialize().b64())
        root.obj("kyberKeys").addProperty(keyId.toString(), KyberPreKeyRecord(keyId, System.currentTimeMillis(), pq,
          identity.privateKey.calculateSignature(pq.publicKey.serialize())).serialize().b64())
      }
      for (key in listOf("messages", "outbox", "seen", "kyberUsed")) root.add(key, JsonArray())
      return SecureEngine(root, persist).also { persist(root.toString()) }
    }
    fun restore(serialized: String, persist: (String) -> Unit): SecureEngine {
      val root = JsonParser.parseString(serialized).asJsonObject
      require(root.number("version") == 1L) { "Неподдерживаемая версия хранилища" }
      return SecureEngine(root, persist)
    }
    fun parseInvitation(text: String, allowLocalHttp: Boolean = false): Invitation {
      require(text.length in 50..6000 && text.startsWith("tonino://invite/")) { "Это не приглашение Tonino" }
      val data = String(Base64.getUrlDecoder().decode(text.removePrefix("tonino://invite/")), Charsets.UTF_8)
      val o = JsonParser.parseString(data).asJsonObject
      require(o.number("v") == 1L && UUID.fromString(o.text("id")).toString() == o.text("id"))
      IdentityKey(o.text("identity").bytes())
      require(Regex("[A-Za-z0-9_-]{43}").matches(o.text("token")) && o.text("name").length in 1..60)
      return Invitation(canonicalServer(o.text("server"), allowLocalHttp), o.text("id"), o.text("identity"), o.text("token"), o.text("name"))
    }
  }
  @Synchronized private fun <T> transaction(block: () -> T): T {
    val before = state.deepCopy()
    try { val result = block(); persist(state.toString()); return result }
    catch (e: Throwable) { state = before; throw e }
  }
  @Synchronized fun snapshot(): String = state.toString()
  fun setToken(token: String) = transaction { require(Regex("[A-Za-z0-9_-]{43}").matches(token)); state.addProperty("token", token) }
  fun markRegistered() = transaction { state.addProperty("registered", true) }
  @Synchronized fun bundle(): JsonObject {
    val signed = store.loadSignedPreKey(1)
    val kyber = store.loadKyberPreKey(1)
    val oneTime = state.obj("prekeys").keySet().map { key ->
      val ec = store.loadPreKey(key.toInt()); val pq = store.loadKyberPreKey(key.toInt())
      json("id" to key.toInt(), "ec" to ec.keyPair.publicKey.serialize().b64(),
        "kyber" to pq.keyPair.publicKey.serialize().b64(), "signature" to pq.signature.b64())
    }
    return json("registrationId" to store.localRegistrationId, "identity" to identity,
      "signedKey" to signed.keyPair.publicKey.serialize().b64(), "signedSignature" to signed.signature.b64(),
      "kyberKey" to kyber.keyPair.publicKey.serialize().b64(), "kyberSignature" to kyber.signature.b64(), "oneTimeKeys" to oneTime)
  }
  fun invitation(inviteToken: String): String {
    require(Regex("[A-Za-z0-9_-]{43}").matches(inviteToken))
    return "tonino://invite/" + Base64.getUrlEncoder().withoutPadding().encodeToString(
      json("v" to 1, "server" to server, "id" to id, "identity" to identity, "token" to inviteToken, "name" to name).toString().toByteArray())
  }
  fun addContact(invite: Invitation) = transaction {
    require(invite.server == server && invite.id != id) { "Приглашение относится к другому серверу или вашему устройству" }
    IdentityKey(invite.identity.bytes())
    val old = state.obj("contacts").getAsJsonObject(invite.id)
    require(old == null || old.text("identity") == invite.identity) { "Ключ собеседника изменился. Связь заблокирована." }
    state.obj("contacts").add(invite.id, json("name" to invite.name, "identity" to invite.identity))
  }
  @Synchronized fun contacts(): List<Contact> = state.obj("contacts").entrySet().map { (id, v) ->
    Contact(id, v.asJsonObject.text("name"), v.asJsonObject.text("identity")) }
  @Synchronized fun messages(): List<ChatMessage> = state.array("messages").map { v -> v.asJsonObject.let {
    ChatMessage(it.text("id"), it.text("peer"), it.text("text"), it.number("time"), it.get("self").asBoolean, it.text("status")) } }
  @Synchronized fun pending(): List<Packet> = state.array("outbox").map { Gson().fromJson(it, Packet::class.java) }
  @Synchronized fun hasSession(peer: String): Boolean = store.containsSession(SignalProtocolAddress(peer, 1))
  fun establish(peer: String, bundle: JsonObject) = transaction {
    require(state.obj("contacts").getAsJsonObject(peer)?.text("identity") == bundle.text("identity")) { "Сервер вернул другой ключ собеседника. Связь заблокирована." }
    val oneTime = if (bundle.has("preKeyId")) bundle else bundle.array("oneTimeKeys").first().asJsonObject.let {
      json("preKeyId" to it.get("id").asInt, "preKey" to it.text("ec"), "kyberId" to it.get("id").asInt,
        "kyberKey" to it.text("kyber"), "kyberSignature" to it.text("signature"))
    }
    val prekey = PreKeyBundle(bundle.get("registrationId").asInt, 1, oneTime.get("preKeyId").asInt, ECPublicKey(oneTime.text("preKey").bytes()),
      1, ECPublicKey(bundle.text("signedKey").bytes()), bundle.text("signedSignature").bytes(),
      IdentityKey(bundle.text("identity").bytes()), oneTime.get("kyberId").asInt, KEMPublicKey(oneTime.text("kyberKey").bytes()), oneTime.text("kyberSignature").bytes())
    SessionBuilder(store, SignalProtocolAddress(peer, 1), address).process(prekey)
  }
  private fun seal(peer: String, kind: String, data: JsonObject): Packet {
    val id = UUID.randomUUID().toString()
    val plain = json("v" to 1, "id" to id, "from" to this.id, "to" to peer, "kind" to kind,
      "time" to System.currentTimeMillis(), "data" to data)
    val bytes = plain.toString().toByteArray()
    require(bytes.size <= 30000) { "Сообщение слишком большое" }
    val encrypted = SessionCipher(store, address, SignalProtocolAddress(peer, 1)).encrypt(bytes)
    val packet = Packet(id, peer, encrypted.type, encrypted.serialize().b64())
    state.array("outbox").add(Gson().toJsonTree(packet))
    return packet
  }
  fun sendText(peer: String, text: String): Packet = transaction {
    require(text.isNotBlank() && text.length <= 6000) { "Сообщение должно содержать от 1 до 6000 символов" }
    val p = seal(peer, "text", json("text" to text))
    addHistory(p.id, peer, text, System.currentTimeMillis(), true, "queued")
    p
  }
  fun signal(peer: String, kind: String, data: JsonObject): Packet = transaction {
    require(kind in setOf("call_offer", "call_answer", "call_ice", "call_end"))
    seal(peer, kind, data)
  }
  fun accepted(id: String) = transaction {
    val out = state.array("outbox")
    for (i in out.size() - 1 downTo 0) if (out[i].asJsonObject.text("id") == id) out.remove(i)
    for (m in state.array("messages")) if (m.asJsonObject.text("id") == id && m.asJsonObject.text("status") == "queued") m.asJsonObject.addProperty("status", "sent")
  }
  /** Returns null for an already committed packet. A changed envelope is rejected before commit. */
  fun receive(p: Packet): Frame? = transaction {
    require(p.to == id && state.obj("contacts").has(p.from)) { "Неизвестный собеседник" }
    val seen = "${p.from}:${p.id}"
    if (state.array("seen").any { it.asString == seen }) return@transaction null
    val cipher = SessionCipher(store, address, SignalProtocolAddress(p.from, 1))
    val decoded = when (p.type) {
      CiphertextMessage.PREKEY_TYPE -> cipher.decrypt(PreKeySignalMessage(p.body.bytes()))
      CiphertextMessage.WHISPER_TYPE -> cipher.decrypt(SignalMessage(p.body.bytes()))
      else -> error("Неизвестный тип шифротекста")
    }
    val plain = JsonParser.parseString(String(decoded, Charsets.UTF_8)).asJsonObject
    require(plain.number("v") == 1L && plain.text("id") == p.id && plain.text("from") == p.from && plain.text("to") == id) { "Заголовок сообщения был подменён" }
    val kind = plain.text("kind")
    require(kind in setOf("text", "receipt", "call_offer", "call_answer", "call_ice", "call_end"))
    val data = plain.obj("data")
    val frame = Frame(p.id, p.from, kind, plain.number("time"), data)
    if (kind == "text") {
      val text = data.text("text")
      require(text.isNotBlank() && text.length <= 6000)
      addHistory(p.id, p.from, text, frame.time, false, "received")
      seal(p.from, "receipt", json("id" to p.id))
    } else if (kind == "receipt") {
      for (m in state.array("messages")) {
        val o = m.asJsonObject
        if (o.text("id") == data.text("id") && o.text("peer") == p.from && o.get("self").asBoolean) o.addProperty("status", "delivered")
      }
    }
    state.array("seen").add(seen)
    while (state.array("seen").size() > 3000) state.array("seen").remove(0)
    frame
  }
  private fun addHistory(id: String, peer: String, text: String, time: Long, self: Boolean, status: String) {
    state.array("messages").add(json("id" to id, "peer" to peer, "text" to text, "time" to time, "self" to self, "status" to status))
    while (state.array("messages").size() > 500 || state.array("messages").toString().toByteArray().size > 8000000) state.array("messages").remove(0)
  }
  @Synchronized fun safetyCode(peer: String): String {
    val contact = contacts().first { it.id == peer }
    val endpoints = listOf("$id:$identity", "${contact.id}:${contact.identity}").sorted().joinToString("|")
    return MessageDigest.getInstance("SHA-256").digest(endpoints.toByteArray()).joinToString("") { "%02X".format(it) }.chunked(4).joinToString(" ")
  }
}

/** The trust policy is pinned invitations in both directions, rather than silent first-use trust. */
private class PinnedStore(private val root: () -> JsonObject) : InMemorySignalProtocolStore(
  IdentityKeyPair(root().text("identityPair").bytes()), root().get("registrationId").asInt) {
  override fun isTrustedIdentity(address: SignalProtocolAddress, key: IdentityKey, direction: IdentityKeyStore.Direction): Boolean =
    address.deviceId == 1 && root().obj("contacts").getAsJsonObject(address.name)?.text("identity") == key.serialize().b64()
  override fun saveIdentity(address: SignalProtocolAddress, key: IdentityKey): IdentityKeyStore.IdentityChange {
    require(isTrustedIdentity(address, key, IdentityKeyStore.Direction.RECEIVING)) { "Ключ собеседника не совпадает с приглашением" }
    return IdentityKeyStore.IdentityChange.NEW_OR_UNCHANGED
  }
  override fun getIdentity(address: SignalProtocolAddress): IdentityKey? = root().obj("contacts").getAsJsonObject(address.name)?.let { IdentityKey(it.text("identity").bytes()) }
  override fun loadSession(address: SignalProtocolAddress): SessionRecord = root().obj("sessions").get(address.name)?.let { SessionRecord(it.asString.bytes()) } ?: SessionRecord()
  override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) { root().obj("sessions").addProperty(address.name, record.serialize().b64()) }
  override fun containsSession(address: SignalProtocolAddress): Boolean = root().obj("sessions").has(address.name)
  override fun deleteSession(address: SignalProtocolAddress) { root().obj("sessions").remove(address.name) }
  override fun deleteAllSessions(name: String) { root().obj("sessions").remove(name) }
  override fun getSubDeviceSessions(name: String): List<Int> = emptyList()
  override fun loadExistingSessions(addresses: List<SignalProtocolAddress>): List<SessionRecord> = addresses.map {
    if (!containsSession(it)) throw NoSessionException(it, "Session absent")
    loadSession(it)
  }
  override fun loadPreKey(id: Int): PreKeyRecord = root().obj("prekeys").get(id.toString())?.let { PreKeyRecord(it.asString.bytes()) } ?: throw InvalidKeyIdException("One-time pre-key absent")
  override fun storePreKey(id: Int, record: PreKeyRecord) { root().obj("prekeys").addProperty(id.toString(), record.serialize().b64()) }
  override fun containsPreKey(id: Int): Boolean = root().obj("prekeys").has(id.toString())
  override fun removePreKey(id: Int) { root().obj("prekeys").remove(id.toString()) }
  override fun loadSignedPreKey(id: Int): SignedPreKeyRecord { if (id != 1) throw InvalidKeyIdException("Unknown signed pre-key"); return SignedPreKeyRecord(root().text("signed").bytes()) }
  override fun loadSignedPreKeys(): List<SignedPreKeyRecord> = listOf(loadSignedPreKey(1))
  override fun containsSignedPreKey(id: Int): Boolean = id == 1
  override fun loadKyberPreKey(id: Int): KyberPreKeyRecord = if (id == 1) KyberPreKeyRecord(root().text("kyber").bytes()) else
    root().obj("kyberKeys").get(id.toString())?.let { KyberPreKeyRecord(it.asString.bytes()) } ?: throw InvalidKeyIdException("One-time Kyber key absent")
  override fun loadKyberPreKeys(): List<KyberPreKeyRecord> = listOf(loadKyberPreKey(1)) + root().obj("kyberKeys").keySet().map { loadKyberPreKey(it.toInt()) }
  override fun containsKyberPreKey(id: Int): Boolean = id == 1 || root().obj("kyberKeys").has(id.toString())
  override fun storeKyberPreKey(id: Int, record: KyberPreKeyRecord) { require(id != 1); root().obj("kyberKeys").addProperty(id.toString(), record.serialize().b64()) }
  override fun markKyberPreKeyUsed(id: Int, signedId: Int, baseKey: ECPublicKey) {
    val marker = "$id:$signedId:${baseKey.serialize().b64()}"
    if (root().array("kyberUsed").any { it.asString == marker }) throw ReusedBaseKeyException()
    root().array("kyberUsed").add(marker)
    if (id != 1) root().obj("kyberKeys").remove(id.toString())
  }
}
