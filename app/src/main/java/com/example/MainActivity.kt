package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.tonino.core.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
  private lateinit var repository: ToninoRepository
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    repository = ViewModelProvider(this)[ToninoRepository::class.java]
    setContent { MyApplicationTheme { ToninoApp(repository) } }
  }
  override fun onStart() { super.onStart(); if (::repository.isInitialized) repository.resume() }
  override fun onStop() { if (::repository.isInitialized) repository.pause(); super.onStop() }
}

private val BubbleShape = RoundedCornerShape(22.dp)
private fun time(ms: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

@Composable private fun ToninoApp(repo: ToninoRepository) {
  val ui by repo.ui.collectAsStateWithLifecycle()
  var selected by rememberSaveable { mutableStateOf<String?>(null) }
  var tab by rememberSaveable { mutableStateOf(0) }
  var showAdd by remember { mutableStateOf(false) }
  var showInvite by remember { mutableStateOf(false) }
  var showSafety by remember { mutableStateOf<String?>(null) }
  var showLicenses by remember { mutableStateOf(false) }
  var micAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var micError by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
    if (allowed) micAction?.invoke() else micError = true
    micAction = null
  }
  fun microphone(action: () -> Unit) {
    if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) action()
    else { micAction = action; mic.launch(Manifest.permission.RECORD_AUDIO) }
  }
  Surface(color = MidnightBlue, modifier = Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
      if (ui.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NeonCyan) }
      } else if (!ui.ready) {
        SetupScreen(ui, repo::connect)
      } else {
        val contact = ui.contacts.find { it.id == selected }
        if (contact != null) {
          Conversation(ui, contact, { selected = null }, { showSafety = contact.id },
            { microphone { repo.call(contact.id) } }, { repo.send(contact.id, it) })
        } else {
          Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandMark(42)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
              Text("Tonino", fontSize = 25.sp, fontWeight = FontWeight.Bold)
              Text(if (ui.online) "Сервер подключён" else "Подключаемся…", color = if (ui.online) CyberBlue else TextSecondary, fontSize = 12.sp)
            }
            IconButton(onClick = { showInvite = true; repo.createInvitation() }, enabled = !ui.busy) { Icon(Icons.Default.QrCode2, "Моё приглашение", tint = CyberBlue) }
            IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.PersonAddAlt1, "Добавить собеседника", tint = TextPrimary) }
          }
          Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
              0, 1 -> {
                if (ui.contacts.isEmpty()) EmptyContacts { showAdd = true }
                else LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  item { Text(if (tab == 0) "Сообщения" else "Голосовые звонки", fontSize = 23.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp)) }
                  items(ui.contacts, key = { it.id }) { c ->
                    ContactRow(c, ui.messages.lastOrNull { it.peer == c.id }, tab == 1) {
                      if (tab == 1) microphone { repo.call(c.id) } else selected = c.id
                    }
                  }
                }
              }
              else -> SettingsScreen(ui, { showInvite = true; repo.createInvitation() }, { showAdd = true }, { showLicenses = true })
            }
          }
          NavigationBar(containerColor = DarkSlate, tonalElevation = 0.dp) {
            listOf("Чаты", "Звонки", "Настройки").forEachIndexed { index, label ->
              NavigationBarItem(selected = tab == index, onClick = { tab = index }, label = { Text(label) },
                icon = { Icon(when(index) { 0 -> Icons.AutoMirrored.Filled.Chat; 1 -> Icons.Default.Call; else -> Icons.Default.Tune }, label) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = NeonCyan, selectedTextColor = NeonCyan, indicatorColor = CyberPurple.copy(alpha = .2f)))
            }
          }
        }
      }
    }
    if (ui.busy) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) { LinearProgressIndicator(Modifier.fillMaxWidth(), color = CyberBlue) }
  }
  if (ui.error != null) AlertDialog(onDismissRequest = repo::clearError, title = { Text("Tonino") }, text = { Text(ui.error!!) }, confirmButton = { TextButton(onClick = repo::clearError) { Text("Понятно") } })
  if (micError) AlertDialog(onDismissRequest = { micError = false }, title = { Text("Доступ к микрофону") }, text = { Text("Разрешите микрофон, чтобы говорить с собеседником. Разрешение можно изменить в настройках Android.") }, confirmButton = { TextButton(onClick = { micError = false }) { Text("Понятно") } })
  if (showInvite) InviteDialog(ui.invitation, ui.busy) { showInvite = false }
  if (showAdd) AddDialog({ showAdd = false }, { repo.addContact(it); showAdd = false })
  if (showLicenses) LicenseDialog { showLicenses = false }
  showSafety?.let { id ->
    val clipboard = LocalClipboardManager.current
    val code = repo.safetyCode(id)
    AlertDialog(onDismissRequest = { showSafety = null }, title = { Text("Проверка собеседника") },
      text = { Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Ключ закреплён по приглашению. Сверьте этот код лично или по другому доверенному каналу: у вас он должен совпадать.")
        Text(code.split(' ').chunked(4).joinToString("\n") { it.joinToString(" ") }, fontFamily = FontFamily.Monospace, color = CyberBlue)
      } }, confirmButton = { TextButton(onClick = { clipboard.setText(AnnotatedString(code)) }) { Text("Копировать") } },
      dismissButton = { TextButton(onClick = { showSafety = null }) { Text("Закрыть") } })
  }
  if (ui.call.phase != "idle" && ui.error == null && !micError) CallDialog(ui.call, ui.contacts.find { it.id == ui.call.peer }?.name ?: "Собеседник",
    { microphone { repo.acceptCall() } }, repo::endCall, repo::toggleMute, repo::toggleSpeaker)
}

@Composable private fun BrandMark(size: Int) {
  Box(Modifier.size(size.dp).clip(RoundedCornerShape((size / 3).dp)).background(Brush.linearGradient(listOf(CyberPurple, CyberBlue))), contentAlignment = Alignment.Center) {
    Text("T", color = Color.White, fontWeight = FontWeight.Black, fontSize = (size * .65).sp)
  }
}
@Composable internal fun SetupScreen(ui: UiState, connect: (String, String, String) -> Unit) {
  var server by rememberSaveable(ui.server) { mutableStateOf(ui.server.ifEmpty { "https://" }) }
  var name by rememberSaveable(ui.name) { mutableStateOf(ui.name) }
  var code by remember { mutableStateOf("") }
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(26.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
    Spacer(Modifier.height(20.dp)); BrandMark(68)
    Text("Ближе друг к другу.", fontSize = 31.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold)
    Text("Личный чат и голос в пространстве Tonino.", color = TextSecondary, fontSize = 16.sp)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(server, { server = it }, label = { Text("Адрес сервера") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(name, { name = it.take(60) }, label = { Text("Ваше имя") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(code, { code = it }, label = { Text("Код подключения") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    Button(onClick = { connect(server, name, code) }, enabled = !ui.busy && name.isNotBlank() && code.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp), shape = BubbleShape) {
      Icon(Icons.Default.Lock, null); Spacer(Modifier.width(10.dp)); Text("Создать защищённый профиль")
    }
    Text("Номер телефона и адресная книга не нужны. Ключи создаются на этом устройстве.", color = TextSecondary, fontSize = 13.sp)
    Text("Тестовая версия: для переписки и звонков держите приложение открытым. При выходе из приложения звонок завершается.", color = GoldAccent, fontSize = 12.sp)
  }
}
@Composable internal fun EmptyContacts(add: () -> Unit) {
  Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
    Icon(Icons.AutoMirrored.Filled.Chat, null, tint = CyberPurple, modifier = Modifier.size(76.dp))
    Spacer(Modifier.height(24.dp)); Text("Здесь начинается общение", fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
    Spacer(Modifier.height(12.dp)); Text("Обменяйтесь приглашениями и добавьте друг друга. Ваш первый разговор появится здесь.", color = TextSecondary)
    Spacer(Modifier.height(24.dp)); Button(onClick = add, shape = BubbleShape) { Text("Добавить собеседника") }
  }
}
@Composable private fun Avatar(name: String, size: Int = 48) {
  Box(Modifier.size(size.dp).clip(CircleShape).background(Brush.linearGradient(listOf(CyberPurple, CyberBlue.copy(alpha = .7f)))), contentAlignment = Alignment.Center) {
    Text(name.take(1).uppercase(), fontSize = (size / 2).sp, color = Color.White, fontWeight = FontWeight.SemiBold)
  }
}
@Composable private fun ContactRow(contact: Contact, last: ChatMessage?, calling: Boolean, click: () -> Unit) {
  Row(Modifier.fillMaxWidth().clip(BubbleShape).background(DarkSlate).clickable(onClick = click).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
    Avatar(contact.name)
    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
      Text(contact.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Spacer(Modifier.height(4.dp))
      Text(if (calling) "Голосовой звонок" else last?.text ?: "Начните разговор", maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextSecondary, fontSize = 13.sp)
    }
    if (calling) Icon(Icons.Default.Call, "Позвонить", tint = CyberBlue)
    else Column(horizontalAlignment = Alignment.End) { last?.let { Text(time(it.time), fontSize = 11.sp, color = TextSecondary) }; Icon(Icons.Default.Lock, null, tint = CyberBlue, modifier = Modifier.size(14.dp).padding(top = 3.dp)) }
  }
}
@Composable private fun Conversation(ui: UiState, contact: Contact, back: () -> Unit, safety: () -> Unit, call: () -> Unit, send: (String) -> Unit) {
  var draft by remember(contact.id) { mutableStateOf("") }
  var pendingDraft by remember { mutableStateOf<String?>(null) }
  val messages = ui.messages.filter { it.peer == contact.id }
  val scroll = rememberLazyListState()
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) scroll.animateScrollToItem(messages.lastIndex)
    if (pendingDraft != null && messages.lastOrNull()?.let { it.self && it.text == pendingDraft } == true) { draft = ""; pendingDraft = null }
  }
  Column(Modifier.fillMaxSize().imePadding()) {
    Row(Modifier.fillMaxWidth().background(DarkSlate).padding(vertical = 10.dp, horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
      Avatar(contact.name, 38)
      Column(Modifier.weight(1f).clickable(onClick = safety).padding(start = 12.dp)) {
        Text(contact.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Проверить ключ собеседника", color = CyberBlue, fontSize = 11.sp)
      }
      IconButton(onClick = call, enabled = !ui.busy) { Icon(Icons.Default.Call, "Позвонить", tint = CyberBlue) }
    }
    LazyColumn(state = scroll, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      items(messages, key = { it.id }) { message ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.self) Arrangement.End else Arrangement.Start) {
          Column(Modifier.widthIn(max = 290.dp).clip(BubbleShape).background(if (message.self) ChatBubbleSelf else ChatBubbleOther).padding(14.dp)) {
            Text(message.text, fontSize = 16.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(5.dp))
            Text(time(message.time) + if (message.self) "  " + when(message.status) { "queued" -> "В очереди"; "delivered" -> "Доставлено"; else -> "На сервере" } else "",
              color = if (message.status == "delivered") CyberBlue else TextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
          }
        }
      }
    }
    Row(Modifier.fillMaxWidth().background(DarkSlate).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(draft, { draft = it.take(6000) }, placeholder = { Text("Сообщение…") }, maxLines = 4, shape = BubbleShape, modifier = Modifier.weight(1f))
      IconButton(onClick = { pendingDraft = draft; send(draft) }, enabled = draft.isNotBlank() && !ui.busy, modifier = Modifier.padding(start = 6.dp)) { Icon(Icons.AutoMirrored.Filled.Send, "Отправить", tint = if (draft.isNotBlank()) CyberBlue else TextSecondary) }
    }
  }
}
@Composable private fun SettingsScreen(ui: UiState, invite: () -> Unit, add: () -> Unit, licenses: () -> Unit) {
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
    Text("Ваше пространство", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    Avatar(ui.name, 68); Text(ui.name, fontSize = 22.sp)
    Text("Сервер подключения", color = TextSecondary, fontSize = 12.sp); Text(ui.server)
    HorizontalDivider(color = GlassOverlay)
    Text("Ключи и история зашифрованы на устройстве. Смена ключа собеседника блокирует установление новой сессии.", color = TextSecondary)
    OutlinedButton(onClick = invite, modifier = Modifier.fillMaxWidth()) { Text("Моё приглашение") }
    OutlinedButton(onClick = add, modifier = Modifier.fillMaxWidth()) { Text("Добавить собеседника") }
    Text("Первая тестовая версия", color = GoldAccent, fontWeight = FontWeight.SemiBold)
    Text("Входящие доступны, пока приложение открыто. При уходе в фон звонок завершается. Не удаляйте приложение: переноса ключей и восстановления истории пока нет.", color = TextSecondary, fontSize = 13.sp)
    TextButton(onClick = licenses) { Text("Исходники и лицензии") }
  }
}
@Composable private fun LicenseDialog(dismiss: () -> Unit) {
  val context = LocalContext.current
  var fullText by remember { mutableStateOf<String?>(null) }
  AlertDialog(onDismissRequest = dismiss, title = { Text("Исходники и лицензии") }, text = {
    Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      if (fullText != null) Text(fullText!!, fontSize = 11.sp) else {
        Text("Tonino Secure 0.1.0-test · AGPL-3.0-only. Тестовая сборка предоставляется без гарантий. libsignal и WebRTC сохраняют лицензии своих авторов.")
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Nefitra/NEXA-Messenger/tree/codex/tonino-secure-chat-calls"))) }) { Text("Открыть исходный код") }
        TextButton(onClick = { fullText = context.assets.open("LICENSE_AGPL_3.txt").bufferedReader().use { it.readText() } }) { Text("Полная лицензия AGPL") }
        TextButton(onClick = { fullText = context.assets.open("THIRD_PARTY_NOTICES.md").bufferedReader().use { it.readText() } + "\n\n" + context.assets.open("LICENSE_WEBRTC.txt").bufferedReader().use { it.readText() } }) { Text("Зависимости и уведомления") }
      }
    }
  }, confirmButton = { TextButton(onClick = { if (fullText != null) fullText = null else dismiss() }) { Text(if (fullText != null) "Назад" else "Закрыть") } })
}
@Composable private fun AddDialog(dismiss: () -> Unit, add: (String) -> Unit) {
  var text by remember { mutableStateOf("") }
  AlertDialog(onDismissRequest = dismiss, title = { Text("Добавить собеседника") }, text = {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Text("Вставьте приглашение, которое получили напрямую от собеседника. Затем отправьте ему своё приглашение.")
      OutlinedTextField(text, { text = it.take(6000) }, label = { Text("Приглашение Tonino") }, maxLines = 5)
    }
  }, confirmButton = { TextButton(onClick = { add(text) }, enabled = text.isNotBlank()) { Text("Добавить") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Отмена") } })
}
@Composable private fun InviteDialog(invitation: String, busy: Boolean, dismiss: () -> Unit) {
  val clipboard = LocalClipboardManager.current
  val context = LocalContext.current
  val qr = remember(invitation) {
    if (invitation.isEmpty()) null else runCatching {
      val matrix = MultiFormatWriter().encode(invitation, BarcodeFormat.QR_CODE, 600, 600)
      Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888).apply {
        val pixels = IntArray(600 * 600) { i -> if (matrix[i % 600, i / 600]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
        setPixels(pixels, 0, 600, 0, 0, 600, 600)
      }.asImageBitmap()
    }.getOrNull()
  }
  AlertDialog(onDismissRequest = dismiss, title = { Text("Ваше приглашение") }, text = {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
      if (busy || invitation.isEmpty()) CircularProgressIndicator() else qr?.let { Image(it, "QR приглашения", Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) }
      Text("Приглашение одноразовое и действует 24 часа. Отправьте его лично собеседнику. В этой версии приглашение добавляют через вставку текста.", color = TextSecondary, fontSize = 13.sp)
      if (invitation.isNotEmpty()) OutlinedButton(onClick = {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, invitation) }, "Поделиться приглашением"))
      }) { Text("Поделиться") }
    }
  }, confirmButton = { TextButton(onClick = { clipboard.setText(AnnotatedString(invitation)) }, enabled = invitation.isNotEmpty()) { Text("Копировать") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Закрыть") } })
}
@Composable private fun CallDialog(call: VoiceState, name: String, accept: () -> Unit, end: () -> Unit, mute: () -> Unit, speaker: () -> Unit) {
  Dialog(onDismissRequest = {}) {
    Surface(shape = RoundedCornerShape(30.dp), color = DarkSlate) {
      Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Icon(Icons.Default.Lock, null, tint = CyberBlue, modifier = Modifier.size(22.dp))
        Avatar(name, 94); Text(name, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        Text(when(call.phase) { "incoming" -> "Входящий звонок"; "outgoing" -> "Вызываем…"; "active" -> "Защищённое соединение"; else -> "Соединяем…" }, color = CyberBlue)
        if (call.phase != "incoming") Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
          IconButton(onClick = mute) { Icon(if (call.muted) Icons.Default.MicOff else Icons.Default.Mic, "Микрофон", tint = if (call.muted) GoldAccent else TextPrimary) }
          IconButton(onClick = speaker) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "Динамик", tint = if (call.speaker) CyberBlue else TextPrimary) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
          if (call.phase == "incoming") FilledIconButton(onClick = accept, modifier = Modifier.size(64.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = TokenGreen)) { Icon(Icons.Default.Call, "Ответить") }
          FilledIconButton(onClick = end, modifier = Modifier.size(64.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = ErrorRed)) { Icon(Icons.Default.CallEnd, "Завершить") }
        }
      }
    }
  }
}
