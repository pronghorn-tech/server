package tech.pronghorn.websocket.services

//package tech.pronghorn.server.services
//
//import mu.KotlinLogging
//import tech.pronghorn.coroutines.core.CoroutineWorker
//import tech.pronghorn.coroutines.service.IntervalService
//import java.time.Duration
//
//class HandshakeTimeoutService(override val worker: CoroutineWorker,
//                              private val pendingConnections: MutableSet<HttpConnection>,
//                              timeout: Duration) : IntervalService(Duration.ofSeconds(10)) {
//    val milliTimeout = timeout.toMillis()
//    override fun process() {
//        pendingConnections.forEach { connection ->
//            if(connection.isHandshakeComplete){
//                pendingConnections.remove(connection)
//            }
//            else if (connection.connectionTime() > milliTimeout) {
//                connection.close("Handshake timeout of $milliTimeout ms exceeded.")
//            }
//        }
//    }
//
//    override val logger = KotlinLogging.logger {}
//}
