package com.example.vpn.proto

import com.example.core.ProxyEndpoint
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Genuine VMess client (AEAD request header, `aes-128-gcm` body security).
 *
 * VMess is the most intricate of the supported protocols. The request header is
 * encrypted with the VMess AEAD scheme (`VMess Header AEAD_Key/Nonce` KDF paths),
 * and the body is a stream of `[2-byte encrypted length][AEAD chunk]` records
 * keyed independently per direction.
 */
class VmessOutbound(
    endpoint: ProxyEndpoint,
    destination: Destination,
    protect: (Socket) -> Boolean
) : Outbound {

    private val carrier = Transport.open(endpoint, protect)

    private val requestKey = ByteArray(16).also { SecureRandom().nextBytes(it) }
    private val requestIv = ByteArray(16).also { SecureRandom().nextBytes(it) }
    private val responseVerify: Byte = SecureRandom().nextInt(256).toByte()

    private val responseKey = sha256(requestKey).copyOf(16)
    private val responseIv = sha256(requestIv).copyOf(16)

    private var encryptCount = 0
    private var decryptCount = 0
    private var responseHeaderConsumed = false

    init {
        val uuid = parseUuid(endpoint.userId)
        val address = destination.encodeWithPortFirst()

        // ---- plain request header ----
        val padding = SecureRandom().nextInt(16)
        val paddingBytes = ByteArray(padding).also { SecureRandom().nextBytes(it) }

        val body = ByteBuffer.allocate(1 + 16 + 16 + 1 + 1 + 1 + 1 + 1 + address.size + padding + 4)
        body.put(1)                                   // version
        body.put(requestIv)
        body.put(requestKey)
        body.put(responseVerify)
        body.put(0x05)                                // option: CHUNK_STREAM | CHUNK_MASKING | GLOBAL_PADDING off
        body.put(((padding shl 4) or SECURITY_AES128GCM).toByte())
        body.put(0)                                   // reserved
        body.put(CMD_TCP)
        body.put(address)
        body.put(paddingBytes)

        val withoutChecksum = body.array().copyOf(body.position())
        val checksum = fnv1a(withoutChecksum)
        body.putInt(checksum)
        val header = body.array().copyOf(body.position())

        carrier.output.write(sealHeader(uuid, header))
        carrier.output.flush()
    }

    // ---- AEAD request header (VMess 2019 spec) -----------------------------

    private fun sealHeader(uuid: ByteArray, header: ByteArray): ByteArray {
        val authId = createAuthId(uuid)
        val nonce = ByteArray(8).also { SecureRandom().nextBytes(it) }
        val cmdKey = md5(uuid + KDF_SALT_CMD)

        val lengthKey = kdf16(cmdKey, LEN_KEY, authId, nonce)
        val lengthNonce = kdf(cmdKey, LEN_IV, authId, nonce).copyOf(12)
        val lengthPlain = byteArrayOf(((header.size ushr 8) and 0xFF).toByte(), (header.size and 0xFF).toByte())
        val lengthSealed = gcm(Cipher.ENCRYPT_MODE, lengthKey, lengthNonce, authId, lengthPlain)

        val payloadKey = kdf16(cmdKey, PAYLOAD_KEY, authId, nonce)
        val payloadNonce = kdf(cmdKey, PAYLOAD_IV, authId, nonce).copyOf(12)
        val payloadSealed = gcm(Cipher.ENCRYPT_MODE, payloadKey, payloadNonce, authId, header)

        return authId + lengthSealed + nonce + payloadSealed
    }

    private fun createAuthId(uuid: ByteArray): ByteArray {
        val cmdKey = md5(uuid + KDF_SALT_CMD)
        val buffer = ByteBuffer.allocate(16)
        buffer.putLong(System.currentTimeMillis() / 1000)
        val random = ByteArray(4).also { SecureRandom().nextBytes(it) }
        buffer.put(random)
        val head = buffer.array().copyOf(12)
        val crc = crc32(head)
        val full = ByteBuffer.allocate(16).put(head).putInt(crc.toInt()).array()

        val key = kdf16(cmdKey, AUTH_ID_KEY)
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(full)
    }

    // ---- body framing ------------------------------------------------------

    override val output: OutputStream = object : OutputStream() {
        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

        override fun write(b: ByteArray, off: Int, len: Int) {
            synchronized(this@VmessOutbound) {
                var written = 0
                while (written < len) {
                    val count = minOf(MAX_CHUNK, len - written)
                    val nonce = chunkNonce(requestIv, encryptCount)
                    val sealed = gcm(
                        Cipher.ENCRYPT_MODE, requestKey, nonce, null,
                        b.copyOfRange(off + written, off + written + count)
                    )
                    encryptCount++
                    carrier.output.write((sealed.size ushr 8) and 0xFF)
                    carrier.output.write(sealed.size and 0xFF)
                    carrier.output.write(sealed)
                    written += count
                }
                carrier.output.flush()
            }
        }

        override fun flush() = carrier.output.flush()
    }

    override val input: InputStream = object : InputStream() {
        private var buffer = ByteArray(0)
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

    private fun readChunk(): ByteArray? {
        consumeResponseHeader()

        val lengthBytes = ByteArray(2)
        if (!readFully(lengthBytes)) return null
        val length = ((lengthBytes[0].toInt() and 0xFF) shl 8) or (lengthBytes[1].toInt() and 0xFF)
        if (length <= 16 || length > MAX_CHUNK + 16) {
            if (length == 0) return null
            throw IOException("Invalid VMess chunk length $length")
        }

        val sealed = ByteArray(length)
        if (!readFully(sealed)) return null
        val nonce = chunkNonce(responseIv, decryptCount)
        decryptCount++
        return gcm(Cipher.DECRYPT_MODE, responseKey, nonce, null, sealed)
    }

    /** The response header is itself AEAD-sealed and must be stripped first. */
    private fun consumeResponseHeader() {
        if (responseHeaderConsumed) return
        responseHeaderConsumed = true

        val lengthKey = kdf16(responseKey, RESP_LEN_KEY)
        val lengthNonce = kdf(responseIv, RESP_LEN_IV).copyOf(12)
        val sealedLength = ByteArray(2 + 16)
        if (!readFully(sealedLength)) throw IOException("VMess response header missing")
        val plainLength = gcm(Cipher.DECRYPT_MODE, lengthKey, lengthNonce, null, sealedLength)
        val headerLength = ((plainLength[0].toInt() and 0xFF) shl 8) or (plainLength[1].toInt() and 0xFF)

        val sealedHeader = ByteArray(headerLength + 16)
        if (!readFully(sealedHeader)) throw IOException("VMess response header truncated")
        val headerKey = kdf16(responseKey, RESP_PAYLOAD_KEY)
        val headerNonce = kdf(responseIv, RESP_PAYLOAD_IV).copyOf(12)
        val header = gcm(Cipher.DECRYPT_MODE, headerKey, headerNonce, null, sealedHeader)

        if (header.isEmpty() || header[0] != responseVerify) {
            throw IOException("VMess response verification failed — wrong UUID?")
        }
    }

    private fun readFully(target: ByteArray): Boolean {
        var read = 0
        while (read < target.size) {
            val count = carrier.input.read(target, read, target.size - read)
            if (count < 0) return false
            read += count
        }
        return true
    }

    override fun close() = carrier.close()

    private companion object {
        const val CMD_TCP: Byte = 1
        const val SECURITY_AES128GCM = 3
        const val MAX_CHUNK = 0x3FFF

        val KDF_SALT_CMD = "c48619fe-8f02-49e0-b9e9-edf763e17e21".toByteArray(Charsets.US_ASCII)
        val KDF_SALT = "VMess AEAD KDF".toByteArray(Charsets.US_ASCII)
        val AUTH_ID_KEY = "AES Auth ID Encryption".toByteArray(Charsets.US_ASCII)
        val LEN_KEY = "VMess Header AEAD Key_Length".toByteArray(Charsets.US_ASCII)
        val LEN_IV = "VMess Header AEAD Nonce_Length".toByteArray(Charsets.US_ASCII)
        val PAYLOAD_KEY = "VMess Header AEAD Key".toByteArray(Charsets.US_ASCII)
        val PAYLOAD_IV = "VMess Header AEAD Nonce".toByteArray(Charsets.US_ASCII)
        val RESP_LEN_KEY = "AEAD Resp Header Len Key".toByteArray(Charsets.US_ASCII)
        val RESP_LEN_IV = "AEAD Resp Header Len IV".toByteArray(Charsets.US_ASCII)
        val RESP_PAYLOAD_KEY = "AEAD Resp Header Key".toByteArray(Charsets.US_ASCII)
        val RESP_PAYLOAD_IV = "AEAD Resp Header IV".toByteArray(Charsets.US_ASCII)

        fun parseUuid(value: String): ByteArray {
            val uuid = runCatching { UUID.fromString(value.trim()) }
                .getOrElse { UUID.nameUUIDFromBytes(value.toByteArray(Charsets.UTF_8)) }
            return ByteBuffer.allocate(16)
                .putLong(uuid.mostSignificantBits)
                .putLong(uuid.leastSignificantBits)
                .array()
        }

        fun md5(data: ByteArray): ByteArray = MessageDigest.getInstance("MD5").digest(data)
        fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

        /** VMess recursive HMAC-SHA256 based KDF. */
        fun kdf(key: ByteArray, vararg paths: ByteArray): ByteArray {
            var mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(KDF_SALT, "HmacSHA256")) }
            for (path in paths) {
                val derived = mac.doFinal(path)
                mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(derived, "HmacSHA256")) }
            }
            return mac.doFinal(key)
        }

        fun kdf16(key: ByteArray, vararg paths: ByteArray): ByteArray = kdf(key, *paths).copyOf(16)

        fun gcm(mode: Int, key: ByteArray, nonce: ByteArray, aad: ByteArray?, data: ByteArray): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(mode, SecretKeySpec(key, "AES"), javax.crypto.spec.GCMParameterSpec(128, nonce))
            if (aad != null) cipher.updateAAD(aad)
            return cipher.doFinal(data)
        }

        /** Per-chunk nonce: 2-byte counter followed by iv[2..12]. */
        fun chunkNonce(iv: ByteArray, count: Int): ByteArray {
            val nonce = ByteArray(12)
            nonce[0] = ((count ushr 8) and 0xFF).toByte()
            nonce[1] = (count and 0xFF).toByte()
            iv.copyInto(nonce, 2, 2, 12)
            return nonce
        }

        fun fnv1a(data: ByteArray): Int {
            var hash = -0x7ee3623b  // 2166136261
            for (b in data) {
                hash = hash xor (b.toInt() and 0xFF)
                hash *= 16777619
            }
            return hash
        }

        fun crc32(data: ByteArray): Long =
            java.util.zip.CRC32().apply { update(data) }.value
    }
}
