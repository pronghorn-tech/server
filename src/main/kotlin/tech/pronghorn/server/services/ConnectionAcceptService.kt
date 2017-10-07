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

import tech.pronghorn.coroutines.service.InternalSleepableService
import tech.pronghorn.server.*
import tech.pronghorn.server.selectionhandlers.AcceptHandler
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

sealed class SocketManagerService : InternalSleepableService() {
    abstract protected val serverSocket: ServerSocketChannel

    override fun onStart() {
        worker.registerSelectionKeyHandler(serverSocket, AcceptHandler(this), SelectionKey.OP_ACCEPT)
    }

    suspend override fun run() {
        while (isRunning) {
            accept()
            sleepAsync()
        }
    }

    abstract fun accept()
}

class SingleSocketManager(val address: InetSocketAddress,
                          val listenBacklog: Int,
                          val distributionStrategy: ConnectionDistributionStrategy) {
    val serverSocket = ServerSocketChannel.open()
    init {
        serverSocket.configureBlocking(false)
    }
    private val hasStarted = AtomicBoolean(false)
    private val hasShutdown = AtomicBoolean(false)
    val acceptLock = ReentrantLock()

    fun start(){
        if (hasStarted.compareAndSet(false, true)) {
            serverSocket.bind(address, listenBacklog)
        }
    }

    fun shutdown() {
        if (hasShutdown.compareAndSet(false, true)) {
            serverSocket.close()
        }
    }
}

class SingleSocketManagerService(override val worker: HttpServerWorker,
                                 val manager: SingleSocketManager) : SocketManagerService() {
    override val serverSocket: ServerSocketChannel = manager.serverSocket
    private val config = worker.server.config

    override fun onStart() {
        super.onStart()
        manager.start()
    }

    override fun onShutdown() {
        super.onShutdown()
        manager.shutdown()
    }

    override fun accept() {
        if (serverSocket.socket().isBound && manager.acceptLock.tryLock()) {
            try {
                logger.debug { "Accepting connections..." }
                var acceptedSocket: SocketChannel? = serverSocket.accept()
                var acceptedCount = 0
                while (acceptedSocket != null) {
                    acceptedCount += 1
                    var handled = false
                    while (!handled) {
                        handled = manager.distributionStrategy.distributeConnection(acceptedSocket)
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
                manager.acceptLock.unlock()
            }
        }
    }
}

class MultiSocketManagerService(override val worker: HttpServerWorker) : SocketManagerService() {
    override val serverSocket: ServerSocketChannel = ServerSocketChannel.open()

    init {
        ReusePort.setReusePort(serverSocket)
        serverSocket.configureBlocking(false)
    }

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
            acceptedSocket.socket().keepAlive = true

            val connection = HttpServerConnection(worker, acceptedSocket)
            worker.addConnection(connection)
            if (acceptedCount >= config.acceptGrouping) {
                break
            }
            acceptedSocket = serverSocket.accept()
        }
        logger.debug { "Accepted $acceptedCount connections." }
    }
}
