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

package tech.pronghorn.server

import tech.pronghorn.coroutines.core.CoroutineWorker
import tech.pronghorn.coroutines.service.Service
import tech.pronghorn.http.HttpResponseHeaderValuePair
import tech.pronghorn.http.protocol.StandardHttpResponseHeaders
import tech.pronghorn.plugins.concurrentSet.ConcurrentSetPlugin
import tech.pronghorn.server.bufferpools.OneUseByteBufferAllocator
import tech.pronghorn.server.bufferpools.ReusableBufferPoolManager
import tech.pronghorn.server.config.HttpServerConfig
import tech.pronghorn.server.requesthandlers.HttpRequestHandler
import tech.pronghorn.server.services.*
import tech.pronghorn.util.finder.*
import tech.pronghorn.util.ignoreException
import java.nio.ByteBuffer
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Arrays
import java.util.HashMap

class URLHandlerMapping(val url: ByteArray,
                        val handler: HttpRequestHandler) : ByteBacked {
    override val bytes = url
}

class HttpServerWorker(val server: HttpServer,
                       private val config: HttpServerConfig) : CoroutineWorker() {
    private val connections = ConcurrentSetPlugin.get<HttpServerConnection>()
    private val connectionReadService = ConnectionReadService(this)
    private val connectionCreationService = ServerConnectionCreationService(this)
    private val httpRequestHandlerService = HttpRequestHandlerService(this)
    private val responseWriterService = ResponseWriterService(this)
    private val handlers = HashMap<Int, URLHandlerMapping>()
    private val gmt = ZoneId.of("GMT")
    private val commonHeaderCache = calculateCommonHeaderCache()
    private var latestDate = System.currentTimeMillis() / 1000
    private var handlerFinder: ByteBackedFinder<URLHandlerMapping> = FinderGenerator.generateFinder(handlers.values.toTypedArray())
    internal val commonHeaderSize = commonHeaderCache.size
    internal val connectionBufferPool = ReusableBufferPoolManager(config.reusableBufferSize, config.useDirectByteBuffers)
    internal val oneUseByteBufferAllocator = OneUseByteBufferAllocator(config.useDirectByteBuffers)
    internal val connectionReadServiceQueueWriter = connectionReadService.getQueueWriter()
    internal val responseWriterServiceQueueWriter = responseWriterService.getQueueWriter()
    internal val httpRequestHandlerServiceQueueWriter = httpRequestHandlerService.getQueueWriter()

    override val services: List<Service> = listOf(
            connectionReadService,
            connectionCreationService,
            httpRequestHandlerService,
            responseWriterService
    )

    internal fun addConnection(connection: HttpServerConnection) {
        connections.add(connection)
    }

    internal fun removeConnection(connection: HttpServerConnection) {
        connections.remove(connection)
    }

    override fun onShutdown() {
        if(connections.size > 0) {
            logger.info { "Worker closing ${connections.size} connections" }
        }

        ignoreException {
            connections.forEach { it.close("Server is shutting down.") }
        }
    }

    private fun calculateCommonHeaderCache(): ByteArray {
        val dateBytes = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(gmt)).toByteArray(Charsets.US_ASCII)
        val dateHeader = HttpResponseHeaderValuePair(StandardHttpResponseHeaders.Date, dateBytes)
        val serverHeader = HttpResponseHeaderValuePair(StandardHttpResponseHeaders.Server, config.serverName)

        val bufferSize = (if (config.sendDateHeader) dateHeader.length else 0) + (if (config.sendServerHeader) serverHeader.length else 0)
        val buffer = ByteBuffer.allocate(bufferSize)

        if (config.sendServerHeader) {
            serverHeader.writeHeader(buffer)
        }
        if (config.sendDateHeader) {
            dateHeader.writeHeader(buffer)
        }

        return Arrays.copyOf(buffer.array(), buffer.capacity())
    }

    internal fun getCommonHeaders(): ByteArray {
        if (config.sendDateHeader) {
            val now = System.currentTimeMillis() / 1000
            if (latestDate != now) {
                latestDate = now
                val dateBytes = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(gmt)).toByteArray(Charsets.US_ASCII)
                val dateStart = commonHeaderCache.size - dateBytes.size - 2
                var x = 0
                while (x < dateBytes.size) {
                    commonHeaderCache[dateStart + x] = dateBytes[x]
                    x += 1
                }
            }
        }

        return commonHeaderCache
    }

    fun getHandler(urlBytes: ByteArray): HttpRequestHandler? = handlerFinder.find(urlBytes)?.handler

    private fun addUrlHandlers(newHandlers: Map<String, (HttpServerWorker) -> HttpRequestHandler>) {
        newHandlers.forEach { (url, handlerGenerator) ->
            val urlBytes = url.toByteArray(Charsets.US_ASCII)
            val handler = handlerGenerator(this)
            handlers.put(Arrays.hashCode(urlBytes), URLHandlerMapping(urlBytes, handler))
        }

        handlerFinder = FinderGenerator.generateFinder(handlers.values.toTypedArray())
    }

    override fun handleMessage(message: Any): Boolean {
        if (message is RegisterUrlHandlersMessage) {
            addUrlHandlers(message.handlers)
            return true
        }

        return false
    }
}
