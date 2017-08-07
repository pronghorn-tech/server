package com.http

import com.http.protocol.HttpMethod
import com.http.protocol.HttpResponseCode
import com.http.protocol.HttpResponseHeader
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.LinkedHashMap

//class PlaintextHandler(server: WebServer) : URLHandler() {
//    val contentBytes = "Hello World".toByteArray(Charsets.US_ASCII)
//    val headers = mapOf<HttpResponseHeader, ByteArray>()
//
//    override fun handle(request: HttpRequest): HttpResponse {
//        return HttpResponse(HttpResponseCode.OK, headers, contentBytes, HttpVersion.HTTP11, )
//    }
//}
//
//class ParseThread(val id: Int,
//                  val count: Int) : Thread() {
//    val buffer = ByteBuffer.allocate(4096)
//    var took: Long = 0
//
//    val server = HttpServer()
//
//    override fun run() {
//        buffer.put("GET /plaintext HTTP/1.1\r\nHost: server\r\nUser-Agent: Mozilla/5.0 (X11; Linux x86_64) Gecko/20130501 Firefox/30.0 AppleWebKit/600.00 Chrome/30.0.0000.0 Trident/10.0 Safari/600.00\r\nCookie: uid=12345678901234567890; __utma=1.1234567890.1234567890.1234567890.1234567890.12; wd=2560x1600\r\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\nAccept-Language: en-US,en;q=0.5\r\nConnection: keep-alive\r\n\r\n".toByteArray(Charsets.US_ASCII))
//        //buffer.put("GET /index.htm?foo=1234&bar=126246 HTTP/1.1\r\nHost: Potato \r\nFoo:     Bar\r\nBleh:Ass   \r\n\r\n".toByteArray(Charsets.US_ASCII))
//        buffer.flip()
//
//        server.registerUrl("/plaintext", PlaintextHandler())
//
//        val start = System.currentTimeMillis()
//        var x = 0
//        var totalSize = 0L
//        while (x < count) {
//            val request = HttpRequestParser.parse(buffer, TODO())!!
//            if (request.method == HttpMethod.GET) {
//                x += 1
//            } else {
//                println("FAILED TO FIND GET")
//                System.exit(1)
//            }
//            val response = server.handleRequest(request)
//            val rendered = server.render(response)
//
//            totalSize += rendered.size
//            buffer.position(0)
//        }
//        val end = System.currentTimeMillis()
//        println("Total size: $totalSize")
//        took = end - start
//    }
//
//    fun output() {
//        val perSecond = Math.round(count / (took / 1000.0))
//        println("Thread $id took $took ms, $perSecond per second")
//    }
//}

fun main(args: Array<String>) {
/*    var loops = 0
    val count = 1000000
    val threadCount = 4

    while (loops < 40) {
        val threads = ArrayList<ParseThread>()
        var t = 0
        while (t < threadCount) {
            threads.add(ParseThread(t, count))
            t += 1
        }

        threads.forEach { thread -> thread.start() }
        threads.forEach { thread -> thread.join() }
        threads.forEach { thread -> thread.output() }

        val avgTaken = threads.map { thread -> thread.took }.sum() / threadCount
        val perSecond = Math.round(count / (avgTaken / 1000.0)) * threadCount
        println("Average taken $avgTaken ms, total $perSecond per second")

        loops += 1
    }*/
}
