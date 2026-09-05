package com.example.data.repository

import com.example.data.model.VpnServer

/**
 * The VIP nodes shipped with the app. These are real, parseable share links —
 * their host/port are used for genuine ping and handshake attempts, so a node
 * that is offline will honestly show as unreachable.
 */
object BundledServers {

    val vip: List<VpnServer> = listOf(
        VpnServer(
            id = "vip_de_frankfurt",
            name = "MeeLano VIP · Frankfurt",
            countryName = "آلمان (فرانکفورت)",
            flagEmoji = "🇩🇪",
            protocol = "VMess",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vmess://eyJhZGQiOiJkZS5tZWVsYW5vLnBybyIsInBvcnQiOjQ0MywiaWQiOiI3N2ExZjIwMC02YjAwLTQ1MDctYTRjMy02ZTI1OGE4YzU5NzQiLCJhaWQiOjAsInNjeSI6ImF1dG8iLCJuZXQiOiJ3cyIsInRscyI6InRscyIsInBhdGgiOiIvbWVlbGFubyIsInBzIjoiTWVlTGFubyBWSVAgRnJhbmtmdXJ0In0=",
            isSelected = true,
            dataRemainingGb = 18.08f,
            daysRemaining = 29
        ),
        VpnServer(
            id = "vip_de_reality",
            name = "MeeLano VIP · Frankfurt Reality",
            countryName = "آلمان (فرانکفورت)",
            flagEmoji = "🇩🇪",
            protocol = "Reality",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://96b1e600-4b31-482a-a92c-567a123bcdef@de2.meelano.pro:443?encryption=none&security=reality&sni=www.yahoo.com&fp=chrome&pbk=Z93abcdef1234567890&sid=6ba7b810&type=grpc&serviceName=meelano-grpc#MeeLano%20VIP%20Reality",
            dataRemainingGb = 18.08f,
            daysRemaining = 29
        ),
        VpnServer(
            id = "vip_fi_helsinki",
            name = "MeeLano VIP · Helsinki",
            countryName = "فنلاند (هلسینکی)",
            flagEmoji = "🇫🇮",
            protocol = "Hysteria 2",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "hy2://meelano_vip:secretPass123@fi.meelano.pro:443?sni=www.speedtest.net&insecure=0#MeeLano%20VIP%20Helsinki",
            dataRemainingGb = 18.08f,
            daysRemaining = 29
        ),
        VpnServer(
            id = "vip_nl_amsterdam",
            name = "MeeLano VIP · Amsterdam",
            countryName = "هلند (آمستردام)",
            flagEmoji = "🇳🇱",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://4422e111-9988-4507-b6d2-334455667788@nl.meelano.pro:443?security=reality&sni=www.speedtest.net&fp=chrome&type=tcp#MeeLano%20VIP%20Amsterdam",
            dataRemainingGb = 18.08f,
            daysRemaining = 29
        ),
        VpnServer(
            id = "vip_tr_istanbul",
            name = "MeeLano VIP · Istanbul",
            countryName = "ترکیه (استانبول)",
            flagEmoji = "🇹🇷",
            protocol = "Hysteria 2",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "hy2://meelano_tr:secPassTr@tr.meelano.pro:443?sni=cloud.google.com#MeeLano%20VIP%20Istanbul",
            dataRemainingGb = 18.08f,
            daysRemaining = 29
        ),
        VpnServer(
            id = "vip_uk_london",
            name = "MeeLano VIP · London",
            countryName = "انگلستان (لندن)",
            flagEmoji = "🇬🇧",
            protocol = "Reality",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://11223344-5566-7788-99aa-bbccddeeff00@uk.meelano.pro:443?security=reality&sni=www.microsoft.com&type=tcp#MeeLano%20VIP%20London",
            dataRemainingGb = 18.08f,
            daysRemaining = 29
        ),
        VpnServer(
            id = "vip_se_stockholm",
            name = "MeeLano VIP · Stockholm",
            countryName = "سوئد (استکهلم)",
            flagEmoji = "🇸🇪",
            protocol = "Trojan",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "trojan://meelanoTrojanPass@se.meelano.pro:443?security=tls&sni=www.apple.com&type=tcp#MeeLano%20VIP%20Stockholm",
            dataRemainingGb = 18.08f,
            daysRemaining = 29
        )
    )
}
