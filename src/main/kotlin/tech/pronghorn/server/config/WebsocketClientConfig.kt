package tech.pronghorn.server.config

import tech.pronghorn.websocket.core.FrameHandler
import java.time.Duration
import java.util.*

/**
 * Configuration for a WebsocketClient
 * The primary constructor takes a function that produces a FrameHandler, use this if the provided FrameHandler class
 * has mutable state that is not thread safe.
 *
 * @param frameHandlerBuilder : A function that produces a FrameHandler per worker thread.
 * @param randomGeneratorBuilder : A function that produces a Random per worker thread. These are assumed not to be thread safe.
 * @param workerCount : The number of worker threads to utilize for this client.
 * @param handshakeTimeout : How long before a websocket is disconnected if it hasn't received a handshake response.
 */
class WebsocketClientConfig(val frameHandlerBuilder: () -> FrameHandler,
                            val workerCount: Int,
                            val randomGeneratorBuilder: () -> Random = { Random() },
                            val handshakeTimeout: Duration = Duration.ofSeconds(10)) {
    /**
     * Secondary constructor, same as primary except a single FrameHandler is provided that is shared among all workers.
     */
    constructor(frameHandler: FrameHandler,
                workerCount: Int,
                randomGeneratorBuilder: () -> Random = { Random() },
                handshakeTimeout: Duration = Duration.ofSeconds(10)) : this({ frameHandler }, workerCount, randomGeneratorBuilder, handshakeTimeout)
}
