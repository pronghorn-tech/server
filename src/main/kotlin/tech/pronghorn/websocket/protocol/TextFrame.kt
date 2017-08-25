//package tech.pronghorn.websocket.protocol
//
//import tech.pronghorn.server.HttpConnection
//import java.nio.charset.StandardCharsets
//
//class TextFrame(override val payload: ByteArray,
//                override val connection: HttpConnection,
//                val text: String = String(payload, StandardCharsets.UTF_8)) : WebsocketFrame(FrameType.TEXT) {
//
//    constructor(text: String,
//                connection: HttpConnection) : this(text.toByteArray(StandardCharsets.UTF_8), connection, text)
//
//    override fun toString(): String {
//        return "TextFrame($text)"
//    }
//}
