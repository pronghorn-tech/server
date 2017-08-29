package tech.pronghorn.websocket

/*
class WebsocketServerConnection(worker: HttpServerWorker,
                           socket: SocketChannel,
                           selectionKey: SelectionKey) : HttpConnection(worker, socket, selectionKey) {
    override val shouldSendMasked: Boolean = false
    override val requiresMasked: Boolean = true

    override fun handleHandshakeRequest(request: ParsedHttpRequest,
                                        handshaker: WebsocketHandshaker): Boolean {
        val requestHeaders = request.requestHeaders
        val key = requestHeaders["Sec-WebSocket-Key"]
        if (key == null) {
            close("Websocket handshakes must include a Sec-WebSocket-Key header.")
            return false
        }
        else {
            val handshake = handshaker.getServerHandshakeResponse(key)
            try {
                socket.write(handshake)
                return true
            }
            catch (e: IOException) {
                close("Unexpected error replying to initial handshake.")
                return false
            }
        }
    }

    private fun parseHandshakeRequest(): ParsedHttpRequest? {
        TODO()
//        if (!socket.isOpen) {
//            return null
//        }
//
//        val buffer = getHandshakeBuffer()
//        val read = socket.read(buffer)
//        if (read < 0) {
//            close("Disconnected.")
//            return null
//        }
//
//        if (read > 0) {
//            buffer.flip()
//            val request = HttpRequestParser.parse(buffer)
//            if (request == null) {
//                if (buffer.limit() == buffer.capacity()) {
//                    // The entire handshake buffer is full, but still no valid handshake
//                    close("Websocket handshake not received before data or too large.")
//                    return null
//                } else {
//                    // Handshake assumed to be partially read, reset the buffer for more reading
//                    buffer.position(buffer.limit())
//                    buffer.limit(buffer.capacity())
//                    // Set the appropriate interestOps for more reading
//                    selectionKey.interestOps(selectionKey.interestOps() or SelectionKey.OP_READ)
//                    return null
//                }
//            } else {
//                releaseHandshakeBuffer()
//                return request
//            }
//        }

        return null
    }

//    abstract fun handleHandshakeRequest(request: ParsedHttpRequest,
//                                        handshaker: WebsocketHandshaker): Boolean


//    fun attemptHandshake(handshaker: WebsocketHandshaker) {
//        val request = parseHandshakeRequest()
//        if (request != null) {
//            // With a request, handleHandshake either succeeds, or closes the connection
//            val success = handleHandshakeRequest(request, handshaker)
//            if (success) {
//                isHandshakeComplete = true
//                selectionKey.interestOps(selectionKey.interestOps() or SelectionKey.OP_READ)
//                worker.clearPendingConnection(this)
//            }
//        }
//    }
}
*/
