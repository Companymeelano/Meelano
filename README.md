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
| **Real handshake** | `TunnelEngine` opens a TCP + TLS 1.2/1.3 connection to the selected node with the config's SNI, and reports the *negotiated* cipher suite. |
| **Config parsing** | `ConfigParser` parses `vmess://`, `vless://` (incl. Reality), `trojan://`, `ss://`, `hy2://`, `socks://` and whole base64 subscription blobs. |
| **Real latency** | `PingTester` does concurrent TCP handshakes. Unreachable nodes report "قطع" and sink to the bottom of the list. |
| **Real routing** | `RouteTable` computes `0.0.0.0/0` minus ~330 Iranian + private CIDR blocks and installs the resulting route set on the builder, so domestic traffic genuinely bypasses the tunnel at kernel level. |
| **DNS relay** | `DnsRelay` resolves queries taken from the TUN device via protected sockets, writes back checksum-correct IPv4/UDP replies, and NXDOMAINs known ad/tracker domains. |
| **UDP NAT** | `UdpNat` keeps per-flow protected `DatagramSocket`s so QUIC / games / plain UDP keep working under the tunnel. |
| **Real traffic stats** | `TrafficCounter` sums bytes crossing the TUN fd, cross-checked against `TrafficStats` per-uid counters. Speeds are derived from wall-clock deltas. |
| **Split tunnelling** | Reads the device's actually-installed launchable apps and feeds them to `addDisallowedApplication`. |
| **Kill switch** | Puts the TUN interface in blocking mode so nothing leaks while the tunnel is down. |
| **Smart failover** | On failure, TCP-pings every known node and switches to the fastest live one. |
| **Subscriptions** | Downloads real GitHub subscription URLs, parses, dedupes, pings in parallel and keeps only the fastest reachable nodes; results are cached to disk. |
| **Scannable QR** | ZXing-generated QR of the actual share link — importable by v2rayNG / V2Box / Hiddify. |
| **Security** | Salted SHA-256 (5 000 rounds) PIN with brute-force lockout, plus the platform `BiometricPrompt` (unlock only on the OS success callback). |
| **Persistence** | Every setting, favourite, custom node and the selected server survive restart via DataStore. |

## Design

A dark "aurora" canvas with drifting blooms, a procedural hex grid and a star field; frosted-glass cards with gradient hairline borders; a layered energy-orb power button whose ring speed and glow follow the real tunnel state; a live packet-flow radar; and a smoothed throughput spline. Four selectable accent themes (نئون / زمرد / بنفش / طلایی) and full RTL layout.

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

* The VIP nodes bundled in `BundledServers` are example endpoints. Replace `configLink`
  with your own subscription/servers — the app pings and connects to whatever is really there.
* The tunnel carries traffic through the OS route set and a userspace DNS/UDP path. Adding a
  full Xray/sing-box core (`libcore.aar`) is the natural next step for protocol-level
  encapsulation; every seam for it already exists in `TunnelEngine`.

---

**MEELANO STUDIO DESIGN** · Designed by Milad Yaghoobi
