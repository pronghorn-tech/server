package tech.pronghorn.websocket.core

import tech.pronghorn.websocket.protocol.WebsocketFrame
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

class WebsocketHandshaker {
    private val base64Encoder = Base64.getEncoder()
    private val sha1Encoder: MessageDigest = MessageDigest.getInstance("SHA-1")

    fun getServerHandshakeResponse(key: String): String {
        val keyWithMagicNumber = key + WebsocketFrame.MAGIC_NUMBER
        sha1Encoder.reset()
        sha1Encoder.update(keyWithMagicNumber.toByteArray(StandardCharsets.UTF_8))
        val base64ResponseKey = base64Encoder.encodeToString(sha1Encoder.digest())

        return "HTTP/1.1 101 Switching Protocols\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: $base64ResponseKey\r\n" +
                "Upgrade: websocket\r\n\r\n"
    }

    fun getClientHandshakeRequest(host: String, keyBytes: ByteArray): String {
        if (keyBytes.size != 16) {
            throw IllegalArgumentException("WebSocket selectionKey must be 16 bytes.")
        }
        val key = base64Encoder.encodeToString(keyBytes)

        return "GET / HTTP/1.1\r\n" +
                "Host: $host\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: $key\r\n" +
                "Sec-WebSocket-Version: 13\r\n\r\n"
    }
}
