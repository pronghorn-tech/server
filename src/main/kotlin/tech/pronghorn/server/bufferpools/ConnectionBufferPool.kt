package tech.pronghorn.server.bufferpools

import tech.pronghorn.server.core.WebsocketConstants

class ConnectionBufferPool : BufferPoolManager(WebsocketConstants.connectionBufferSize)
