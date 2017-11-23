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

import tech.pronghorn.coroutines.services.ExternalQueueService
import tech.pronghorn.server.HttpServerConnection
import tech.pronghorn.server.HttpServerWorker
import java.nio.channels.*

internal class ServerConnectionCreationService(override val worker: HttpServerWorker) : ExternalQueueService<SocketChannel>() {
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override suspend fun process(socket: SocketChannel) {
        socket.configureBlocking(false)
        socket.socket().tcpNoDelay = true
        socket.socket().keepAlive = true
        val connection = HttpServerConnection(worker, socket)
        worker.addConnection(connection)
    }
}

