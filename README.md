<div align="center">

# TG WS Proxy — Android

**A local SOCKS5 proxy that tunnels Telegram traffic over WebSocket (TLS)**

[![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Based on](https://img.shields.io/badge/based%20on-Flowseal%2Ftg--ws--proxy-orange)](https://github.com/Flowseal/tg-ws-proxy)

</div>

---

## What it does

TG WS Proxy runs a local **SOCKS5 proxy on `127.0.0.1:1080`** that intercepts Telegram connections and wraps them in a **TLS WebSocket tunnel** — useful in environments where direct TCP to Telegram DCs is blocked.

```
Telegram app
    │  SOCKS5
    ▼
TG WS Proxy  ──── WSS (TLS) ────▶  Telegram DC
```

- Intercepts connections to Telegram IP addresses
- Extracts DC ID from the MTProto obfuscation init packet
- Opens a WebSocket (TLS) connection to the corresponding DC via Telegram domains
- If WebSocket is unavailable (302 redirect) — automatically falls back to direct TCP

---

## Features

- One-tap start / stop with persistent foreground service
- Auto-start on device boot or app launch
- Configurable listen host & port
- DC IP override with built-in presets (Default / All Main / All Media)
- WS connection pool per DC (configurable size)
- TCP_NODELAY toggle
- Verbose logging + optional log-to-file
- Dark / Light / System theme
- English / Russian / System language
- Material 3 UI with Jetpack Compose

---

## Screenshots

> _Coming soon_

---

## Getting started

### Connect Telegram automatically

1. Tap **Start** on the Home screen
2. Tap **Connect in Telegram** — Telegram opens with the proxy pre-filled
3. Confirm when prompted

### Connect manually

In Telegram: **Settings → Data and Storage → Proxy → Add Proxy → SOCKS5**

| Field    | Value       |
|----------|-------------|
| Server   | `127.0.0.1` |
| Port     | `1080`      |
| Login    | _(empty)_   |
| Password | _(empty)_   |

---

## Build

```bash
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/TGWSProxy-v1.0.0-debug.apk
```

Requirements: Android Studio Hedgehog+ / JDK 17 / Android SDK 35

---

## Tech stack

| Layer | Library |
|-------|---------|
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Settings | DataStore Preferences |
| Navigation | Navigation Compose |
| Logging | Timber |
| Serialization | kotlinx.serialization |

---

## Credits

This project is an Android port / UI wrapper around the original proxy logic by **Flowseal**.

> Original idea & proxy core: [github.com/Flowseal/tg-ws-proxy](https://github.com/Flowseal/tg-ws-proxy) — MIT License

---

## License

```
MIT License — see LICENSE file for details.
```
