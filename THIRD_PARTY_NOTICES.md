# Third-party components

This experimental app does not connect to Signal or Telegram infrastructure.

| Component | Version / license | Upstream source |
|---|---|---|
| Signal libsignal Java/Android | 0.107.0 / AGPL-3.0-only | https://github.com/signalapp/libsignal/tree/v0.107.0 |
| WebRTC Android prebuilt distribution | 150.7871.04 / BSD-style WebRTC and bundled third-party licenses | https://github.com/webrtc-sdk/android |
| WebRTC engine | WebRTC BSD license and third-party notices | https://webrtc.googlesource.com/src/+/refs/heads/main/LICENSE |
| Kotlin | 2.2.10 / Apache-2.0 | https://github.com/JetBrains/kotlin |
| AndroidX / Jetpack Compose | Versions pinned in app/build.gradle.kts / Apache-2.0 | https://android.googlesource.com/platform/frameworks/support/ |
| OkHttp | 4.12.0 / Apache-2.0 | https://github.com/square/okhttp/tree/parent-4.12.0 |
| Gson | 2.13.2 / Apache-2.0 | https://github.com/google/gson |
| ZXing core (QR generation) | 3.5.3 / Apache-2.0 | https://github.com/zxing/zxing |
| Caddy (separate server container) | 2.x / Apache-2.0 | https://github.com/caddyserver/caddy |
| coturn (separate server container) | 4.18.0-r0 / BSD-3-Clause | https://github.com/coturn/coturn |
| Node.js (separate server container) | 24.x / MIT and bundled notices | https://github.com/nodejs/node |

libsignal's complete AGPL license is included in LICENSE. Obtain the corresponding
source for this app from the same repository branch/commit as the APK. Dependency
sources and notices are available at the upstream links and in their published
artifacts. The packages remain subject to their original copyright and license
notices. WebRTC's prebuilt native engine also includes Chromium third-party code;
consult its upstream third-party notices when redistributing a production build.
