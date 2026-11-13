plugins { kotlin("jvm"); `java-library` }
kotlin { jvmToolchain(21) }
dependencies {
  api("org.signal:libsignal-client:0.107.0")
  api("com.google.code.gson:gson:2.13.2")
  testImplementation("junit:junit:4.13.2")
}
tasks.test { testLogging { events("passed", "failed", "skipped") } }
