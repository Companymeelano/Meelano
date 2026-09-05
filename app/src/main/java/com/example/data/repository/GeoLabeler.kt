package com.example.data.repository

/**
 * Derives a country label + flag from a node hostname / remark without any
 * network call, using ccTLDs, common datacentre naming and ISO codes that free
 * subscription providers embed in their remarks (e.g. `🇩🇪 DE-Frankfurt-04`).
 */
object GeoLabeler {

    data class Geo(val country: String, val flag: String)

    private val UNKNOWN = Geo("نامشخص", "🌐")

    private val byCode: Map<String, Geo> = mapOf(
        "de" to Geo("آلمان", "🇩🇪"), "nl" to Geo("هلند", "🇳🇱"), "fi" to Geo("فنلاند", "🇫🇮"),
        "tr" to Geo("ترکیه", "🇹🇷"), "gb" to Geo("انگلستان", "🇬🇧"), "uk" to Geo("انگلستان", "🇬🇧"),
        "se" to Geo("سوئد", "🇸🇪"), "fr" to Geo("فرانسه", "🇫🇷"), "ch" to Geo("سوئیس", "🇨🇭"),
        "at" to Geo("اتریش", "🇦🇹"), "pl" to Geo("لهستان", "🇵🇱"), "us" to Geo("آمریکا", "🇺🇸"),
        "ca" to Geo("کانادا", "🇨🇦"), "it" to Geo("ایتالیا", "🇮🇹"), "es" to Geo("اسپانیا", "🇪🇸"),
        "ru" to Geo("روسیه", "🇷🇺"), "ae" to Geo("امارات", "🇦🇪"), "in" to Geo("هند", "🇮🇳"),
        "jp" to Geo("ژاپن", "🇯🇵"), "sg" to Geo("سنگاپور", "🇸🇬"), "hk" to Geo("هنگ‌کنگ", "🇭🇰"),
        "kr" to Geo("کره جنوبی", "🇰🇷"), "au" to Geo("استرالیا", "🇦🇺"), "br" to Geo("برزیل", "🇧🇷"),
        "ro" to Geo("رومانی", "🇷🇴"), "cz" to Geo("چک", "🇨🇿"), "no" to Geo("نروژ", "🇳🇴"),
        "dk" to Geo("دانمارک", "🇩🇰"), "be" to Geo("بلژیک", "🇧🇪"), "ir" to Geo("ایران", "🇮🇷"),
        "am" to Geo("ارمنستان", "🇦🇲"), "ge" to Geo("گرجستان", "🇬🇪"), "lv" to Geo("لتونی", "🇱🇻"),
        "lt" to Geo("لیتوانی", "🇱🇹"), "ee" to Geo("استونی", "🇪🇪"), "ua" to Geo("اوکراین", "🇺🇦"),
        "md" to Geo("مولداوی", "🇲🇩"), "bg" to Geo("بلغارستان", "🇧🇬"), "hu" to Geo("مجارستان", "🇭🇺"),
        "ie" to Geo("ایرلند", "🇮🇪"), "pt" to Geo("پرتغال", "🇵🇹"), "il" to Geo("اسرائیل", "🇮🇱"),
        "qa" to Geo("قطر", "🇶🇦"), "kz" to Geo("قزاقستان", "🇰🇿"), "cn" to Geo("چین", "🇨🇳")
    )

    private val byCityKeyword: Map<String, String> = mapOf(
        "frankfurt" to "de", "berlin" to "de", "munich" to "de", "nuremberg" to "de",
        "amsterdam" to "nl", "helsinki" to "fi", "istanbul" to "tr", "ankara" to "tr",
        "london" to "gb", "manchester" to "gb", "stockholm" to "se", "paris" to "fr",
        "zurich" to "ch", "vienna" to "at", "warsaw" to "pl", "newyork" to "us",
        "dallas" to "us", "seattle" to "us", "toronto" to "ca", "milan" to "it",
        "madrid" to "es", "moscow" to "ru", "dubai" to "ae", "mumbai" to "in",
        "tokyo" to "jp", "osaka" to "jp", "singapore" to "sg", "hongkong" to "hk",
        "seoul" to "kr", "sydney" to "au", "bucharest" to "ro", "prague" to "cz",
        "oslo" to "no", "copenhagen" to "dk", "brussels" to "be", "yerevan" to "am",
        "tbilisi" to "ge", "riga" to "lv", "vilnius" to "lt", "tallinn" to "ee",
        "kyiv" to "ua", "sofia" to "bg", "budapest" to "hu", "dublin" to "ie",
        "lisbon" to "pt", "telaviv" to "il", "doha" to "qa", "almaty" to "kz"
    )

    fun of(host: String, remark: String = ""): Geo {
        val haystack = "$remark $host".lowercase()

        // 1. Flag emoji already present in the remark
        flagToCode(remark)?.let { code -> byCode[code]?.let { return it } }

        // 2. City names
        byCityKeyword.entries.firstOrNull { haystack.contains(it.key) }
            ?.let { entry -> byCode[entry.value]?.let { return it } }

        // 3. Explicit two letter codes surrounded by separators (de-01, [nl], _fr_)
        Regex("(?:^|[^a-z])([a-z]{2})(?:[^a-z]|$)").findAll(haystack)
            .mapNotNull { byCode[it.groupValues[1]] }
            .firstOrNull()
            ?.let { return it }

        // 4. ccTLD of the hostname
        host.substringAfterLast('.').lowercase().let { tld -> byCode[tld]?.let { return it } }

        return UNKNOWN
    }

    /** Converts a 🇩🇪 style regional-indicator pair back to "de". */
    private fun flagToCode(text: String): String? {
        val codePoints = text.codePoints().toArray()
        for (i in 0 until codePoints.size - 1) {
            val a = codePoints[i]
            val b = codePoints[i + 1]
            if (a in 0x1F1E6..0x1F1FF && b in 0x1F1E6..0x1F1FF) {
                val first = 'a' + (a - 0x1F1E6)
                val second = 'a' + (b - 0x1F1E6)
                return "$first$second"
            }
        }
        return null
    }
}
