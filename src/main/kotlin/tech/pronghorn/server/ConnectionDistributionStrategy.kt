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

import tech.pronghorn.coroutines.awaitable.queue.ExternalQueue
import tech.pronghorn.server.services.ServerConnectionCreationService
import java.nio.channels.SocketChannel

public abstract class ConnectionDistributionStrategy {
    protected abstract val workers: Set<HttpServerWorker>

    private val writers = LinkedHashMap<HttpServerWorker, ExternalQueue.Writer<SocketChannel>>()

    private fun getWriter(worker: HttpServerWorker): ExternalQueue.Writer<SocketChannel> {
        return worker.getService<ServerConnectionCreationService>()?.getQueueWriter()
                ?: throw IllegalStateException("ServerConnectionCreationService not available")
    }

    protected abstract fun getWorker(): HttpServerWorker

    internal suspend fun distributeConnection(socket: SocketChannel): Boolean {
        val worker = getWorker()
        val workerWriter = writers.getOrPut(worker, { getWriter(worker) })
        return workerWriter.offer(socket)
    }
}

public class RoundRobinConnectionDistributionStrategy(override val workers: Set<HttpServerWorker>) : ConnectionDistributionStrategy() {
    private var lastWorkerID = 0
    private val workerCount = workers.size

    override fun getWorker(): HttpServerWorker {
        return workers.elementAt(lastWorkerID++ % workerCount)
    }
}
