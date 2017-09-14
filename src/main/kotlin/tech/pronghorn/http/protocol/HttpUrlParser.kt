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

package tech.pronghorn.http.protocol

import tech.pronghorn.util.sliceToArray
import java.nio.ByteBuffer

val RootURI = ValueHttpUrl("/")
val StarURI = ValueHttpUrl("*")

private const val httpAsInt = 1752462448
private const val doubleSlashAsShort: Short = 12079
private const val secureByte: Byte = 0x73

fun parseHttpUrl(buffer: ByteBuffer): HttpUrlParseResult {
    if (!buffer.hasRemaining()) {
        return IncompleteHttpUrl
    }

    var byte = buffer.get()
    var pathContainsPercentEncoding = false
    var pathStart = -1
    var portStart = -1
    var hostStart = -1
    var queryParamStart = -1
    var pathEnd = -1
    var end = -1
    var port: Int? = null
    var isSecure: Boolean? = null

    if (byte == forwardSlashByte) {
        if (!buffer.hasRemaining()) {
            return RootURI
        }

        pathStart = buffer.position() - 1
        // abs_path
        while (buffer.hasRemaining()) {
            byte = buffer.get()

            if (byte == percentByte) {
                pathContainsPercentEncoding = true
            }
            else if (byte == questionByte) {
                pathEnd = buffer.position() - 1
                queryParamStart = buffer.position()
            }
            else if (byte == spaceByte) {
                end = buffer.position() - 1
                if (end - pathStart == 1) {
                    return RootURI
                }
                break
            }
        }
    }
    else if (byte == asteriskByte) {
        // starURI
        if (!buffer.hasRemaining() || buffer.get() == spaceByte) {
            return StarURI
        }
        else {
            return InvalidHttpUrl
        }
    }
    else {
        buffer.position(buffer.position() - 1)
        // absoluteURI
        if (buffer.remaining() < 4) {
            return IncompleteHttpUrl
        }

        val firstFour = buffer.getInt()
        if (firstFour != httpAsInt) {
            return InvalidHttpUrl
        }

        byte = buffer.get()
        isSecure = byte == secureByte

        if (isSecure) {
            byte = buffer.get()
        }

        if (byte == colonByte) {
            if (buffer.remaining() < 2) {
                return IncompleteHttpUrl
            }
            val slashes = buffer.getShort()
            if (slashes != doubleSlashAsShort) {
                return InvalidHttpUrl
            }
        }
        else {
            return InvalidHttpUrl
        }

        hostStart = buffer.position()

        while (buffer.hasRemaining()) {
            byte = buffer.get()

            if (byte == colonByte) {
                // parse port
                portStart = buffer.position()
                port = 0
                while (buffer.hasRemaining()) {
                    val portByte = buffer.get()
                    if (portByte == forwardSlashByte) {
                        pathStart = buffer.position() - 1
                        break
                    }
                    else if (portByte == spaceByte) {
                        end = buffer.position() - 1
                        break
                    }

                    port = (port!! * 10) + (portByte - 48)
                }
                break
            }
            else if (byte == forwardSlashByte) {
                pathStart = buffer.position() - 1
                break
            }
            else if (byte == spaceByte) {
                end = buffer.position() - 1
                break
            }
        }

        if (end == -1) {
            while (buffer.hasRemaining()) {
                byte = buffer.get()
                if (byte == percentByte) {
                    pathContainsPercentEncoding = true
                }
                else if (byte == questionByte) {
                    pathEnd = buffer.position() - 1
                    queryParamStart = buffer.position()
                    break
                }
                else if (byte == spaceByte) {
                    end = buffer.position() - 1
                    break
                }
            }
        }
    }

    if (end == -1) {
        end = buffer.position()
    }

    if (pathEnd == -1) {
        pathEnd = end
    }

    val path = if (pathStart != -1) {
        buffer.sliceToArray(pathStart, pathEnd - pathStart)
    }
    else {
        null
    }

    val prePath = if (pathStart != -1) {
        pathStart
    }
    else {
        end
    }

    val host = if (hostStart != -1) {
        buffer.sliceToArray(hostStart, if (portStart != -1) portStart - 1 - hostStart else prePath - hostStart)
    }
    else {
        null
    }

    val queryParams = if (queryParamStart != -1) {
        buffer.sliceToArray(queryParamStart, end - queryParamStart)
    }
    else {
        null
    }

    return ByteArrayHttpUrl(
            path = path,
            isSecure = isSecure,
            host = host,
            port = port,
            queryParams = queryParams,
            pathContainsPercentEncoding = pathContainsPercentEncoding
    )
}
