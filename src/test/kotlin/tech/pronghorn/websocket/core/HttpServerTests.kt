package tech.pronghorn.websocket.core

import com.http.HttpRequest
import com.http.HttpVersion
import com.http.protocol.HttpResponseCode
import com.http.protocol.HttpResponseHeader
import eventually
import mu.KotlinLogging
import org.junit.Test
import tech.pronghorn.coroutines.service.Service
import tech.pronghorn.http.ByteArrayResponseHeaderValue
import tech.pronghorn.http.HttpResponse
import tech.pronghorn.http.HttpResponseHeaderValue
import tech.pronghorn.http.NumericResponseHeaderValue
import tech.pronghorn.stats.StatTracker
import tech.pronghorn.test.CDBTest
import tech.pronghorn.server.HttpConnection
import tech.pronghorn.server.WebServer
import tech.pronghorn.server.WebServerWorker
import tech.pronghorn.server.WebWorker
import tech.pronghorn.server.config.WebServerConfig
import tech.pronghorn.server.core.HttpRequestHandler
import java.net.InetSocketAddress
import java.net.SocketOptions
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpCounterHandler : HttpRequestHandler() {
    private val logger = KotlinLogging.logger {}
    val stats = StatTracker()
    //var requestsHandled = 0L
    val requestsHandled = AtomicLong(0)
    val serverBytes = "Pronghorn".toByteArray(Charsets.US_ASCII)

    val contentBytes = "Hello World!".toByteArray(Charsets.US_ASCII)
    //val headers = mapOf<HttpResponseHeader, ByteArray>()
    val headers = ArrayList<HttpResponseHeaderValue<*>>()

    init {
        headers.add(NumericResponseHeaderValue(HttpResponseHeader.ContentLength, contentBytes.size))
        headers.add(ByteArrayResponseHeaderValue(HttpResponseHeader.Server, serverBytes))
    }

    override suspend fun handleGet(request: HttpRequest): HttpResponse {
        //val now = System.nanoTime()
        //stats.addValue(now - frame.text.toLong())
        requestsHandled.incrementAndGet()
        val tmpHeaders = ArrayList<HttpResponseHeaderValue<*>>()
        tmpHeaders.add(NumericResponseHeaderValue(HttpResponseHeader.ContentLength, contentBytes.size))
        tmpHeaders.add(ByteArrayResponseHeaderValue(HttpResponseHeader.Server, serverBytes))
        return HttpResponse(HttpResponseCode.OK, tmpHeaders, contentBytes, HttpVersion.HTTP11, serverBytes)
//        return HttpResponse(HttpResponseCode.OK, headers, contentBytes, HttpVersion.HTTP11, serverBytes)
    }
}

class FakeConnection(fakeWorker: WebWorker,
                     fakeSocket: SocketChannel,
                     fakeKey: SelectionKey) : HttpConnection(fakeWorker, fakeSocket, fakeKey) {
    override fun handleHandshakeRequest(request: ParsedHttpRequest, handshaker: WebsocketHandshaker): Boolean = true
    override val shouldSendMasked: Boolean = false
    override val requiresMasked: Boolean = false
}


class FakeHttpConnection(fakeWorker: WebWorker,
                         fakeSocket: SocketChannel,
                         fakeKey: SelectionKey) : HttpConnection(fakeWorker, fakeSocket, fakeKey) {
    override fun handleHandshakeRequest(request: ParsedHttpRequest, handshaker: WebsocketHandshaker): Boolean = true
    override val shouldSendMasked: Boolean = false
    override val requiresMasked: Boolean = false
}

class HttpServerTests : CDBTest() {
    val host = "10.0.1.2"
//    val host = "localhost"
    val port = 2648
    val address = InetSocketAddress(host, port)

    /*
     * servers should send requests to the request handler
     */
    @Test
    fun serversHandleRequests() {
        repeat(256) {
            val serverThreadCount = 8
            val clientThreadCount = 2
            val channelCount = 16384

//                val batchSize = 256
//                val batchCount = 128 * 16

            val batchSize = 16
            val batchCount = 128 * 128

            val counterHandlers = mutableListOf<HttpCounterHandler>(HttpCounterHandler())
            val serverConfig = WebServerConfig(address, serverThreadCount)

            val server = WebServer(serverConfig, counterHandlers.first())
            server.start()

            Thread.sleep(10000000)

            val channels = mutableListOf<SocketChannel>()
            try {
                eventually { assertTrue(server.isRunning) }

                val pre = System.currentTimeMillis()
                for (c in 1..channelCount) {
//                    while(c > channels.count(SocketChannel::isConnected) + 64){
                        // no-op
//                    }
                    while(c > server.getConnectionCount() + 64){
                        // no-op
                    }

                    val preA = System.currentTimeMillis()
                    val channel = SocketChannel.open(address)
                    val postA = System.currentTimeMillis()
                    if (postA - preA > 1) {
//                        logger.error("Took ${postA - preA} ms to open.")
                    }

//                    channel.socket().tcpNoDelay = true

                    if(!channel.isConnected || channel.isConnectionPending){
                        TODO()
                    }
//                    val channel = SocketChannel.open()
//                    channel.configureBlocking(false)
//                    channel.socket().keepAlive = false
//                    channel.connect(address)
//                    channel.register(clientSelector, SelectionKey.OP_CONNECT)
                    channels.add(channel)
                }

                val postConnect = System.currentTimeMillis()
                logger.info("Took ${postConnect - pre} ms to queue connections")

                var finishedConnecting = 0

                eventually(Duration.ofSeconds(10)) {
                    assertEquals(channelCount, channels.count { channel -> channel.isConnected })
                    assertEquals(channelCount, server.getConnectionCount())
                }

                val post = System.currentTimeMillis()
                println("Took ${post - pre} ms to accept $channelCount connections")

                val requestBytes = "GET /plaintext HTTP/1.1\r\nHost: server\r\nUser-Agent: Mozilla/5.0 (X11; Linux x86_64) Gecko/20130501 Firefox/30.0 AppleWebKit/600.00 Chrome/30.0.0000.0 Trident/10.0 Safari/600.00\r\nCookie: uid=12345678901234567890; __utma=1.1234567890.1234567890.1234567890.1234567890.12; wd=2560x1600\r\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\nAccept-Language: en-US,en;q=0.5\r\nConnection: keep-alive\r\n\r\n".toByteArray(Charsets.US_ASCII)
//                val requestBytes = "GET /plaintext HTTP/1.1\r\nHost: server\r\nUser-Agent: Mozilla/5.0\r\nCookie: uid=12345678901234567890\r\nAccept: text/html\r\nAccept-Language: en-US,en\r\nConnection: keep-alive\r\n\r\n".toByteArray(Charsets.US_ASCII)

                val clientThreads = mutableListOf<Thread>()

                for (c in 1..clientThreadCount) {
                    val clientThread = thread(start = false) {
                        val clientID = c - 1
                        val writeBuffer = ByteBuffer.allocate(batchSize * requestBytes.size)
                        var y = 0

                        var x = 0
                        while (x < batchSize) {
                            writeBuffer.put(requestBytes)
                            x += 1
                        }
                        writeBuffer.flip()

                        while (y < batchCount) {
//                            var x = 0

                            val id = ((y % (channelCount / clientThreadCount)) * clientThreadCount) + clientID

//                            while (x < batchSize) {
//                                writeBuffer.put(requestBytes)
//                                x += 1
//                            }
//                            writeBuffer.flip()
                            val wrote = channels[id].write(writeBuffer)
                            assertEquals((requestBytes.size * batchSize), wrote)
//                            writeBuffer.clear()
                            writeBuffer.position(0)

                            y += 1
                        }
                    }
                    clientThreads.add(clientThread)
                }

                val totalExpected = (batchSize * batchCount * clientThreadCount)

                val taken = measureTimeMillis {
                    clientThreads.forEach(Thread::start)
                    clientThreads.forEach(Thread::join)
                    val clientsFinished = System.currentTimeMillis()
                    eventually(Duration.ofSeconds(5)) {
                        assertEquals(totalExpected.toLong(), counterHandlers.map { handler -> handler.requestsHandled.get() }.sum())
                    }
                    val serverFinished = System.currentTimeMillis()
                    println("Server took ${serverFinished - clientsFinished} ms longer than clients.")
                }

                Thread.sleep(100)

                counterHandlers.map { handler ->
                    println("End to end latency: min ${handler.stats.minMillis()} avg ${handler.stats.meanMillis()}")
//                        handler.stats.printHistogram()
                }
                val fps = (1000f / taken) * totalExpected
                val bandwidth = (fps * requestBytes.size) / (1024 * 1024)
                logger.warn("Took $taken ms for $totalExpected frames. Effective fps : $fps, Effective bandwidth: $bandwidth MB/s")
            } catch (ex: AssertionError) {
                ex.printStackTrace()
            } finally {
                server.shutdown()
                channels.forEach { it.close() }
                Thread.sleep(100)
            }
        }
    }
}
