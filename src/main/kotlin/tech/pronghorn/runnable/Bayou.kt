package tech.pronghorn.runnable

import bayou.http.HttpResponse
import bayou.http.HttpServer
import bayou.http.HttpStatus
import bayou.http.SimpleHttpResponse
import bayou.mime.ContentType
import java.util.*

fun main(args: Array<String>) {
    System.setProperty("bayou.http.server.pipeline", "true") // favor pipelined requests
    System.setProperty("bayou.http.server.fiber", "false") // fiber not needed in this app

    val bytesHelloWorld = "Hello, World!".toByteArray()

    val server = HttpServer { request ->
        SimpleHttpResponse(HttpStatus.c200_OK, ContentType.text_plain, bytesHelloWorld)
    }

    server.conf().setProxyDefaults().port(2648).ip("10.0.1.2")  // disable some non-essential features
    //server.conf().trafficDump(System.out::print);

    server.start()
}
