package com.example.vpn.proto

import com.example.core.ProxyEndpoint
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Genuine Shadowsocks AEAD client (`aes-128-gcm`, `aes-256-gcm`,
 * `chacha20-ietf-poly1305`).
 *
 * Wire format per SIP004: the master key is derived from the password with
 * EVP_BytesToKey, a random salt is sent first, and the session subkey is
 * `HKDF-SHA1(masterKey, salt, "ss-subkey")`. Payload is then a stream of
 * length-prefixed AEAD chunks:
 * ```
 * [encrypted 2-byte length][16-byte tag][encrypted payload][16-byte tag]
 * ```
 * with a per-chunk little-endian nonce counter.
 */
class ShadowsocksOutbound(
    endpoint: ProxyEndpoint,
    destination: Destination,
    protect: (Socket) -> Boolean
) : Outbound {

    private val spec = CipherSpec.of(endpoint.method)
    private val socket = Socket()
    private val rawIn: InputStream
    private val rawOut: OutputStream

    private val masterKey: ByteArray
    private lateinit var encryptKey: ByteArray
    private lateinit var decryptKey: ByteArray
    private var encryptNonce = ByteArray(12)
    private var decryptNonce = ByteArray(12)
    private var headerSent = false
    private var saltReceived = false

    private val pendingHeader: ByteArray

    init {
        protect(socket)
        socket.tcpNoDelay = true
        socket.soTimeout = 60_000
        socket.connect(InetSocketAddress(endpoint.host, endpoint.port), 10_000)
        rawIn = socket.getInputStream().buffered(32 * 1024)
        rawOut = socket.getOutputStream()

        val password = endpoint.password.ifBlank { endpoint.userId }
        masterKey = evpBytesToKey(password, spec.keySize)

        val salt = ByteArray(spec.saltSize).also { SecureRandom().nextBytes(it) }
        encryptKey = hkdfSha1(masterKey, salt, SUBKEY_INFO, spec.keySize)
        rawOut.write(salt)

        // The SOCKS5-style target address is the first payload chunk.
        pendingHeader = destination.encodeWithPortLast()
    }

    override val output: OutputStream = object : OutputStream() {
        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

        override fun write(b: ByteArray, off: Int, len: Int) {
            synchronized(this@ShadowsocksOutbound) {
                if (!headerSent) {
                    headerSent = true
                    writeChunk(pendingHeader, 0, pendingHeader.size)
                }
                var written = 0
                while (written < len) {
                    val count = minOf(MAX_CHUNK, len - written)
                    writeChunk(b, off + written, count)
                    written += count
                }
                rawOut.flush()
            }
        }

        override fun flush() = rawOut.flush()
    }

    override val input: InputStream = object : InputStream() {
        private var buffer: ByteArray = ByteArray(0)
        private var offset = 0

        override fun read(): Int {
            val single = ByteArray(1)
            return if (read(single, 0, 1) == 1) single[0].toInt() and 0xFF else -1
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            while (offset >= buffer.size) {
                buffer = readChunk() ?: return -1
                offset = 0
            }
            val count = minOf(len, buffer.size - offset)
            buffer.copyInto(b, off, offset, offset + count)
            offset += count
            return count
        }
    }

    // ---- AEAD chunk framing ------------------------------------------------

    private fun writeChunk(data: ByteArray, off: Int, len: Int) {
        val lengthBlock = byteArrayOf(((len ushr 8) and 0xFF).toByte(), (len and 0xFF).toByte())
        rawOut.write(seal(encryptKey, encryptNonce, lengthBlock))
        increment(encryptNonce)
        rawOut.write(seal(encryptKey, encryptNonce, data.copyOfRange(off, off + len)))
        increment(encryptNonce)
    }

    private fun readChunk(): ByteArray? {
        if (!saltReceived) {
            val salt = ByteArray(spec.saltSize)
            if (!readFully(salt)) return null
            decryptKey = hkdfSha1(masterKey, salt, SUBKEY_INFO, spec.keySize)
            saltReceived = true
        }

        val lengthCipher = ByteArray(2 + TAG_SIZE)
        if (!readFully(lengthCipher)) return null
        val lengthPlain = open(decryptKey, decryptNonce, lengthCipher) ?: return null
        increment(decryptNonce)

        val length = ((lengthPlain[0].toInt() and 0xFF) shl 8) or (lengthPlain[1].toInt() and 0xFF)
        if (length <= 0 || length > MAX_CHUNK) throw IOException("Invalid Shadowsocks chunk size $length")

        val payloadCipher = ByteArray(length + TAG_SIZE)
        if (!readFully(payloadCipher)) return null
        val payload = open(decryptKey, decryptNonce, payloadCipher)
            ?: throw IOException("Shadowsocks payload authentication failed")
        increment(decryptNonce)
        return payload
    }

    private fun seal(key: ByteArray, nonce: ByteArray, plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(spec.transformation)
        val keySpec = SecretKeySpec(key, spec.algorithm)
        if (spec.isChaCha) {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(nonce))
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(TAG_SIZE * 8, nonce))
        }
        return cipher.doFinal(plain)
    }

    private fun open(key: ByteArray, nonce: ByteArray, sealed: ByteArray): ByteArray? = try {
        val cipher = Cipher.getInstance(spec.transformation)
        val keySpec = SecretKeySpec(key, spec.algorithm)
        if (spec.isChaCha) {
            cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(nonce))
        } else {
            cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(TAG_SIZE * 8, nonce))
        }
        cipher.doFinal(sealed)
    } catch (e: Exception) {
        null
    }

    private fun readFully(target: ByteArray): Boolean {
        var read = 0
        while (read < target.size) {
            val count = rawIn.read(target, read, target.size - read)
            if (count < 0) return false
            read += count
        }
        return true
    }

    override fun close() {
        runCatching { socket.close() }
    }

    private data class CipherSpec(
        val keySize: Int,
        val saltSize: Int,
        val transformation: String,
        val algorithm: String,
        val isChaCha: Boolean
    ) {
        companion object {
            fun of(method: String): CipherSpec = when (method.lowercase().trim()) {
                "aes-128-gcm" -> CipherSpec(16, 16, "AES/GCM/NoPadding", "AES", false)
                "chacha20-ietf-poly1305", "chacha20-poly1305" ->
                    CipherSpec(32, 32, "ChaCha20-Poly1305", "ChaCha20", true)
                // aes-256-gcm is both the most common and the safe default.
                else -> CipherSpec(32, 32, "AES/GCM/NoPadding", "AES", false)
            }
        }
    }

    companion object {
        /**
         * AEAD ciphers this client implements. Shadowsocks-2022 (`2022-blake3-*`)
         * uses a different key schedule and session protocol, so those nodes are
         * rejected up front rather than failing mid-handshake.
         */
        private val SUPPORTED_METHODS = setOf(
            "aes-128-gcm", "aes-256-gcm",
            "chacha20-ietf-poly1305", "chacha20-poly1305"
        )

        fun supportsMethod(method: String): Boolean {
            val normalised = method.lowercase().trim()
            // A blank method usually means the link omitted it; assume the default.
            if (normalised.isEmpty()) return true
            return normalised in SUPPORTED_METHODS
        }

        private const val TAG_SIZE = 16
        private const val MAX_CHUNK = 0x3FFF
        private val SUBKEY_INFO = "ss-subkey".toByteArray(Charsets.US_ASCII)

        /** Little-endian nonce counter increment, per the Shadowsocks AEAD spec. */
        fun increment(nonce: ByteArray) {
            for (i in nonce.indices) {
                val value = (nonce[i].toInt() and 0xFF) + 1
                nonce[i] = (value and 0xFF).toByte()
                if (value <= 0xFF) break
            }
        }

        /** OpenSSL EVP_BytesToKey with MD5, as Shadowsocks specifies. */
        fun evpBytesToKey(password: String, keySize: Int): ByteArray {
            val passwordBytes = password.toByteArray(Charsets.UTF_8)
            val md5 = MessageDigest.getInstance("MD5")
            val key = ByteArray(keySize)
            var previous = ByteArray(0)
            var generated = 0
            while (generated < keySize) {
                md5.reset()
                md5.update(previous)
                md5.update(passwordBytes)
                previous = md5.digest()
                val count = minOf(previous.size, keySize - generated)
                previous.copyInto(key, generated, 0, count)
                generated += count
            }
            return key
        }

        /** HKDF-SHA1 (extract + expand) used to derive the per-session subkey. */
        fun hkdfSha1(key: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(salt, "HmacSHA1"))
            val pseudoRandomKey = mac.doFinal(key)

            val result = ByteArray(length)
            var block = ByteArray(0)
            var generated = 0
            var counter = 1
            while (generated < length) {
                mac.reset()
                mac.init(SecretKeySpec(pseudoRandomKey, "HmacSHA1"))
                mac.update(block)
                mac.update(info)
                mac.update(counter.toByte())
                block = mac.doFinal()
                val count = minOf(block.size, length - generated)
                block.copyInto(result, generated, 0, count)
                generated += count
                counter++
            }
            return result
        }
    }
}
