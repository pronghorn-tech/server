package tech.pronghorn.websocket.core

import com.http.HttpRequest
import com.http.HttpVersion
import com.http.StringLocation
import com.http.protocol.HttpResponseCode
import com.http.protocol.HttpResponseHeader
import com.jsoniter.output.JsonStream
import eventually
import mu.KotlinLogging
import org.junit.Test
import tech.pronghorn.coroutines.service.Service
import tech.pronghorn.http.ByteArrayResponseHeaderValue
import tech.pronghorn.http.HttpResponse
import tech.pronghorn.http.HttpResponseHeaderValue
import tech.pronghorn.http.NumericResponseHeaderValue
import tech.pronghorn.http.protocol.CommonMimeTypes
import tech.pronghorn.server.*
import tech.pronghorn.stats.StatTracker
import tech.pronghorn.test.CDBTest
import tech.pronghorn.server.config.WebServerConfig
import tech.pronghorn.server.core.HttpRequestHandler
import java.net.InetSocketAddress
import java.net.SocketOptions
import java.net.URI
import java.net.URLDecoder
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

data class JsonExample(val message: String)

class HttpCounterHandler: HttpRequestHandler() {
    private val logger = KotlinLogging.logger {}
    var server: WebServer? = null
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
        return HttpResponse(HttpResponseCode.OK, tmpHeaders, contentBytes, HttpVersion.HTTP11, serverBytes, request.connection)

//        val example = JsonExample("Hello, World!")
//        val json = JsonStream.serialize(example)
//        val jsonBytes = json.toByteArray(Charsets.UTF_8)
//
//        return HttpResponse(HttpResponseCode.OK, staticHeaders, jsonBytes, HttpVersion.HTTP11, serverBytes, request.connection)
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

data class QueryParam(val name: StringLocation,
                      val value: StringLocation)

data class RequestCredentials(val username: StringLocation,
                              val password: StringLocation)

abstract class RequestURI {
    abstract fun getPathBytes(): ByteArray
    abstract fun getPath(): String
    abstract fun isSecure(): Boolean?
    abstract fun getCredentials(): RequestCredentials?
    abstract fun getHostBytes(): ByteArray?
    abstract fun getHost(): String?
    abstract fun getPort(): Int?
    abstract fun getQueryParams(): List<QueryParam>?
}

class StringLocationRequestURI(private val path: StringLocation,
                               private val isSecure: Boolean? = null,
                               private val credentials: StringLocation? = null,
                               private val host: StringLocation? = null,
                               private val port: Int? = null,
                               private val queryParams: StringLocation? = null,
                               private val pathContainsPercentEncoding: Boolean): RequestURI() {
    override fun getPathBytes(): ByteArray = path.bytes

    override fun getPath(): String {
        val pathString = path.toString()
        if(!pathContainsPercentEncoding){
            return pathString
        }
        else {
            return URLDecoder.decode(pathString, Charsets.UTF_8.name())
        }
    }

    override fun isSecure(): Boolean? = isSecure

    override fun getCredentials(): RequestCredentials? {
        if(credentials == null){
            return null
        }

        TODO()
    }

    override fun getHostBytes(): ByteArray? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getHost(): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPort(): Int? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getQueryParams(): List<QueryParam>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class ValueRequestURI(private val path: String,
                      private val containsPercentEncoding: Boolean = false,
                      private val isSecure: Boolean? = null,
                      private val credentials: RequestCredentials? = null,
                      private val host: String? = null,
                      private val port: Int? = null,
                      private val queryParams: List<QueryParam>? = null): RequestURI() {

    override fun getPathBytes(): ByteArray = path.toByteArray(Charsets.US_ASCII)

    override fun getPath(): String {
        if(!containsPercentEncoding) {
            return path
        }
        else {
            return URLDecoder.decode(path, Charsets.UTF_8.name())
        }
    }

    override fun isSecure(): Boolean? = isSecure

    override fun getCredentials(): RequestCredentials? = credentials

    override fun getHostBytes(): ByteArray? = host?.toByteArray(Charsets.US_ASCII)

    override fun getHost(): String? = host

    override fun getPort(): Int? = port

    override fun getQueryParams(): List<QueryParam>? = queryParams
}

data class ParseTest(val uriString: String,
                     val uri: RequestURI)

val RootURI = ValueRequestURI("/")
val StarURI = ValueRequestURI("*")

class HttpServerTests : CDBTest() {
    val host = "10.0.1.2"
    //    val host = "localhost"
    val port = 2648
    val address = InetSocketAddress(host, port)

    fun parse(url: String): RequestURI {
        val bytes = url.toByteArray(Charsets.US_ASCII)
        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.put(bytes)
        buffer.flip()

        val firstByte = buffer.get()

        if(buffer.remaining() == 1){
            when(firstByte) {
                forwardSlashByte -> return RootURI
                asteriskByte -> return StarURI
                else -> TODO()
            }
        }

        var pathContainsPercentEncoding = false
        var credentialsStart = -1
        var pathStart = -1
        var portStart = -1
        var hostStart = -1
        var queryParamStart = -1
        var port: Int? = null
        var isSecure: Boolean? = null

        if(firstByte == forwardSlashByte){
            pathStart = 0
            // abs_path
            while(buffer.hasRemaining()){
                val byte = buffer.get()
                if(byte == percentByte) {
                    pathContainsPercentEncoding = true
                }
                else if(byte == questionByte){
                    queryParamStart = buffer.position()
                    break
                }
            }
        }
        else {
            // absoluteURI
            val httpAsInt = 0
            val doubleSlashAsShort: Short = 0
            val firstFour = buffer.getInt()
            if(firstFour != httpAsInt){
                TODO("Exception")
            }

            val secureByte: Byte = 0x73

            val next = buffer.get()
            if(next == secureByte){
                isSecure = true
            }
            else {
                isSecure = false
                if(next == colonByte){
                    val slashes = buffer.getShort()
                    if(slashes != doubleSlashAsShort){
                        TODO("Exception")
                    }
                }

                hostStart = buffer.position()

                while(buffer.hasRemaining()){
                    val byte = buffer.get()

                    if(byte == colonByte){
                        // parse port
                        portStart = buffer.position()
                        port = 0
                        while(buffer.hasRemaining()){
                            val portByte = buffer.get()
                            if(portByte == forwardSlashByte){
                                break
                            }

                            port = port!! * 10 + (portByte - 48)

                        }
                    }

                    if(byte == forwardSlashByte){
                        break
                    }
                }

                pathStart = buffer.position()

                while(buffer.hasRemaining()){
                    val byte = buffer.get()
                    if(byte == percentByte) {
                        pathContainsPercentEncoding = true
                    }
                    else if(byte == questionByte){
                        queryParamStart = buffer.position()
                        break
                    }
                    else if(byte == atByte){
                        if(!isSecure){
                            TODO("Exception")
                        }

                        credentialsStart = pathStart
                        pathStart = buffer.position()
                    }
                }
            }
        }

        val end = buffer.position()

        val credentials = if(credentialsStart != -1){
            StringLocation(buffer, credentialsStart, pathStart - credentialsStart)
        }
        else {
            null
        }

        val host = if(hostStart != -1){
            StringLocation(buffer, hostStart, if(portStart != -1) portStart else pathStart)
        }
        else {
            null
        }

        val queryParams = if(queryParamStart != -1){
            StringLocation(buffer, queryParamStart, end - queryParamStart)
        }
        else {
            null
        }


        return StringLocationRequestURI(
            path = StringLocation(buffer, pathStart, end),
            credentials = credentials,
            isSecure = isSecure,
            host = host,
            port = port,
            queryParams = queryParams,
            pathContainsPercentEncoding = pathContainsPercentEncoding
        )
    }

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
                        ValueRequestURI(path = "/")
                ),
                ParseTest(
                        "*",
                        ValueRequestURI(path = "*")
                ),
                ParseTest(
                        "/foo",
                        ValueRequestURI(path = "/foo")
                )/*,
                ParseTest(
                        //"http://見.香港/foo" http://xn--nw2a.xn--j6w193g/
                        "/foo?bar=5",
                        RequestURI(path = "/foo", query = listOf(QueryParam("bar", "5")))
                ),
                ParseTest(
                        "http://name.com",
                        RequestURI(path = "/", schema = "http", host = "name.com")
                ),
                ParseTest(
                        "http://name.com/",
                        RequestURI(path = "/", schema = "http", host = "name.com")
                ),
                ParseTest(
                        "http://joe:pass@name.com",
                        RequestURI(path = "/", schema = "http", host = "name.com", credentials = RequestCredentials("joe", "pass"))
                )*/
        )

        tests.forEach { test ->
            assertEquals(parse(test.uriString), test.uri)
        }
    }

    /*
     * servers should send requests to the request handler
     */
    @Test
    fun serversHandleRequests() {
        repeat(256) {
            val serverThreadCount = 4
            val clientThreadCount = 2
            val channelCount = 256

//                val batchSize = 256
//                val batchCount = 128 * 16

            val batchSize = 1
            val batchCount = 128 * 128

            val counterHandler = HttpCounterHandler()
            val serverConfig = WebServerConfig(address, serverThreadCount)

            val server = WebServer(serverConfig, counterHandler)
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
