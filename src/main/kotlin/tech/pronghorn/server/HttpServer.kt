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

import tech.pronghorn.coroutines.core.CoroutineApplication
import tech.pronghorn.server.config.HttpServerConfig
import tech.pronghorn.server.handlers.HttpRequestHandler
import tech.pronghorn.server.services.*
import java.net.InetSocketAddress
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.locks.ReentrantLock

class HttpServer(val config: HttpServerConfig) : CoroutineApplication<HttpServerWorker>() {
    override val workerCount = config.workerCount
    private val serverSocket by lazy {
        val socket = ServerSocketChannel.open()
        socket.configureBlocking(false)
        socket
    }
    private val acceptLock by lazy { ReentrantLock() }
    private val distributionStrategy by lazy { RoundRobinConnectionDistributionStrategy(workers) }

    private val preStartupHandlerRequests = mutableMapOf<String, () -> HttpRequestHandler>()

    constructor(address: InetSocketAddress) : this(HttpServerConfig(address))

    constructor(host: String,
                port: Int) : this(HttpServerConfig(InetSocketAddress(host, port)))

    override fun spawnWorker(): HttpServerWorker = HttpServerWorker(this, config)

    override fun onStart() {
        logger.info { "Starting server with configuration: $config" }
        if(preStartupHandlerRequests.isNotEmpty()){
            registerUrlHandlerGenerators(preStartupHandlerRequests.toMap())
            preStartupHandlerRequests.clear()
        }
    }

    override fun onShutdown() {
        logger.info { "Shutting down server at ${config.address}." }
    }

    fun getSocketManagerService(worker: HttpServerWorker,
                                selector: Selector): SocketManagerService {
        if (config.reusePort) {
            return MultiSocketManagerService(worker, selector)
        }
        else {
            return SingleSocketManagerService(worker, selector, serverSocket, acceptLock, distributionStrategy)
        }
    }

    fun getConnectionCount(): Int = workers.map(HttpServerWorker::getConnectionCount).sum()

    fun registerUrlHandlerGenerators(handlers: Map<String, () -> HttpRequestHandler>) {
        if(!isRunning){
            handlers.forEach { (url, handlerGenerator) ->
                preStartupHandlerRequests.put(url, handlerGenerator)
            }
        }
        else {
            workers.forEach { worker ->
                worker.sendInterWorkerMessage(RegisterUrlHandlersMessage(handlers))
            }
        }
    }

    fun registerUrlHandlerGenerator(url: String,
                                    handlerGenerator: () -> HttpRequestHandler) {
        registerUrlHandlerGenerators(mapOf(url to handlerGenerator))
    }

    fun registerUrlHandler(url: String,
                           handler: HttpRequestHandler) {
        registerUrlHandlerGenerator(url, { handler })
    }
}
