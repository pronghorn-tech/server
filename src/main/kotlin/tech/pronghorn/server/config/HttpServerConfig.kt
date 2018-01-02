/*
 * Copyright 2017 Pronghorn Technology LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.pronghorn.server.config

import tech.pronghorn.plugins.logging.LoggingPlugin
import tech.pronghorn.server.ReusePort
import tech.pronghorn.util.kibibytes
import tech.pronghorn.util.mebibytes
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

public object HttpServerConfigDefaultValues {
    public val workerCount = Runtime.getRuntime().availableProcessors()
    public const val serverName = "Pronghorn"
    public const val sendServerHeader = true
    public const val sendDateHeader = true
    public val reusePort = supportsReusePort()
    public const val listenBacklog = 128
    public const val maxPipelinedRequests = 64
    public val maxRequestSize = mebibytes(1)
    public val reusableBufferSize = kibibytes(64)
    public val socketReadBufferSize = getDefaultSocketReadBufferSize()
    public val socketWriteBufferSize = getDefaultSocketWriteBufferSize()
    public val useDirectByteBuffers = true

    private fun getDefaultSocketReadBufferSize(): Int {
        val tmpSocket = SocketChannel.open()
        val readBufferSize = tmpSocket.socket().receiveBufferSize
        tmpSocket.close()
        return readBufferSize
    }

    private fun getDefaultSocketWriteBufferSize(): Int {
        val tmpSocket = SocketChannel.open()
        val writeBufferSize = tmpSocket.socket().sendBufferSize
        tmpSocket.close()
        return writeBufferSize
    }

    private fun supportsReusePort(): Boolean {
        val tmpSocket = ServerSocketChannel.open()
        val success = ReusePort.setReusePort(tmpSocket)
        tmpSocket.close()
        return success
    }
}

/**
 * Configuration for a HttpServer.
 * @param address : The address to bind to.
 * @param workerCount : The number of worker threads to utilize, should likely be the number of cores available
 * @param sendServerHeader : If true, the Server header is automatically sent with each response
 * @param sendDateHeader : If true, the Date header is automatically sent with each response
 * @param serverName : The value to send in the Server response header if sendServerHeader is true
 * @param reusableBufferSize : The size of pooled read/write buffers, should be at least as large as the average expected request
 * @param socketReadBufferSize : The size of read buffers for client socket connections, very large or small values may be ignored by the underlying os implementation
 * @param socketWriteBufferSize : The size of write buffers for client socket connections, very large or small values may be ignored by the underlying os implementation
 * @param reusePort : If true, the SO_REUSEPORT socket option is used and each worker uses a dedicated socket
 * @param listenBacklog : The value for the accept queue for the server socket
 * @param acceptGrouping : How many connections should be accepted in a batch, usually equal to the listen backlog
 * @param maxPipelinedRequests : The maximum number of http requests allowed to be pipelined on a single connection
 * @param maxRequestSize : The maximum acceptable size of a single http request
 * @param useDirectByteBuffers : Whether socket read/write buffers should be direct ByteBuffers
 */
public open class HttpServerConfig(val address: InetSocketAddress,
                                   workerCount: Int = HttpServerConfigDefaultValues.workerCount,
                                   val sendServerHeader: Boolean = HttpServerConfigDefaultValues.sendServerHeader,
                                   val sendDateHeader: Boolean = HttpServerConfigDefaultValues.sendDateHeader,
                                   serverName: String = HttpServerConfigDefaultValues.serverName,
                                   reusableBufferSize: Int = HttpServerConfigDefaultValues.reusableBufferSize,
                                   socketReadBufferSize: Int = HttpServerConfigDefaultValues.socketReadBufferSize,
                                   socketWriteBufferSize: Int = HttpServerConfigDefaultValues.socketWriteBufferSize,
                                   reusePort: Boolean = HttpServerConfigDefaultValues.reusePort,
                                   listenBacklog: Int = HttpServerConfigDefaultValues.listenBacklog,
                                   acceptGrouping: Int = listenBacklog,
                                   maxPipelinedRequests: Int = HttpServerConfigDefaultValues.maxPipelinedRequests,
                                   maxRequestSize: Int = HttpServerConfigDefaultValues.maxRequestSize,
                                   val useDirectByteBuffers: Boolean = HttpServerConfigDefaultValues.useDirectByteBuffers) {
    private val logger = LoggingPlugin.get(javaClass)
    public val workerCount = validateWorkerCount(workerCount)
    public val serverName = validateServerName(serverName)
    public val reusableBufferSize = validateIsPositive("reusableBufferSize", reusableBufferSize, HttpServerConfigDefaultValues.reusableBufferSize)
    public val socketReadBufferSize = validateIsPositive("socketReadBufferSize", socketReadBufferSize, HttpServerConfigDefaultValues.socketReadBufferSize)
    public val socketWriteBufferSize = validateIsPositive("socketWriteBufferSize", socketWriteBufferSize, HttpServerConfigDefaultValues.socketWriteBufferSize)
    public val listenBacklog = validateIsPositive("listenBacklog", listenBacklog, HttpServerConfigDefaultValues.listenBacklog)
    public val acceptGrouping = validateIsPositive("acceptGrouping", acceptGrouping, HttpServerConfigDefaultValues.listenBacklog)
    public val reusePort = validateReusePort(reusePort)
    public val maxPipelinedRequests = validateMaxPipelinedRequests(maxPipelinedRequests)
    public val maxRequestSize = validateIsPositive("maxRequestSize", maxRequestSize, HttpServerConfigDefaultValues.maxRequestSize)

    private fun validateIsPositive(name: String,
                                   value: Int,
                                   default: Int): Int {
        if (value < 1) {
            logger.warn { "$name set to ($value), but must be greater than zero. Using default: ($default)" }
            return default
        }
        else {
            return value
        }
    }

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
                "maxRequestSize($maxRequestSize), " +
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
}
