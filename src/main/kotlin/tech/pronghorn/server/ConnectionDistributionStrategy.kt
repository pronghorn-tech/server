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

import tech.pronghorn.coroutines.awaitable.QueueWriter
import tech.pronghorn.server.services.ServerConnectionCreationService
import java.nio.channels.SocketChannel

abstract class ConnectionDistributionStrategy {
    abstract val workers: Set<HttpServerWorker>

    private val writers = LinkedHashMap<HttpServerWorker, QueueWriter<SocketChannel>>()

    private fun getWriter(worker: HttpServerWorker): QueueWriter<SocketChannel> {
        return worker.getServiceQueueWriter<SocketChannel, ServerConnectionCreationService>()
                ?: throw IllegalStateException("ServerConnectionCreationService not available")
    }

    protected abstract fun getWorker(): HttpServerWorker

    fun distributeConnection(socket: SocketChannel): Boolean {
        val worker = getWorker()
        val workerWriter = writers.getOrPut(worker, { getWriter(worker) })
        return workerWriter.offer(socket)
    }
}

class RoundRobinConnectionDistributionStrategy(override val workers: Set<HttpServerWorker>) : ConnectionDistributionStrategy() {
    private var lastWorkerID = 0
    val workerCount = workers.size

    override fun getWorker(): HttpServerWorker {
        return workers.elementAt(lastWorkerID++ % workerCount)
    }
}
