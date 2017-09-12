package tech.pronghorn.server.config

import tech.pronghorn.plugins.logging.LoggingPlugin
import tech.pronghorn.server.ReusePort
import tech.pronghorn.util.kibibytes
import tech.pronghorn.util.mebibytes
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel

object HttpServerConfigDefaultValues {
    val workerCount = Runtime.getRuntime().availableProcessors()
    const val serverName = "Pronghorn"
    const val sendServerHeader = true
    const val sendDateHeader = true
    val reusePort = supportsReusePort()
    const val listenBacklog = 128
    const val maxPipelinedRequests = 64
    val maxRequestSize = mebibytes(1)
    val reusableBufferSize = kibibytes(64)
    val useDirectByteBuffers = true

    private fun supportsReusePort(): Boolean {
        val tmpSocket = ServerSocketChannel.open()
        val success = ReusePort.setReusePort(tmpSocket)
        tmpSocket.close()
        return success
    }
}

/**
 * Configuration for a HttpServer.
 * The primary constructor takes a function that produces a FrameHandler, use this if the provided FrameHandler class
 * has mutable state that is not thread safe.

 * However, if a single shared FrameHandler is safe, use the secondary constructor.
 *
 * @param address : The address to bind to.
 * @param workerCount : The number of worker threads to utilize, should likely be the number of cores available.
 * @param serverName : The value to send in the Server response header if sendServerHeader is true
 * @param sendServerHeader : If true, the Server header is automatically sent with each response
 * @param sendDateHeader : If true, the Date header is automatically sent with each response
 * @param reusableBufferSize : The size of pooled read/write buffers, should be at least as large as the average expected request.
 * @param reusePort : If true, the SO_REUSEPORT socket option is used and each worker uses a dedicated socket
 * @param listenBacklog : The value for the accept queue for the server socket.
 * @param acceptGrouping : How many connections should be accepted at a time, usually equal to the listen backlog.
 * @param maxPipelinedRequests : The maximum number of http requests allowed to be pipelined on a single connection.
 * @param maxRequestSize : The maximum acceptable size of a single http request.
 * @param useDirectByteBuffers : Whether socket read/write buffers should be direct ByteBuffers
 */
class HttpServerConfig(val address: InetSocketAddress,
                       workerCount: Int = HttpServerConfigDefaultValues.workerCount,
                       serverName: String = HttpServerConfigDefaultValues.serverName,
                       val sendServerHeader: Boolean = HttpServerConfigDefaultValues.sendServerHeader,
                       val sendDateHeader: Boolean = HttpServerConfigDefaultValues.sendDateHeader,
                       reusableBufferSize: Int = HttpServerConfigDefaultValues.reusableBufferSize,
                       reusePort: Boolean = HttpServerConfigDefaultValues.reusePort,
                       listenBacklog: Int = HttpServerConfigDefaultValues.listenBacklog,
                       acceptGrouping: Int = listenBacklog,
                       maxPipelinedRequests: Int = HttpServerConfigDefaultValues.maxPipelinedRequests,
                       maxRequestSize: Int = HttpServerConfigDefaultValues.maxRequestSize,
                       val useDirectByteBuffers: Boolean = HttpServerConfigDefaultValues.useDirectByteBuffers) {
    private val logger = LoggingPlugin.get(javaClass)
    val workerCount = validateWorkerCount(workerCount)
    val serverName = validateServerName(serverName)
    val reusableBufferSize = validateReusableBufferSize(reusableBufferSize)
    val listenBacklog = validateListenBacklog(listenBacklog)
    val acceptGrouping = validateAcceptGrouping(acceptGrouping)
    val reusePort = validateReusePort(reusePort)
    val maxPipelinedRequests = validateMaxPipelinedRequests(maxPipelinedRequests)
    val maxRequestSize = validateMaxRequestSize(maxRequestSize)

    override fun toString(): String {
        return "HttpServerConfig: " +
                "address($address), " +
                "workerCount($workerCount), " +
                "serverName($serverName), " +
                "sendServerHeader($sendServerHeader), " +
                "sendDateHeader($sendDateHeader), " +
                "reusableBufferSize($reusableBufferSize), " +
                "reusePort($reusePort), " +
                "listenBacklog($listenBacklog), " +
                "acceptGrouping($acceptGrouping), " +
                "maxPipelinedRequests($maxPipelinedRequests), " +
                "maxRequestSize($maxRequestSize)," +
                "useDirectByteBuffers($useDirectByteBuffers)"
    }

    private fun validateWorkerCount(value: Int): Int {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        if (value < 1) {
            logger.warn {
                "workerCount value ($value) must be greater than zero, " +
                        "using default value(${HttpServerConfigDefaultValues.workerCount})"
            }
            return HttpServerConfigDefaultValues.workerCount
        }
        else {
            if (value > availableProcessors) {
                logger.warn {
                    "workerCount value ($value) is greater than available processors ($availableProcessors). " +
                            "Utilizing more workers than there are available processors is not advised."
                }
            }
            return value
        }
    }

    private fun validateServerName(value: String): String {
        if (!Charsets.US_ASCII.newEncoder().canEncode(value)) {
            logger.warn { "serverName value ($value) contains non-ascii characters, this is likely to cause problems." }
        }
        return value
    }

    private fun validateReusableBufferSize(value: Int): Int {
        if (value < 1) {
            logger.warn { "reusableBufferSize set to invalid value ($value), using default: (${HttpServerConfigDefaultValues.reusableBufferSize})" }
            return HttpServerConfigDefaultValues.reusableBufferSize
        }
        else {
            return value
        }
    }

    private fun validateListenBacklog(value: Int): Int {
        if (value < 1) {
            logger.warn {
                "listenBacklog value ($value) must be greater than zero, " +
                        "using default value(${HttpServerConfigDefaultValues.listenBacklog})"
            }
            return HttpServerConfigDefaultValues.listenBacklog
        }
        else {
            return value
        }
    }

    private fun validateAcceptGrouping(value: Int): Int {
        if (value < 1) {
            logger.warn {
                "acceptGrouping value ($value) must be greater than zero, " +
                        "using listenBacklog value ($listenBacklog)"
            }
            return listenBacklog
        }
        else {
            return value
        }
    }

    private fun validateReusePort(value: Boolean): Boolean {
        val osName = System.getProperty("os.name") ?: "Unknown"
        if (value && !osName.contains("Linux")) {
            logger.warn { "reusePort value ($value) is currently only supported on Linux, and is not supported on ($osName)." }
        }

        return value
    }

    private fun validateMaxPipelinedRequests(value: Int): Int {
        if (value < 1) {
            logger.warn {
                "maxPipelinedRequests value ($value) must be greater than zero. " +
                        "To disable pipelining, use a value of 1. Using default value (${HttpServerConfigDefaultValues.maxPipelinedRequests})"
            }
            return HttpServerConfigDefaultValues.maxPipelinedRequests
        }
        else {
            return value
        }
    }

    private fun validateMaxRequestSize(value: Int): Int {
        if (value < 1) {
            logger.warn {
                "maxRequestSize value($value) must be greater than zero. " +
                        "Using default value (${HttpServerConfigDefaultValues.maxRequestSize})"
            }
            return HttpServerConfigDefaultValues.maxRequestSize
        }
        else {
            return value
        }
    }
}
