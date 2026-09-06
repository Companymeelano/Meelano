import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

/**
 * MeeLano Tunnel for Windows.
 *
 * Compose for Desktop, so the visual language — the aurora ground, the glass
 * cards, the neon wordmark, the orb — is the same code as the phone build
 * rather than a reimplementation that would drift apart.
 *
 * The protocol engine is genuinely shared too: every outbound under
 * `vpn/proto/` is plain JVM (sockets, JSSE, no Android imports), so this build
 * compiles the real VLESS/VMess/Trojan/Shadowsocks/SOCKS5 implementations
 * straight from the app source tree. See the source set wiring below for
 * exactly which files are pulled in and why the list is explicit.
 */

kotlin {
    jvmToolchain(17)
}

sourceSets {
    main {
        kotlin {
            // Share the engine with the Android app rather than forking it.
            //
            // Only Android-free files are listed. ConfigParser, for instance, is
            // deliberately excluded because it parses links with android.net.Uri;
            // the desktop build has its own parser over java.net.URI that
            // produces the same ProxyEndpoint. Keeping this list explicit means
            // an Android import accidentally added upstream breaks the Windows
            // build loudly at compile time instead of silently at runtime.
            val shared = "../app/src/main/java/com/example"
            // The endpoint model and protocol enum: pure data, no imports.
            srcDir("$shared/core/portable")
            // Every outbound: VLESS, VMess, Trojan, Shadowsocks, SOCKS5, plus
            // the TLS/WebSocket transports. All plain JVM sockets and JSSE.
            srcDir("$shared/vpn/proto")
            // Deliberately NOT vpn/stack: that is the userspace TCP/IP stack
            // that reassembles packets from Android's TUN device. Windows has
            // no TUN here — the desktop build proxies already-formed TCP
            // streams — so including it would only drag in vpn/net and its
            // Android dependencies for code that would never run.
        }
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.kotlinx.coroutines.core)
    // Swing interop is used for the tray icon and the native file chooser.
    implementation(compose.desktop.common)

    testImplementation(libs.junit)
}

compose.desktop {
    application {
        mainClass = "com.example.desktop.MainKt"

        nativeDistributions {
            // MSI is the installer Windows users expect: Add/Remove Programs,
            // a Start-menu entry and a clean uninstall. EXE here would be a
            // bare launcher with none of that.
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)

            packageName = "MeeLano Tunnel"
            packageVersion = "19.0.0"
            description = "MeeLano Tunnel — secure, fast, beautiful"
            vendor = "MeeLano Studio"
            copyright = "MeeLano Studio"

            // Trim the bundled runtime to the modules actually used; the whole
            // JDK would add hundreds of megabytes to the installer.
            includeAllModules = false
            modules(
                "java.base",
                "java.desktop",
                "java.logging",
                "java.naming",
                "java.net.http",
                "java.prefs",
                "java.sql",
                "jdk.crypto.ec",
                "jdk.unsupported"
            )

            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                // Stable UUID: an installer whose upgrade code changes between
                // releases installs alongside the old version instead of
                // replacing it, leaving users with two copies.
                upgradeUuid = "5D3B4E2A-91C7-4F86-A0D2-7E1B6C48F390"
                menuGroup = "MeeLano"
                perUserInstall = true
                dirChooser = true
                shortcut = true
            }
        }

        buildTypes.release.proguard {
            // The engine reaches for classes reflectively in places; shrinking
            // here buys little and risks a runtime failure in the tunnel.
            isEnabled.set(false)
        }
    }
}
