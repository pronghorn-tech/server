package tech.pronghorn.websocket.core

import eventually
import mu.KotlinLogging
import org.junit.Test
import tech.pronghorn.http.*
import tech.pronghorn.http.protocol.*
import tech.pronghorn.server.*
import tech.pronghorn.server.config.HttpServerConfig
import tech.pronghorn.server.core.HttpRequestHandler
import tech.pronghorn.stats.StatTracker
import tech.pronghorn.test.CDBTest
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class JsonExample(val message: String)

class HttpCounterHandler : HttpRequestHandler() {
    private val logger = KotlinLogging.logger {}
    var server: HttpServer? = null
    val stats = StatTracker()
    //var requestsHandled = 0L
    val requestsHandled = AtomicLong(0)
    val serverBytes = "Pronghorn".toByteArray(Charsets.US_ASCII)

    val contentBytes = "Hello World!".toByteArray(Charsets.US_ASCII)

    val dateBytes = "Tue, 15 Aug 2017 00:28:37 GMT".toByteArray(Charsets.US_ASCII)

    val staticHeaders = ArrayList<HttpResponseHeaderValue<*>>(8)

    init {
        staticHeaders.add(ByteArrayResponseHeaderValue(HttpResponseHeader.Server, serverBytes))
//        staticHeaders.add(ByteArrayResponseHeaderValue(HttpResponseHeader.ContentType, CommonMimeTypes.ApplicationJson.bytes))
        staticHeaders.add(NumericResponseHeaderValue(HttpResponseHeader.ContentLength, 27))
//        staticHeaders.add(ByteArrayResponseHeaderValue(HttpResponseHeader.Date, dateBytes))
    }

    override suspend fun handleGet(request: HttpRequest): HttpResponse {
//        requestsHandled.incrementAndGet()

        val tmpHeaders = ArrayList<HttpResponseHeaderValue<*>>()
        tmpHeaders.add(NumericResponseHeaderValue(HttpResponseHeader.ContentLength, contentBytes.size))
        tmpHeaders.add(ByteArrayResponseHeaderValue(HttpResponseHeader.Server, serverBytes))

//        tmpHeaders.add(ByteArrayResponseHeaderValue(HttpResponseHeader.ContentType, CommonMimeTypes.ApplicationJson.bytes))
//        tmpHeaders.add(ByteArrayResponseHeaderValue(HttpResponseHeader.Date, dateBytes))

        return HttpResponse(HttpResponseCode.OK, tmpHeaders, contentBytes, HttpVersion.HTTP11, serverBytes, request.connection)

//        val example = JsonExample("Hello, World!")
//        val json = JsonStream.serialize(example)
//        val jsonBytes = json.toByteArray(Charsets.UTF_8)
//
//        return HttpResponse(HttpResponseCode.OK, staticHeaders, jsonBytes, HttpVersion.HTTP11, serverBytes, request.connection)
    }
}

class FakeConnection(fakeWorker: HttpWorker,
                     fakeSocket: SocketChannel,
                     fakeKey: SelectionKey) : HttpConnection(fakeWorker, fakeSocket, fakeKey) {
    override fun handleHandshakeRequest(request: ParsedHttpRequest, handshaker: WebsocketHandshaker): Boolean = true
    override val shouldSendMasked: Boolean = false
    override val requiresMasked: Boolean = false
}


class FakeHttpConnection(fakeWorker: HttpWorker,
                         fakeSocket: SocketChannel,
                         fakeKey: SelectionKey) : HttpConnection(fakeWorker, fakeSocket, fakeKey) {
    override fun handleHandshakeRequest(request: ParsedHttpRequest, handshaker: WebsocketHandshaker): Boolean = true
    override val shouldSendMasked: Boolean = false
    override val requiresMasked: Boolean = false
}

data class ParseTest(val uriString: String,
                     val uri: HttpRequestURI) {
    val bytes = uriString.toByteArray(Charsets.US_ASCII)
    val buffer = ByteBuffer.allocateDirect(bytes.size + 1)
    init {
        buffer.position(1)
        buffer.put(bytes)
        buffer.flip()
        buffer.position(1)
    }
}

class HttpServerTests : CDBTest() {
    val host = "10.0.1.2"
    //    val host = "localhost"
    val port = 2648
    val address = InetSocketAddress(host, port)

    @Test
    fun uriParser() {
//        val foo = "港"
//        val ch: Char = foo.get(0)
//        println(ch.toInt())
//
//        println(foo)
//        System.exit(1)
//


        val tests = arrayOf(
                ParseTest(
                        "/",
                        ValueHttpRequestURI(path = "/")
                ),
                ParseTest(
                        "*",
                        ValueHttpRequestURI(path = "*")
                ),
                ParseTest(
                        "/foo",
                        ValueHttpRequestURI(path = "/foo")
                ),
                ParseTest(
                        "http://name.com",
                        ValueHttpRequestURI(path = "/", isSecure = false, host = "name.com")
                ),
                ParseTest(
                        "http://name.com/",
                        ValueHttpRequestURI(path = "/", isSecure = false, host = "name.com")
                ),
                ParseTest(
                        "https://name.com",
                        ValueHttpRequestURI(path = "/", isSecure = true, host = "name.com")
                ),
                ParseTest(
                        "https://name.com:10",
                        ValueHttpRequestURI(path = "/", isSecure = true, host = "name.com", port = 10)
                ),
                ParseTest(
                        "https://name.com:4000/",
                        ValueHttpRequestURI(path = "/", isSecure = true, host = "name.com", port = 4000)
                ),
                ParseTest(
                        "https://name.com:5000/foo",
                        ValueHttpRequestURI(path = "/foo", isSecure = true, host = "name.com", port = 5000)
                ),
                ParseTest(
                        "/ ",
                        ValueHttpRequestURI(path = "/")
                ),
                ParseTest(
                        "* ",
                        ValueHttpRequestURI(path = "*")
                ),
                ParseTest(
                        "/foo ",
                        ValueHttpRequestURI(path = "/foo")
                ),
                ParseTest(
                        "http://name.com ",
                        ValueHttpRequestURI(path = "/", isSecure = false, host = "name.com")
                ),
                ParseTest(
                        "http://name.com/ ",
                        ValueHttpRequestURI(path = "/", isSecure = false, host = "name.com")
                ),
                ParseTest(
                        "https://name.com ",
                        ValueHttpRequestURI(path = "/", isSecure = true, host = "name.com")
                ),
                ParseTest(
                        "https://name.com:10 ",
                        ValueHttpRequestURI(path = "/", isSecure = true, host = "name.com", port = 10)
                ),
                ParseTest(
                        "https://name.com:4000/ ",
                        ValueHttpRequestURI(path = "/", isSecure = true, host = "name.com", port = 4000)
                ),
                ParseTest(
                        "https://name.com:5000/foo ",
                        ValueHttpRequestURI(path = "/foo", isSecure = true, host = "name.com", port = 5000)
                )/*,
                ParseTest(
                        //"http://見.香港/foo" http://xn--nw2a.xn--j6w193g/
                        "/foo?bar=5",
                        ValueHttpRequestURI(path = "/foo", queryParams = listOf(QueryParam("bar", "5")))
                ),
                ParseTest(
                        "http://joe:pass@name.com",
                        HttpRequestURI(path = "/", schema = "http", host = "name.com", credentials = RequestCredentials("joe", "pass"))
                )*/
        )

        tests.forEach { test ->
            val parsed = parseHttpURI(test.buffer)
            test.buffer.position(1)
            assertEquals(test.uri, parsed)
        }

        /*repeat(64) {
            val pre = System.currentTimeMillis()
            val goal = 10000000
            var x = 0
            while (x < goal) {
                tests.forEach { test ->
//                    val parsed = URI(test.uriString)
                    val parsed = parseHttpURI(test.buffer, x % 1000000 == 0)
                    test.buffer.rewind()
                    x += 1
                    //assertEquals(parsed, test.uri)
                }
            }
            val post = System.currentTimeMillis()
            println("Took ${post - pre} ms to parse $goal urls")
        }*/
    }

    /*
     * servers should send requests to the request handler
     */
    @Test
    fun serversHandleRequests() {
        repeat(256) {
            val serverThreadCount = 1
            val clientThreadCount = 2
            val channelCount = 256

//                val batchSize = 256
//                val batchCount = 128 * 16

            val batchSize = 1
            val batchCount = 128 * 128

            val counterHandler = HttpCounterHandler()
            val serverConfig = HttpServerConfig(address, serverThreadCount)

            val server = HttpServer(serverConfig)

//            var u = 0
//            while(u < 4000){
//                server.registerUrl("/plain$u", counterHandler)
//                u += 1
//            }

            server.registerUrl("/plaintext", counterHandler)

            counterHandler.server = server
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
                    while (c > server.getConnectionCount() + 64) {
                        // no-op
                    }

                    val preA = System.currentTimeMillis()
                    val channel = SocketChannel.open(address)
                    val postA = System.currentTimeMillis()
                    if (postA - preA > 1) {
//                        logger.error("Took ${postA - preA} ms to open.")
                    }

//                    channel.socket().tcpNoDelay = true

                    if (!channel.isConnected || channel.isConnectionPending) {
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
                        assertEquals(totalExpected.toLong(), counterHandler.requestsHandled.get())
                    }
                    val serverFinished = System.currentTimeMillis()
                    println("Server took ${serverFinished - clientsFinished} ms longer than clients.")
                }

                Thread.sleep(100)

                /*counterHandlers.map { handler ->
                    println("End to end latency: min ${handler.stats.minMillis()} avg ${handler.stats.meanMillis()}")
//                        handler.stats.printHistogram()
                }*/
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
