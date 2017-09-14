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

package tech.pronghorn.server.services

import tech.pronghorn.coroutines.awaitable.QueueWriter
import tech.pronghorn.coroutines.service.InternalSleepableService
import tech.pronghorn.server.*
import java.io.IOException
import java.nio.channels.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

sealed class SocketManagerService : InternalSleepableService() {
    abstract protected val serverSocket: ServerSocketChannel
    abstract val acceptSelectionKey: SelectionKey

    suspend override fun run() {
        while (isRunning) {
            accept()
            sleepAsync()
        }
    }

    abstract fun accept()
}

class SingleSocketManagerService(override val worker: HttpServerWorker,
                                 selector: Selector,
                                 override val serverSocket: ServerSocketChannel,
                                 val acceptLock: ReentrantLock,
                                 val distributionStrategy: ConnectionDistributionStrategy) : SocketManagerService() {
    override val acceptSelectionKey: SelectionKey = serverSocket.register(selector, SelectionKey.OP_ACCEPT)
    private val hasStarted = AtomicBoolean(false)
    private val hasShutdown = AtomicBoolean(false)
    private val config = worker.server.config
    private val writers = LinkedHashMap<HttpServerWorker, QueueWriter<SocketChannel>>()

    override fun onStart() {
        super.onStart()
        if (hasStarted.compareAndSet(false, true)) {
            serverSocket.bind(config.address, config.listenBacklog)
        }
    }

    override fun onShutdown() {
        super.onShutdown()
        if (hasShutdown.compareAndSet(false, true)) {
            serverSocket.close()
        }
    }

    override fun accept() {
        if (acceptLock.tryLock()) {
            try {
                logger.debug { "Accepting connections..." }
                var acceptedSocket: SocketChannel? = serverSocket.accept()
                var acceptedCount = 0
                while (acceptedSocket != null) {
                    acceptedCount += 1
                    acceptedSocket.configureBlocking(false)
                    acceptedSocket.socket().tcpNoDelay = true
                    var handled = false
                    while (!handled) {
                        val worker = distributionStrategy.getWorker()
                        val workerWriter = writers.getOrPut(worker, { worker.requestMultiExternalWriter<SocketChannel, ServerConnectionCreationService>() })
                        handled = workerWriter.offer(acceptedSocket)
                    }
                    if (acceptedCount >= config.acceptGrouping) {
                        break
                    }
                    acceptedSocket = serverSocket.accept()
                }
                logger.debug { "Accepted $acceptedCount connections." }
            }
            catch (ex: IOException) {
                // no-op
            }
            finally {
                acceptLock.unlock()
            }
        }
    }
}

class MultiSocketManagerService(override val worker: HttpServerWorker,
                                private val selector: Selector) : SocketManagerService() {
    override val serverSocket: ServerSocketChannel = ServerSocketChannel.open()

    init {
        ReusePort.setReusePort(serverSocket)
        serverSocket.configureBlocking(false)
    }

    override val acceptSelectionKey: SelectionKey = serverSocket.register(selector, SelectionKey.OP_ACCEPT)
    private val config = worker.server.config

    override fun onStart() {
        super.onStart()
        serverSocket.bind(config.address, config.listenBacklog)
    }

    override fun onShutdown() {
        super.onShutdown()
        serverSocket.close()
    }

    override fun accept() {
        logger.debug { "Accepting connections..." }
        var acceptedCount = 0
        var acceptedSocket = serverSocket.accept()
        while (acceptedSocket != null) {
            acceptedCount += 1
            acceptedSocket.configureBlocking(false)
            acceptedSocket.socket().tcpNoDelay = true
            val selectionKey = acceptedSocket.register(selector, SelectionKey.OP_READ)
            val connection = HttpServerConnection(worker, acceptedSocket, selectionKey)
            worker.addConnection(connection)
            selectionKey.attach(connection)
            if (acceptedCount >= config.acceptGrouping) {
                break
            }
            acceptedSocket = serverSocket.accept()
        }
        logger.debug { "Accepted $acceptedCount connections." }
    }

}
//
//class ConnectionAcceptService(override val worker: HttpServerWorker,
//                              private val selector: Selector) : InternalSleepableService() {
//    private val config = worker.server.config
//    val serverSocket = ServerSocketChannel.open()
//
//    init {
//        setReusePort(serverSocket)
//        serverSocket.configureBlocking(false)
//    }
//
//    val acceptSelectionKey = serverSocket.register(selector, SelectionKey.OP_ACCEPT)
//
//    suspend override fun run() {
//        serverSocket.bind(config.address, config.listenBacklog)
//        var accepted = 0
//        while (isRunning) {
//            while (accepted < config.acceptGrouping) {
//                if (!accept()) {
//                    break
//                }
//                accepted += 1
//            }
//
//            sleepAsync()
//            accepted = 0
//        }
//    }
//
//    private fun setReusePort(serverSocket: ServerSocketChannel): Boolean {
//        try {
//            val fdProp = serverSocket::class.declaredMemberProperties.find { field -> field.name == "fd" } ?: return false
//            fdProp.isAccessible = true
//            val fd = fdProp.call(serverSocket)
//
//            val netClass = this.javaClass.classLoader.loadClass("sun.nio.ch.Net").kotlin
//            val setOpt = netClass.declaredFunctions.find { function ->
//                function.name == "setIntOption0"
//            } ?: return false
//            setOpt.isAccessible = true
//            setOpt.javaMethod?.invoke(null, fd, false, 1, SO_REUSEPORT, 1, false)
//            return true
//        }
//        catch (e: Exception) {
//            return false
//        }
//    }
//
//    fun accept(): Boolean {
//        val acceptedSocket = serverSocket.accept() ?: return false
//        acceptedSocket.configureBlocking(false)
//        acceptedSocket.socket().tcpNoDelay = true
//        val selectionKey = acceptedSocket.register(selector, SelectionKey.OP_READ)
//        val connection = HttpServerConnection(worker, acceptedSocket, selectionKey)
//        worker.addConnection(connection)
//        selectionKey.attach(connection)
//        return true
//    }
//}
