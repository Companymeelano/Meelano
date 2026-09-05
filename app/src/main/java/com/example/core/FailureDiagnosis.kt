package com.example.core

/**
 * Turns a raw connection failure into something the user can act on.
 *
 * "اتصال ناموفق" is true but useless. The remedy for a dead server, a blocked
 * handshake and a missing internet connection are completely different, and on
 * a censored network the user is usually the only one who can tell which
 * applies — but only if the app describes what it saw.
 *
 * Classification is by evidence, from the most specific signal to the least, so
 * a precise cause is never masked by a generic one.
 */
object FailureDiagnosis {

    enum class Kind {
        /** The link itself is malformed. */
        BAD_CONFIG,

        /** Nothing answered at the address at all. */
        UNREACHABLE,

        /** TCP connected, then the peer hung up — the signature of a probe reset. */
        BLOCKED,

        /** TLS negotiation failed. Often DPI, sometimes a genuine cert problem. */
        TLS_REJECTED,

        /** The peer answered but rejected our credentials. */
        AUTH_REJECTED,

        /** Connected but nothing came back in time. */
        TIMEOUT,

        /** The device has no working internet at all. */
        NO_INTERNET,

        /** Android refused the VPN interface. */
        VPN_DENIED,

        /** Protocol the bundled engines cannot speak. */
        UNSUPPORTED,

        UNKNOWN
    }

    data class Result(
        val kind: Kind,
        /** One line stating what happened. */
        val summary: String,
        /** What the user should try next. */
        val advice: String,
        /**
         * Whether trying a different server is likely to help. False for faults
         * that would repeat on every node, so failover does not burn through
         * the whole list for nothing.
         */
        val tryAnotherServer: Boolean
    )

    /**
     * @param raw the underlying error text, which may be an exception message.
     * @param tcpReachable whether a plain TCP connection to the node succeeded,
     *   which is what separates "server is gone" from "handshake was blocked".
     */
    fun diagnose(raw: String?, tcpReachable: Boolean = false): Result {
        val text = raw.orEmpty().lowercase()

        return when {
            text.contains("قابل تجزیه") || text.contains("parse") ->
                Result(
                    Kind.BAD_CONFIG,
                    "کانفیگ این سرور معتبر نیست",
                    "لینک را دوباره کپی کنید یا سرور دیگری انتخاب کنید.",
                    tryAnotherServer = true
                )

            text.contains("مجوز vpn") || text.contains("tun") || text.contains("revoked") ->
                Result(
                    Kind.VPN_DENIED,
                    "اندروید اجازهٔ ساخت تونل نداد",
                    "اگر برنامهٔ VPN دیگری فعال است آن را ببندید، سپس دوباره تلاش کنید.",
                    tryAnotherServer = false
                )

            text.contains("پشتیبانی نمی‌شود") || text.contains("unsupported") ->
                Result(
                    Kind.UNSUPPORTED,
                    "این پروتکل در نسخهٔ فعلی پشتیبانی نمی‌شود",
                    "سرور دیگری با پروتکل VLESS، VMess یا Trojan انتخاب کنید.",
                    tryAnotherServer = true
                )

            // Credentials are wrong: retrying the same node is pointless, but
            // another node may well work.
            text.contains("رمز") || text.contains("auth") ||
                text.contains("پاسخ نامعتبر") || text.contains("invalid response") ->
                Result(
                    Kind.AUTH_REJECTED,
                    "سرور اتصال را نپذیرفت (شناسه یا رمز اشتباه)",
                    "این کانفیگ منقضی شده است. فهرست سرورها را به‌روزرسانی کنید.",
                    tryAnotherServer = true
                )

            text.contains("trust anchor") || text.contains("certpath") ||
                text.contains("ssl") || text.contains("handshake") ->
                Result(
                    Kind.TLS_REJECTED,
                    "دست‌دادن امن ناموفق بود",
                    "این معمولاً یعنی فیلترینگ اتصال را شناسایی کرده. " +
                        "سروری با Reality یا WebSocket امتحان کنید.",
                    tryAnotherServer = true
                )

            text.contains("timeout") || text.contains("زمان") ->
                if (tcpReachable) {
                    // Port answers but nothing comes back: classic DPI drop.
                    Result(
                        Kind.BLOCKED,
                        "سرور پاسخ می‌دهد ولی داده‌ای عبور نمی‌کند",
                        "به احتمال زیاد این نود مسدود شده است. سرور دیگری را امتحان کنید.",
                        tryAnotherServer = true
                    )
                } else {
                    Result(
                        Kind.TIMEOUT,
                        "سرور در زمان مقرر پاسخ نداد",
                        "اتصال اینترنت خود را بررسی کنید، سپس سرور دیگری را امتحان کنید.",
                        tryAnotherServer = true
                    )
                }

            text.contains("econnreset") || text.contains("connection reset") ||
                text.contains("closed before") || text.contains("eof") ->
                Result(
                    Kind.BLOCKED,
                    "اتصال بلافاصله پس از برقراری قطع شد",
                    "این الگوی معمول فیلترینگ فعال است. سروری با پروتکل متفاوت امتحان کنید.",
                    tryAnotherServer = true
                )

            text.contains("unable to resolve") || text.contains("unknownhost") ||
                text.contains("no address") ->
                Result(
                    Kind.NO_INTERNET,
                    "نام سرور قابل ترجمه نبود",
                    "اینترنت دستگاه قطع است یا DNS مسدود شده. " +
                        "در تنظیمات، DNS را به ۱٫۱٫۱٫۱ تغییر دهید.",
                    tryAnotherServer = false
                )

            text.contains("econnrefused") || text.contains("refused") ||
                text.contains("unreachable") || text.contains("در دسترس") ->
                Result(
                    Kind.UNREACHABLE,
                    "سرور در دسترس نیست",
                    "این نود احتمالاً خاموش شده است. فهرست را به‌روزرسانی کنید.",
                    tryAnotherServer = true
                )

            else ->
                Result(
                    Kind.UNKNOWN,
                    raw?.takeIf { it.isNotBlank() } ?: "اتصال برقرار نشد",
                    "سرور دیگری را امتحان کنید یا فهرست سرورها را به‌روزرسانی کنید.",
                    tryAnotherServer = true
                )
        }
    }
}
