package com.example

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import com.google.gson.JsonParser
import com.tonino.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal data class UpdateRelease(val code: Long, val name: String, val size: Long, val sha256: String, val url: String) {
  companion object {
    fun parse(text: String, packageName: String): UpdateRelease {
      require(text.length <= 16384)
      val j = JsonParser.parseString(text).asJsonObject
      require(j.text("packageId") == packageName)
      val r = UpdateRelease(j.number("versionCode"), j.text("versionName"), j.number("size"), j.text("sha256"), j.text("url"))
      require(r.code > 0 && r.name.length in 1..60 && r.size in 1..209715200)
      require(r.sha256.matches(Regex("[a-f0-9]{64}")))
      require(r.url == "https://dressmenow.eu/tonino-preview.apk")
      return r
    }
  }
}

data class UpdateState(val phase: String = "idle", val message: String = "Версия ${BuildConfig.VERSION_NAME}", val progress: Int = 0)

/** A same-package, same-signer update. Android always owns the final installation confirmation. */
class AppUpdater(app: Application) : AndroidViewModel(app) {
  private val worker = Executors.newSingleThreadExecutor()
  private val state = MutableStateFlow(UpdateState())
  val ui = state.asStateFlow()
  private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
    .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).callTimeout(4, TimeUnit.MINUTES).build()
  private val folder = File(app.cacheDir, "updates").apply { mkdirs() }
  private val apk = File(folder, "Tonino-update.apk")
  @Volatile private var autoInstall = false
  private var waitingPermission = false
  private var release: UpdateRelease? = null

  fun downloadLatest() {
    if (state.value.phase in setOf("checking", "downloading")) return
    state.value = UpdateState("checking", "Проверяем обновление…")
    worker.execute {
      val partial = File(folder, "incoming.apk")
      try {
        val metadata = client.newCall(Request.Builder().url("https://dressmenow.eu/tonino-update.json").build()).execute().use { response ->
          check(response.isSuccessful)
          val body = checkNotNull(response.body)
          body.byteStream().use { input ->
            val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(4096)
            while (true) { val n=input.read(buffer); if(n<0) break; check(out.size()+n<=16384); out.write(buffer,0,n) }
            out.toString(Charsets.UTF_8.name())
          }
        }
        val candidate = UpdateRelease.parse(metadata, getApplication<Application>().packageName)
        if (candidate.code <= BuildConfig.VERSION_CODE) {
          state.value = UpdateState("idle", "Установлена актуальная версия ${BuildConfig.VERSION_NAME}")
        } else {
          state.value = UpdateState("downloading", "Скачиваем ${candidate.name}…")
          val digest = MessageDigest.getInstance("SHA-256")
          client.newCall(Request.Builder().url(candidate.url).build()).execute().use { response ->
            check(response.isSuccessful)
            val body = checkNotNull(response.body)
            body.byteStream().use { input -> partial.outputStream().use { output ->
              val buffer=ByteArray(65536); var total=0L; var progress=-1
              while(true) {
                val n=input.read(buffer);if(n<0)break
                total+=n;check(total<=candidate.size);digest.update(buffer,0,n);output.write(buffer,0,n)
                val next=(total*100/candidate.size).toInt()
                if(next!=progress){progress=next;state.value=UpdateState("downloading","Скачиваем ${candidate.name}…",next)}
              }
              check(total==candidate.size)
              output.fd.sync()
            } }
          }
          check(digest.digest().joinToString("") { "%02x".format(it) } == candidate.sha256)
          verifyArchive(partial, candidate)
          check(!apk.exists() || apk.delete());check(partial.renameTo(apk))
          release=candidate;autoInstall=true
          state.value=UpdateState("ready","Обновление проверено. Открываем установку…",100)
        }
      } catch (_: Exception) {
        partial.delete();autoInstall=false
        state.value=UpdateState("idle","Не удалось проверить или скачать обновление. Проверьте сеть и повторите.")
      }
    }
  }
  @Suppress("DEPRECATION") private fun verifyArchive(file: File, expected: UpdateRelease) {
    val app=getApplication<Application>();val pm=app.packageManager
    val flags=if(Build.VERSION.SDK_INT>=28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    val installed=pm.getPackageInfo(app.packageName,flags)
    val archive=checkNotNull(pm.getPackageArchiveInfo(file.absolutePath,flags))
    check(archive.packageName==app.packageName)
    val version=if(Build.VERSION.SDK_INT>=28) archive.longVersionCode else archive.versionCode.toLong()
    check(version==expected.code && version>BuildConfig.VERSION_CODE)
    val before=if(Build.VERSION.SDK_INT>=28) installed.signingInfo?.apkContentsSigners else installed.signatures
    val after=if(Build.VERSION.SDK_INT>=28) archive.signingInfo?.apkContentsSigners else archive.signatures
    check(!before.isNullOrEmpty() && !after.isNullOrEmpty())
    check(before.map{it.toCharsString()}.toSet()==after.map{it.toCharsString()}.toSet())
  }
  fun requestInstall(activity: Activity) { autoInstall=true;maybeInstall(activity) }
  fun resumed(activity: Activity) {
    if(waitingPermission && activity.packageManager.canRequestPackageInstalls()) {waitingPermission=false;autoInstall=true}
    maybeInstall(activity)
  }
  fun maybeInstall(activity: Activity) {
    if(!autoInstall || state.value.phase!="ready" || (activity as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.currentState!=Lifecycle.State.RESUMED) return
    try {
      if(!activity.packageManager.canRequestPackageInstalls()) {
        autoInstall=false;waitingPermission=true
        state.value=state.value.copy(message="Разрешите Tonino устанавливать обновления и вернитесь в приложение.")
        activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:${activity.packageName}")))
      } else {
        val expected=checkNotNull(release);verifyArchive(apk,expected)
        autoInstall=false
        val uri=FileProvider.getUriForFile(activity,"${activity.packageName}.updates",apk)
        activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
          .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        state.value=state.value.copy(message="Подтвердите «Обновить» в системном окне Android.")
      }
    } catch (_: Exception) {
      autoInstall=false;state.value=state.value.copy(message="Не удалось открыть установку. Нажмите «Установить» ещё раз.")
    }
  }
  override fun onCleared() { client.dispatcher.cancelAll();worker.shutdownNow() }
}
