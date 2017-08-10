package tech.pronghorn.server.bufferpools

import tech.pronghorn.server.core.WebsocketConstants

class HandshakeBufferPool : BufferPoolManager(WebsocketConstants.maxHandshakeSize)
