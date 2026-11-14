package com.example

import com.tonino.core.json
import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
  private fun metadata() = json("packageId" to "com.tonino.secure.preview", "versionCode" to 103,
    "versionName" to "0.1.3-preview", "size" to 54254226, "sha256" to "a".repeat(64),
    "url" to "https://dressmenow.eu/tonino-preview.apk")
  @Test fun anotherPackageOrDownloadOriginIsRejected() {
    for ((key,value) in listOf("packageId" to "com.attacker.app", "url" to "https://attacker.example/app.apk",
      "url" to "http://dressmenow.eu/tonino-preview.apk")) {
      val data=metadata().apply{addProperty(key,value)}
      assertThrows(IllegalArgumentException::class.java){UpdateRelease.parse(data.toString(),"com.tonino.secure.preview")}
    }
  }
  @Test fun unboundedOrInvalidDownloadsAreRejected() {
    val oversized=metadata().apply{addProperty("size",Long.MAX_VALUE)}
    val missingDigest=metadata().apply{remove("sha256")}
    for(data in listOf(oversized,missingDigest)) assertThrows(IllegalArgumentException::class.java){
      UpdateRelease.parse(data.toString(),"com.tonino.secure.preview")
    }
  }
}
