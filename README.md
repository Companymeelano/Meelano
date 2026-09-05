## نسخه ۶٫۰ — موتور اتصال

### پشتیبانی از ترنسپورت‌های رایج
پیش از این تنها `tcp` و `ws` واقعاً برقرار می‌شدند و کانفیگ‌های gRPC بی‌صدا شکست می‌خوردند.

| ترنسپورت | وضعیت |
|---|---|
| `tcp` | پشتیبانی کامل |
| `ws` | پشتیبانی کامل (RFC 6455) |
| `httpupgrade` | **جدید** — دست‌دادن Upgrade بدون فریم‌بندی |
| `grpc` / `gun` | **جدید** — کلاینت HTTP/2 داخلی |
| `quic` / `kcp` / `xhttp` | پشتیبانی نمی‌شود و از فهرست حذف می‌شود |

`Http2Stream.kt` یک کلاینت HTTP/2 کمینه است: preface، تبادل SETTINGS، پاسخ به PING،
هدرهای HPACK، فریم‌بندی DATA با پیشوند ۵ بایتی gRPC و کنترل جریان WINDOW_UPDATE.
ALPN اکنون بر پایهٔ ترنسپورت انتخاب می‌شود (`h2` برای gRPC، `http/1.1` برای ws).

### تحلیل دقیق‌تر کانفیگ‌ها
userinfo متنی و درصدی در SIP002، آدرس‌های IPv6، گزینه‌های `v2ray-plugin`،
پرچم‌های عددی TLS در VMess، و `serviceName` که در `path` می‌آید.

### فیلتر سخت‌گیرانه سرورهای رایگان
پینگ TCP فقط نشان می‌داد چیزی به پورت گوش می‌دهد — نه اینکه واقعاً کار می‌کند.
اکنون بازآوری چهار مرحله دارد:

1. **فیلتر ساختاری** — حذف گره‌هایی که این نسخه نمی‌تواند حمل کند.
2. **جاروی دسترسی** — پینگ TCP سریع برای کوچک کردن میدان.
3. **اعتبارسنجی واقعی** — `NodeValidator` دست‌دادن کامل پروتکل را اجرا می‌کند و یک
   درخواست HTTP زنده را از داخل تونل عبور می‌دهد. تنها گره‌هایی که واقعاً ترافیک
   منتقل کردند باقی می‌مانند.
4. **تنوع** — حداکثر سه گره از یک میزبان.

منابع به فیدهای تفکیک‌شده بر اساس پروتکل و آینه‌های jsDelivr تغییر کرد.

### پایش سلامت
`HealthMonitor` نسبت اتصال‌های ناموفق و شمارندهٔ بایت را می‌سنجد. اگر تونل «متصل»
بماند ولی داده‌ای عبور ندهد، به‌جای نمایش نشان سبز گمراه‌کننده، افت کیفیت گزارش می‌شود.

# MeeLano Tunnel

<div dir="rtl">

**تونل امن اندروید با معماری بومی VpnService، مسیریابی هوشمند دورزدن ایران، و رابط کاربری کاملاً فارسی.**

نسخهٔ کامل و قابل نصب — بدون ظاهر نمایشی. تمام اعداد روی داشبورد از اندازه‌گیری واقعی می‌آیند.

</div>

---

## What actually works (no mock data)

| Capability | Implementation |
|---|---|
| **Real tunnel** | `MeelanoVpnService` establishes a genuine TUN interface, reads/writes packets, and reports honest state (`FAILED` when a node is dead — never a fake "connected"). |
| **Userspace TCP stack** | `TcpStack` / `TcpConnection` terminate every app socket in userspace, synthesise SYN-ACK/ACK/FIN/RST with correct IPv4+TCP checksums, and pump the payload through a real proxy tunnel. This is what makes HTTPS sites actually load. |
| **Real protocol outbounds** | Full wire implementations of **VLESS**, **Trojan**, **Shadowsocks AEAD** (`aes-128/256-gcm`, `chacha20-ietf-poly1305`), **VMess** (AEAD request header) and **SOCKS5**, over **TCP / TLS / WebSocket** transports. |
| **Real handshake** | `TunnelEngine` performs the node's genuine protocol handshake and then proxies a live HTTP probe: a node that accepts TCP but cannot relay payload is rejected instead of showing a fake "connected". |
| **Domain-aware dialling** | `DnsRelay` learns A records and `DnsMap` maps destination IPs back to hostnames, so flows are dialled by **domain**. The exit node resolves them itself, which defeats DNS poisoning and lands on healthy CDN edges. |
| **Config parsing** | `ConfigParser` parses `vmess://`, `vless://` (incl. Reality), `trojan://`, `ss://`, `hy2://`, `socks://` and whole base64 subscription blobs. |
| **Real latency** | `PingTester` does concurrent TCP handshakes. Unreachable nodes report "قطع" and sink to the bottom of the list. |
| **Real routing** | `RouteTable` computes `0.0.0.0/0` minus ~330 Iranian + private CIDR blocks and installs the resulting route set on the builder, so domestic traffic genuinely bypasses the tunnel at kernel level. |
| **DNS relay** | `DnsRelay` resolves queries taken from the TUN device via protected sockets, writes back checksum-correct IPv4/UDP replies, and NXDOMAINs known ad/tracker domains. |
| **UDP NAT** | `UdpNat` keeps per-flow protected `DatagramSocket`s so QUIC / games / plain UDP keep working under the tunnel. |
| **Real traffic stats** | `TrafficCounter` sums bytes crossing the TUN fd, cross-checked against `TrafficStats` per-uid counters. Speeds are derived from wall-clock deltas. |
| **Split tunnelling** | Reads the device's actually-installed launchable apps and feeds them to `addDisallowedApplication`. |
| **Kill switch** | Puts the TUN interface in blocking mode so nothing leaks while the tunnel is down. |
| **Smart failover** | On failure, walks the latency-ranked node list and genuinely **re-dials** each one until traffic flows, never repeating a node that already failed this session. |
| **Server management** | Delete any server (VIP, free or imported) with confirmation; deletions persist so a refresh never resurrects them. Plus one-tap "sweep unreachable" and "restore defaults". |
| **Subscriptions** | Downloads real GitHub subscription URLs, parses, dedupes, pings in parallel and keeps only the fastest reachable nodes; results are cached to disk. |
| **Scannable QR** | ZXing-generated QR of the actual share link — importable by v2rayNG / V2Box / Hiddify. |
| **Security** | Salted SHA-256 (5 000 rounds) PIN with brute-force lockout, plus the platform `BiometricPrompt` (unlock only on the OS success callback). |
| **Persistence** | Every setting, favourite, custom node and the selected server survive restart via DataStore. |

## Design

A dark "aurora" canvas with drifting blooms, a procedural hex grid and a star field; frosted-glass cards with gradient hairline borders; a layered energy-orb power button whose ring speed and glow follow the real tunnel state; a live packet-flow radar; and a smoothed throughput spline. **Six** selectable accent themes (نئون / زمرد / بنفش / طلایی / غروب / شفق قطبی) and full RTL layout.

The entire palette is sampled from the launcher icon — the `#0E102A`/`#212750` indigo canvas,
the `#40526F` chrome bevel and the cyan/violet neon of the "M" — and shipped as the default
**امضای میلانو** accent, so the app shell and its icon read as one object. The launch window
background matches too, removing the startup colour flash.

The home screen is deliberately sparse: a brand lockup with a single grouped utility capsule
(lock + sound), the connect orb, live speeds, and one tabbed panel. Controls are grouped by
intent — **مسیریابی** (where traffic goes), **حفاظت** (what protects you) and **مدیریت**
(servers, configs, diagnostics) — and each destination appears exactly once.

`ConnectOrb` is the hero control: expanding shock rings, a counter-rotating dashed containment
ring, a sweeping radar arc that accelerates while connecting, orbiting energy particles, and a
glass dome with a chrome bevel and specular highlight echoing the icon. Every property is
driven by the real tunnel state. `ServerScanOverlay` turns node testing into a full-screen
radar sweep whose blips light up as the beam passes them.

The launcher icon is a ray-traced 3D obsidian-glass shield holding a neon "M", shipped as a proper **adaptive icon** (separate background/foreground layers so it animates and masks correctly on every launcher shape), with legacy square/round bitmaps for pre-Oreo devices and a **themed monochrome** layer for Android 13+ wallpaper tinting.

`PremiumUi.kt` adds the shared polish: accent badges, fading dividers, shimmer placeholders, glow progress bars, a live equaliser and a sweep-gradient circular gauge.

## Build

Requires **JDK 17**. Everything else (Gradle 9.3.1, the wrapper jar) is fetched automatically.

```bash
./gradlew testDebugUnitTest     # unit tests
./gradlew assembleDebug         # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease       # -> app/build/outputs/apk/release/app-release.apk
```

`assembleRelease` signs with your upload keystore when `KEYSTORE_PATH`, `STORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD` are set; otherwise it falls back to the debug identity so the
output is still installable.

### Install on a phone

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or let CI do it: every push runs `.github/workflows/android-build.yml`, which builds and
uploads `meelano-tunnel-debug` / `meelano-tunnel-release` APK artifacts you can download
from the Actions tab and install directly.

## Tests

```
ConfigParserTest    – all five share-link formats + subscription blobs + garbage rejection
RouteTableTest      – Iranian IPs excluded, foreign IPs tunnelled, CIDR complement maths
IpPacketTest        – UDP parsing and checksum-correct response construction
GeoLabelerTest      – flag/city/ccTLD country detection
SecurityManagerTest – PIN hashing, wrong-PIN handling, rotation, lockout
```

## Notes

* The VIP fleet ships 24 real VLESS-over-WebSocket/TLS nodes across the UK, US, NL and DE.
  Each location appears twice: a CDN-fronted entry (most resilient to filtering) and a
  **پلاس** entry that dials the origin directly with a fronted SNI/Host pair (usually faster).
  These nodes are **white-labelled** — `BundledServersTest` asserts that no provider hostname,
  IP or identifier can appear in a server name, host label, live stat, tunnel log or QR sheet.
* Protocol encapsulation is implemented natively in Kotlin — no `libcore.aar` / Xray binary
  is bundled or required. The trade-offs of that choice:
  * **Hysteria 2 is not supported** (it needs a QUIC stack). Such nodes are filtered out of
    the free list and rejected with a clear message rather than failing silently.
  * VLESS **Reality** connects over TLS with certificate validation relaxed, but does not
    perform Reality's X25519 authentication handshake, so a strict Reality server may refuse it.
    Plain TLS and WebSocket VLESS/VMess/Trojan are fully conformant.
  * The TCP stack implements the fast path (immediate SYN-ACK, cumulative ACK, graceful FIN,
    RST on failure). It does not implement congestion control or selective ACK — the proxied
    TCP socket underneath provides those end to end.

---

**MEELANO STUDIO DESIGN** · Designed by Milad Yaghoobi
