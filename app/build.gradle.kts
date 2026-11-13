plugins {
  id("com.android.application")
  kotlin("android")
  id("org.jetbrains.kotlin.plugin.compose")
}
android {
  namespace = "com.example"
  compileSdk = 36
  ndkVersion = "28.0.13004108"
  defaultConfig {
    applicationId = "com.tonino.secure"
    minSdk = 26
    targetSdk = 36
    versionCode = 100
    versionName = "0.1.0-test"
    ndk { abiFilters += (findProperty("toninoAbi")?.toString() ?: "arm64-v8a") }
  }
  buildFeatures { compose = true; buildConfig = true }
  compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21; isCoreLibraryDesugaringEnabled = true }
  packaging {
    resources.excludes += setOf("libsignal_jni*.dylib", "signal_jni*.dll", "libsignal_jni*.so", "META-INF/AL2.0", "META-INF/LGPL2.1")
    jniLibs { excludes += "**/libsignal_jni_testing.so"; useLegacyPackaging = true }
  }
  testOptions { unitTests.isIncludeAndroidResources = true }
  buildTypes {
    debug { applicationIdSuffix = ".test" }
    release { isMinifyEnabled = false }
  }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) } }
dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
  implementation(project(":core"))
  implementation("org.signal:libsignal-android:0.107.0")
  implementation("io.github.webrtc-sdk:android:150.7871.04")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.google.zxing:core:3.5.3")
  implementation(platform("androidx.compose:compose-bom:2024.09.00"))
  implementation("androidx.activity:activity-compose:1.10.1")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.compose.ui:ui-tooling-preview")
  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.robolectric:robolectric:4.16.1")
  testImplementation("androidx.compose.ui:ui-test-junit4")
}
