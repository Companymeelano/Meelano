package com.example.data.repository

import com.example.data.model.VpnServer

/**
 * The VIP nodes shipped with the app.
 *
 * Every entry is a real, parseable VLESS-over-WebSocket/TLS share link, so ping,
 * handshake and throughput figures are measured against the live node — one that
 * is offline honestly reports as unreachable rather than faking a connection.
 *
 * Each location appears twice:
 *  * the plain entry routes through a CDN front, which is the most resilient to
 *    filtering;
 *  * the **پلاس** entry dials the origin directly with a fronted SNI/Host pair,
 *    which is usually faster whenever it is reachable.
 *
 * Naming is deliberately generic: users see only the destination country and the
 * MeeLano brand, never the upstream provider's hostnames or identifiers.
 */
object BundledServers {

    val vip: List<VpnServer> = listOf(
        VpnServer(
            id = "vip_uk_1_a",
            name = "MeeLano بریتانیا 1",
            countryName = "بریتانیا",
            flagEmoji = "🇬🇧",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@0sowk.hbsek.org:443?type=ws&security=tls&sni=0sowk.hbsek.org&host=0sowk.hbsek.org&path=%2Fvl%2Ft%2FtUtK%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A8%D8%B1%DB%8C%D8%AA%D8%A7%D9%86%DB%8C%D8%A7%201",
            isSelected = true,
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_uk_1_b",
            name = "MeeLano بریتانیا 1 پلاس",
            countryName = "بریتانیا",
            flagEmoji = "🇬🇧",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@178.239.157.239:443?security=tls&sni=cdn.asset.aparat.com&host=cdn.asset.aparat.com&pcs=8e823bc5dc2bfd715bda25eb3e345229a20c538339ccc07ccb28dd692b9a7185&type=ws&path=%2Fvl%2Ft%2FtUtK%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A8%D8%B1%DB%8C%D8%AA%D8%A7%D9%86%DB%8C%D8%A7%201%20%D9%BE%D9%84%D8%A7%D8%B3",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_uk_2_a",
            name = "MeeLano بریتانیا 2",
            countryName = "بریتانیا",
            flagEmoji = "🇬🇧",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@0sowk.hbsek.org:443?type=ws&security=tls&sni=0sowk.hbsek.org&host=0sowk.hbsek.org&path=%2Fvl%2Ft%2FuUuK%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A8%D8%B1%DB%8C%D8%AA%D8%A7%D9%86%DB%8C%D8%A7%202",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_uk_2_b",
            name = "MeeLano بریتانیا 2 پلاس",
            countryName = "بریتانیا",
            flagEmoji = "🇬🇧",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@178.239.157.239:443?security=tls&sni=asset.aparat.com&host=asset.aparat.com&pcs=0c0b948c66aff8553c1b4d9c21fc8cf43d0588a72135f85fc1a75e9c08376d3f&type=ws&path=%2Fvl%2Ft%2FuUuK%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A8%D8%B1%DB%8C%D8%AA%D8%A7%D9%86%DB%8C%D8%A7%202%20%D9%BE%D9%84%D8%A7%D8%B3",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_uk_3_a",
            name = "MeeLano بریتانیا 3",
            countryName = "بریتانیا",
            flagEmoji = "🇬🇧",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@0sowk.hbsek.org:443?type=ws&security=tls&sni=0sowk.hbsek.org&host=0sowk.hbsek.org&path=%2Fvl%2Ft%2FwUwK%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A8%D8%B1%DB%8C%D8%AA%D8%A7%D9%86%DB%8C%D8%A7%203",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_uk_3_b",
            name = "MeeLano بریتانیا 3 پلاس",
            countryName = "بریتانیا",
            flagEmoji = "🇬🇧",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@178.239.157.239:443?security=tls&sni=gateway.telewebion.ir&host=gateway.telewebion.ir&pcs=6c00814782e75231e31056aec28bf35ec1e757f2899114457db59f23709a302c&type=ws&path=%2Fvl%2Ft%2FwUwK%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A8%D8%B1%DB%8C%D8%AA%D8%A7%D9%86%DB%8C%D8%A7%203%20%D9%BE%D9%84%D8%A7%D8%B3",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_us_1_a",
            name = "MeeLano ایالات متحده 1",
            countryName = "ایالات متحده",
            flagEmoji = "🇺🇸",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@0sowk.hbsek.org:443?type=ws&security=tls&sni=0sowk.hbsek.org&host=0sowk.hbsek.org&path=%2Fvl%2Ft%2FtUtStA%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A7%DB%8C%D8%A7%D9%84%D8%A7%D8%AA%20%D9%85%D8%AA%D8%AD%D8%AF%D9%87%201",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_us_1_b",
            name = "MeeLano ایالات متحده 1 پلاس",
            countryName = "ایالات متحده",
            flagEmoji = "🇺🇸",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@178.239.157.239:443?security=tls&sni=static.telewebion.ir&host=static.telewebion.ir&pcs=7d65a6dcf82b36ee24aa7b724cf229806dde3e7c1e55b8b0d143141feb61895e&type=ws&path=%2Fvl%2Ft%2FtUtStA%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A7%DB%8C%D8%A7%D9%84%D8%A7%D8%AA%20%D9%85%D8%AA%D8%AD%D8%AF%D9%87%201%20%D9%BE%D9%84%D8%A7%D8%B3",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_us_2_a",
            name = "MeeLano ایالات متحده 2",
            countryName = "ایالات متحده",
            flagEmoji = "🇺🇸",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@0sowk.hbsek.org:443?type=ws&security=tls&sni=0sowk.hbsek.org&host=0sowk.hbsek.org&path=%2Fvl%2Ft%2FuUuSuA%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A7%DB%8C%D8%A7%D9%84%D8%A7%D8%AA%20%D9%85%D8%AA%D8%AD%D8%AF%D9%87%202",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_us_2_b",
            name = "MeeLano ایالات متحده 2 پلاس",
            countryName = "ایالات متحده",
            flagEmoji = "🇺🇸",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@37.32.44.59:443?security=tls&sni=cdn.asset.aparat.com&host=cdn.asset.aparat.com&pcs=8e823bc5dc2bfd715bda25eb3e345229a20c538339ccc07ccb28dd692b9a7185&type=ws&path=%2Fvl%2Ft%2FuUuSuA%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A7%DB%8C%D8%A7%D9%84%D8%A7%D8%AA%20%D9%85%D8%AA%D8%AD%D8%AF%D9%87%202%20%D9%BE%D9%84%D8%A7%D8%B3",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_us_3_a",
            name = "MeeLano ایالات متحده 3",
            countryName = "ایالات متحده",
            flagEmoji = "🇺🇸",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@0sowk.hbsek.org:443?type=ws&security=tls&sni=0sowk.hbsek.org&host=0sowk.hbsek.org&path=%2Fvl%2Ft%2FwUwSwA%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A7%DB%8C%D8%A7%D9%84%D8%A7%D8%AA%20%D9%85%D8%AA%D8%AD%D8%AF%D9%87%203",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_us_3_b",
            name = "MeeLano ایالات متحده 3 پلاس",
            countryName = "ایالات متحده",
            flagEmoji = "🇺🇸",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@178.239.157.239:443?security=tls&sni=asset.aparat.com&host=asset.aparat.com&pcs=0c0b948c66aff8553c1b4d9c21fc8cf43d0588a72135f85fc1a75e9c08376d3f&type=ws&path=%2Fvl%2Ft%2FwUwSwA%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A7%DB%8C%D8%A7%D9%84%D8%A7%D8%AA%20%D9%85%D8%AA%D8%AD%D8%AF%D9%87%203%20%D9%BE%D9%84%D8%A7%D8%B3",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_nl_1_a",
            name = "MeeLano هلند 1",
            countryName = "هلند",
            flagEmoji = "🇳🇱",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@0sowk.hbsek.org:443?type=ws&security=tls&sni=0sowk.hbsek.org&host=0sowk.hbsek.org&path=%2Fvl%2Ft%2FtNtL%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D9%87%D9%84%D9%86%D8%AF%201",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_nl_1_b",
            name = "MeeLano هلند 1 پلاس",
            countryName = "هلند",
            flagEmoji = "🇳🇱",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@37.32.44.59:443?security=tls&sni=gateway.telewebion.ir&host=gateway.telewebion.ir&pcs=6c00814782e75231e31056aec28bf35ec1e757f2899114457db59f23709a302c&type=ws&path=%2Fvl%2Ft%2FtNtL%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D9%87%D9%84%D9%86%D8%AF%201%20%D9%BE%D9%84%D8%A7%D8%B3",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_nl_2_a",
            name = "MeeLano هلند 2",
            countryName = "هلند",
            flagEmoji = "🇳🇱",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@0sowk.hbsek.org:443?type=ws&security=tls&sni=0sowk.hbsek.org&host=0sowk.hbsek.org&path=%2Fvl%2Ft%2FuNuL%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D9%87%D9%84%D9%86%D8%AF%202",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_nl_2_b",
            name = "MeeLano هلند 2 پلاس",
            countryName = "هلند",
            flagEmoji = "🇳🇱",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@37.32.44.59:443?security=tls&sni=static.telewebion.ir&host=static.telewebion.ir&pcs=7d65a6dcf82b36ee24aa7b724cf229806dde3e7c1e55b8b0d143141feb61895e&type=ws&path=%2Fvl%2Ft%2FuNuL%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D9%87%D9%84%D9%86%D8%AF%202%20%D9%BE%D9%84%D8%A7%D8%B3",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_nl_3_a",
            name = "MeeLano هلند 3",
            countryName = "هلند",
            flagEmoji = "🇳🇱",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@0sowk.hbsek.org:443?type=ws&security=tls&sni=0sowk.hbsek.org&host=0sowk.hbsek.org&path=%2Fvl%2Ft%2FwNwL%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D9%87%D9%84%D9%86%D8%AF%203",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_nl_3_b",
            name = "MeeLano هلند 3 پلاس",
            countryName = "هلند",
            flagEmoji = "🇳🇱",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@178.239.157.239:443?security=tls&sni=cdn.asset.aparat.com&host=cdn.asset.aparat.com&pcs=8e823bc5dc2bfd715bda25eb3e345229a20c538339ccc07ccb28dd692b9a7185&type=ws&path=%2Fvl%2Ft%2FwNwL%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D9%87%D9%84%D9%86%D8%AF%203%20%D9%BE%D9%84%D8%A7%D8%B3",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_de_1_a",
            name = "MeeLano آلمان 1",
            countryName = "آلمان",
            flagEmoji = "🇩🇪",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@0sowk.hbsek.org:443?type=ws&security=tls&sni=0sowk.hbsek.org&host=0sowk.hbsek.org&path=%2Fvl%2Ft%2FtDtE%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A2%D9%84%D9%85%D8%A7%D9%86%201",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_de_1_b",
            name = "MeeLano آلمان 1 پلاس",
            countryName = "آلمان",
            flagEmoji = "🇩🇪",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@37.32.44.59:443?security=tls&sni=asset.aparat.com&host=asset.aparat.com&pcs=0c0b948c66aff8553c1b4d9c21fc8cf43d0588a72135f85fc1a75e9c08376d3f&type=ws&path=%2Fvl%2Ft%2FtDtE%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A2%D9%84%D9%85%D8%A7%D9%86%201%20%D9%BE%D9%84%D8%A7%D8%B3",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_de_2_a",
            name = "MeeLano آلمان 2",
            countryName = "آلمان",
            flagEmoji = "🇩🇪",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@0sowk.hbsek.org:443?type=ws&security=tls&sni=0sowk.hbsek.org&host=0sowk.hbsek.org&path=%2Fvl%2Ft%2FuDuE%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A2%D9%84%D9%85%D8%A7%D9%86%202",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_de_2_b",
            name = "MeeLano آلمان 2 پلاس",
            countryName = "آلمان",
            flagEmoji = "🇩🇪",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@37.32.44.59:443?security=tls&sni=gateway.telewebion.ir&host=gateway.telewebion.ir&pcs=6c00814782e75231e31056aec28bf35ec1e757f2899114457db59f23709a302c&type=ws&path=%2Fvl%2Ft%2FuDuE%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A2%D9%84%D9%85%D8%A7%D9%86%202%20%D9%BE%D9%84%D8%A7%D8%B3",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_de_3_a",
            name = "MeeLano آلمان 3",
            countryName = "آلمان",
            flagEmoji = "🇩🇪",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@0sowk.hbsek.org:443?type=ws&security=tls&sni=0sowk.hbsek.org&host=0sowk.hbsek.org&path=%2Fvl%2Ft%2FwDwE%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A2%D9%84%D9%85%D8%A7%D9%86%203",
            dataRemainingGb = 0f,
            daysRemaining = 0
        ),
        VpnServer(
            id = "vip_de_3_b",
            name = "MeeLano آلمان 3 پلاس",
            countryName = "آلمان",
            flagEmoji = "🇩🇪",
            protocol = "VLESS",
            isVip = true,
            pingMs = 0,
            speedMbps = 0f,
            configLink = "vless://8d0f2761-c586-4822-8238-757ff717fdf8@178.239.157.239:443?security=tls&sni=static.telewebion.ir&host=static.telewebion.ir&pcs=7d65a6dcf82b36ee24aa7b724cf229806dde3e7c1e55b8b0d143141feb61895e&type=ws&path=%2Fvl%2Ft%2FwDwE%2Fgfdr&encryption=none&fp=chrome&alpn=http%2F1.1#MeeLano%20%D8%A2%D9%84%D9%85%D8%A7%D9%86%203%20%D9%BE%D9%84%D8%A7%D8%B3",
            dataRemainingGb = 0f,
            daysRemaining = 0
        )
    )
}
