package com.example.vpn.proto

import java.io.InputStream
import java.io.OutputStream

/**
 * A live, protocol-level tunnel to one destination through a proxy server.
 *
 * Implementations perform the genuine wire handshake of their protocol (VLESS,
 * Trojan, Shadowsocks, VMess, SOCKS5) so that arbitrary TCP traffic — HTTPS to
 * Instagram, for example — really is carried end to end.
 */
interface Outbound {
    val input: InputStream
    val output: OutputStream
    fun close()
}

/** Destination address of a proxied flow, in the form the protocols encode it. */
data class Destination(val host: String, val port: Int, val isDomain: Boolean) {
    companion object {
        private val IPV4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

        fun of(host: String, port: Int): Destination =
            Destination(host, port, !IPV4.matches(host))
    }

    /** SOCKS5-style address encoding, shared by VLESS, VMess and Trojan. */
    fun encode(): ByteArray {
        val out = ArrayList<Byte>(host.length + 6)
        if (isDomain) {
            out.add(0x03)                                   // domain
            val bytes = host.toByteArray(Charsets.US_ASCII)
            out.add(bytes.size.toByte())
            bytes.forEach { out.add(it) }
        } else {
            out.add(0x01)                                   // IPv4
            host.split('.').forEach { out.add(it.toInt().toByte()) }
        }
        return out.toByteArray()
    }

    /** Address encoding with the port placed *before* the address (VLESS/VMess). */
    fun encodeWithPortFirst(): ByteArray {
        val address = encode()
        val result = ByteArray(2 + address.size)
        result[0] = ((port ushr 8) and 0xFF).toByte()
        result[1] = (port and 0xFF).toByte()
        address.copyInto(result, 2)
        return result
    }

    /** Address encoding with the port placed *after* the address (Trojan/SOCKS5). */
    fun encodeWithPortLast(): ByteArray {
        val address = encode()
        val result = ByteArray(address.size + 2)
        address.copyInto(result, 0)
        result[address.size] = ((port ushr 8) and 0xFF).toByte()
        result[address.size + 1] = (port and 0xFF).toByte()
        return result
    }
}
