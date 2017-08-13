package tech.pronghorn.server.bufferpools

import tech.pronghorn.server.core.WebsocketConstants

class ConnectionBufferPool(direct: Boolean = false) : BufferPoolManager(WebsocketConstants.connectionBufferSize, direct)
