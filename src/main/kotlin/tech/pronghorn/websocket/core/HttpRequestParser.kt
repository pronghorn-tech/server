package tech.pronghorn.websocket.core

import com.google.common.base.Splitter
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * A very minimally featured parser for HTTP requests.
 * Implements only what's needed to successfully parse WebSocket handshakes.
 */
internal object HttpRequestParserOLD {
    private val colonSplitter = Splitter.on(':').trimResults()
    private val newLineSplitter = Splitter.on("\r\n")

    /**
     * Attempts to parse a ParsedHttpRequest from the beginning of the provided buffer.
     *
     * @param buffer Buffer from which a request is to be parsed.
     * @return On success, a parsed request, otherwise if no request was present, null.
     */
    fun parse(buffer: ByteBuffer): ParsedHttpRequest? {
        var endOfHeaders = -1
        var x = (buffer.remaining() - 4)
        while(x >= 0){
            // Search backwards for the end of the headers /r/n/r/n, (218762506 as a 4 byte integer)
            if (buffer.getInt(x) == 218762506) {
                endOfHeaders = x
                break
            }
            x -= 1
        }

        if (endOfHeaders == -1) {
            return null
        }

        val requestBytes = ByteArray(endOfHeaders)
        buffer.get(requestBytes)
        val request = String(requestBytes, StandardCharsets.UTF_8)
        val headerLines = newLineSplitter.splitToList(request)
        val status = headerLines.first()
        val headers = HashMap<String, String>()
        headerLines.drop(1).forEach { headerLine ->
            val header = colonSplitter.splitToList(headerLine)
            headers.put(header.first(), header.last())
        }

        return ParsedHttpRequest(status, headers)
    }
}
