package tech.pronghorn.runnable

import eventually
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.time.Duration
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val logger = KotlinLogging.logger {}
    val host = "10.0.0.1"
    val port = 5432
    val address = InetSocketAddress(host, port)

    val clientThreadCount = 3
    val channelCount = 32

    val batchSize = 16
    val batchCount = 128 * 128

    val channels = mutableListOf<SocketChannel>()
    try {
        for (c in 1..channelCount) {
            val channel = SocketChannel.open(address)
            channel.socket().setKeepAlive(true)
            channels.add(channel)
        }

        Thread.sleep(100)

        val requestBytes = "GET /plaintext HTTP/1.1\r\nHost: server\r\nUser-Agent: Mozilla/5.0 (X11; Linux x86_64) Gecko/20130501 Firefox/30.0 AppleWebKit/600.00 Chrome/30.0.0000.0 Trident/10.0 Safari/600.00\r\nCookie: uid=12345678901234567890; __utma=1.1234567890.1234567890.1234567890.1234567890.12; wd=2560x1600\r\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\nAccept-Language: en-US,en;q=0.5\r\nConnection: keep-alive\r\n\r\n".toByteArray(Charsets.US_ASCII)
        //val requestBytes = "GET /plaintext HTTP/1.1\r\nHost: server\r\nUser-Agent: Mozilla/5.0\r\nCookie: uid=12345678901234567890\r\nAccept: text/html\r\nAccept-Language: en-US,en\r\nConnection: keep-alive\r\n\r\n".toByteArray(Charsets.US_ASCII)

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

                    val id = ((y % (channelCount / clientThreadCount)) * clientThreadCount) + clientID
                    val wrote = channels[id].write(writeBuffer)
                    assert(requestBytes.size * batchSize == wrote)
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
            val serverFinished = System.currentTimeMillis()
            println("Server took ${serverFinished - clientsFinished} ms longer than clients.")
        }

        Thread.sleep(100)

        val fps = (1000f / taken) * totalExpected
        val bandwidth = (fps * requestBytes.size) / (1024 * 1024)
        logger.warn("Took $taken ms for $totalExpected frames. Effective fps : $fps, Effective bandwidth: $bandwidth MB/s")
    } catch (ex: AssertionError) {
        ex.printStackTrace()
    } finally {
        channels.forEach { it.close() }
    }
}
