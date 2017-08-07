package tech.pronghorn.server.config

import java.net.InetSocketAddress

/**
 * Configuration for a WebServer.
 * The primary constructor takes a function that produces a FrameHandler, use this if the provided FrameHandler class
 * has mutable state that is not thread safe.

 * However, if a single shared FrameHandler is safe, use the secondary constructor.
 *
 * @param address : The address to bind to.
 * @param workerCount : The number of worker threads to utilize, should likely be the number of cores available.
 */
data class WebServerConfig(val address: InetSocketAddress,
                           val workerCount: Int,
                           val serverName: String = "Pronghorn")
