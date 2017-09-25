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

package tech.pronghorn.server.selectionhandlers

import tech.pronghorn.coroutines.core.ReadWriteSelectionKeyHandler
import tech.pronghorn.plugins.logging.LoggingPlugin
import tech.pronghorn.server.HttpServerConnection

class HttpSocketHandler(private val connection: HttpServerConnection): ReadWriteSelectionKeyHandler<HttpServerConnection> {
    private val logger by lazy(LazyThreadSafetyMode.NONE) { LoggingPlugin.get(javaClass) }
    private val connectionReadServiceQueueWriter = connection.worker.connectionReadServiceQueueWriter
    private val responseWriterServiceQueueWriter = connection.worker.responseWriterServiceQueueWriter

    override fun handleRead() {
        if (!connection.isReadQueued) {
            if (!connectionReadServiceQueueWriter.offer(connection)) {
                logger.warn { "Connection read service is overloaded!" }
                return
            }
            connection.isReadQueued = true
        }
    }

    override fun handleWrite() {
        if (!connection.isWriteQueued) {
            if (!responseWriterServiceQueueWriter.offer(connection)) {
                logger.warn { "Connection write service is overloaded!" }
                return
            }
            connection.isWriteQueued = true
        }
    }
}
