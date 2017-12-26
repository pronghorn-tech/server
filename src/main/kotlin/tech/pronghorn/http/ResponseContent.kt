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

package tech.pronghorn.http

import tech.pronghorn.http.protocol.*
import tech.pronghorn.server.bufferpools.ManagedByteBuffer
import tech.pronghorn.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.CRC32
import java.util.zip.Deflater

public sealed class ResponseContent {
    companion object {
        public fun from(byteArray: ByteArray): BufferedResponseContent = ByteArrayResponseContent(byteArray)

        public fun from(buffer: ByteBuffer): BufferedResponseContent = ByteBufferResponseContent(buffer)

        public fun from(managed: ManagedByteBuffer): BufferedResponseContent = ManagedByteBufferResponseContent(managed)

        public fun from(string: String,
                        charset: Charset): BufferedResponseContent = ByteArrayResponseContent(string, charset)

        public fun from(string: String): BufferedResponseContent = ByteArrayResponseContent(string, Charsets.US_ASCII)

        public fun from(fileChannel: FileChannel): DirectToSocketResponseContent = FileChannelResponseContent(fileChannel)
    }

    protected fun getOffset(remaining: Long): Long = size - remaining

    abstract val size: Long
}

public sealed class BufferedResponseContent : ResponseContent() {
    public abstract fun writeToBuffer(buffer: ByteBuffer,
                                      remaining: Long = 0): Long
}

public sealed class DirectToSocketResponseContent : ResponseContent() {
    public abstract fun writeToSocket(socket: SocketChannel,
                                      remaining: Long): Long
}

public class Chunk(private val content: BufferedResponseContent) : BufferedResponseContent() {
    private val chunkSizeAsHexSize = stringLengthAsHexOfLong(content.size) + 2
    private val contentSizeWithFooter = content.size + 2
    override val size = chunkSizeAsHexSize + contentSizeWithFooter

    override tailrec fun writeToBuffer(buffer: ByteBuffer,
                                       remaining: Long): Long {
        if (remaining > contentSizeWithFooter) {
            val sizeRemaining = (remaining - contentSizeWithFooter).toInt()
            if (sizeRemaining == chunkSizeAsHexSize && buffer.remaining() >= sizeRemaining) {
                writeLongAsHexToBuffer(content.size, buffer)
                buffer.putShort(carriageReturnNewLineShort)
                return writeToBuffer(buffer, remaining - chunkSizeAsHexSize)
            }
            else {
                val sizeBytes = ByteArray(chunkSizeAsHexSize)
                writeLongAsHexToByteArray(content.size, sizeBytes)
                sizeBytes[chunkSizeAsHexSize - 2] = carriageReturnByte
                sizeBytes[chunkSizeAsHexSize - 1] = newLineByte
                val writableSize = Math.min(sizeRemaining, buffer.remaining())
                buffer.put(sizeBytes, chunkSizeAsHexSize - sizeRemaining, writableSize)
                val remainingAfterWrite = remaining - writableSize
                if (remainingAfterWrite == contentSizeWithFooter) {
                    return writeToBuffer(buffer, remainingAfterWrite)
                }
                else {
                    return remainingAfterWrite
                }
            }
        }
        else if (remaining <= 2) {
            if (remaining == 2L) {
                if (buffer.remaining() >= 2) {
                    buffer.putShort(carriageReturnNewLineShort)
                    return 0
                }
                else {
                    buffer.put(carriageReturnByte)
                    return 1
                }
            }
            else {
                buffer.put(newLineByte)
                return 0
            }
        }
        else {
            val newRemaining = content.writeToBuffer(buffer, remaining - 2) + 2
            if (newRemaining == 2L && buffer.remaining() >= 2) {
                buffer.putShort(carriageReturnNewLineShort)
                return 0
            }
            else if (buffer.hasRemaining()) {
                return writeToBuffer(buffer, newRemaining)
            }
            else {
                return newRemaining
            }
        }
    }
}

public abstract class ChunkedResponseContent : BufferedResponseContent() {
    companion object {
        private val finalChunk = Chunk(EmptyResponseContent)
    }

    override val size = -1L
    private var currentChunk: Chunk? = null

    protected fun finalChunk(): Chunk = finalChunk

    private fun getChunk(): Chunk {
        val chunk = currentChunk ?: nextChunk()
        if (currentChunk == null) {
            currentChunk = chunk
        }
        return chunk
    }

    override tailrec fun writeToBuffer(buffer: ByteBuffer,
                                       remaining: Long): Long {
        val chunk = getChunk()
        val actualRemaining = if (remaining == -1L) chunk.size else remaining
        val newRemaining = chunk.writeToBuffer(buffer, actualRemaining)
        if (newRemaining > 0) {
            return newRemaining
        }
        else if (chunk == finalChunk) {
            return 0
        }
        else {
            val next = nextChunk()
            currentChunk = next
            if (buffer.hasRemaining()) {
                return writeToBuffer(buffer, next.size)
            }
            else {
                return next.size
            }
        }
    }

    internal abstract fun nextChunk(): Chunk
}

public class GzipFileResponseContent(path: Path) : ChunkedResponseContent() {
    private val fileChannel = FileChannel.open(path, StandardOpenOption.READ)
    private val deflater = Deflater(Deflater.BEST_SPEED, true)
    private val bufferSize = kibibytes(16)
    private val inputBuffer = ByteBuffer.allocate(bufferSize)
    private val outputBuffer = ByteBuffer.allocate(bufferSize)
    private val outputArray = outputBuffer.array()
    private val inputArray = inputBuffer.array()
    private val crc = CRC32()
    private var headerWritten = false
    private var footerWritten = false
    private var finishedRead = false

    override tailrec fun nextChunk(): Chunk {
        outputBuffer.clear()
        if (!headerWritten) {
            headerWritten = true
            outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
            writeGzipHeader(outputBuffer)
            outputBuffer.limit(outputBuffer.position())
            outputBuffer.position(0)
            return Chunk(ResponseContent.from(outputBuffer))
        }

        if (finishedRead) {
            if (!deflater.finished()) {
                deflater.finish()
                val wrote = deflater.deflate(outputBuffer.array(), 0, bufferSize, Deflater.FULL_FLUSH)
                if (wrote != 0) {
                    outputBuffer.limit(wrote)
                    outputBuffer.position(0)
                    return Chunk(ResponseContent.from(outputBuffer))
                }
                else {
                    deflater.end()
                    return nextChunk()
                }
            }
            else {
                if (!footerWritten) {
                    footerWritten = true
                    outputBuffer.putInt(crc.value.toInt())
                    outputBuffer.putInt(fileChannel.size().toInt())
                    outputBuffer.flip()
                    return Chunk(ResponseContent.from(outputBuffer))
                }
                else {
                    fileChannel.close()
                    return finalChunk()
                }
            }
        }

        var wrote = 0
        while (wrote < bufferSize) {
            if (deflater.needsInput()) {
                inputBuffer.clear()
                val read = fileChannel.read(inputBuffer)
                if (read == -1) {
                    finishedRead = true
                    if (wrote > 0) {
                        outputBuffer.limit(wrote)
                        outputBuffer.position(0)
                        return Chunk(ResponseContent.from(outputBuffer))
                    }
                    else {
                        return nextChunk()
                    }
                }
                else {
                    deflater.setInput(inputArray, 0, read)
                    crc.update(inputArray, 0, read)
                }
            }

            wrote += deflater.deflate(outputArray, wrote, bufferSize - wrote)
        }

        outputBuffer.limit(wrote)
        outputBuffer.position(0)
        return Chunk(ResponseContent.from(outputBuffer))
    }
}

public object EmptyResponseContent : BufferedResponseContent() {
    override val size = 0L

    override fun writeToBuffer(buffer: ByteBuffer,
                               remaining: Long): Long = 0L
}

public class ByteArrayResponseContent(private val content: ByteArray) : BufferedResponseContent() {
    override val size = content.size.toLong()

    constructor(body: String,
                charset: Charset) : this(body.toByteArray(charset))

    override fun writeToBuffer(buffer: ByteBuffer,
                               remaining: Long): Long {
        if (content.isEmpty()) {
            return 0
        }
        val writeBytes = Math.min(remaining.toInt(), buffer.remaining())
        buffer.put(content, getOffset(remaining).toInt(), writeBytes)
        return remaining - writeBytes
    }
}

public class ByteBufferResponseContent(private val content: ByteBuffer) : BufferedResponseContent() {
    override val size = content.limit().toLong()

    override fun writeToBuffer(buffer: ByteBuffer,
                               remaining: Long): Long {
        val offset = getOffset(remaining).toInt()
        if (offset == 0 && size <= buffer.remaining()) {
            buffer.put(content)
            return 0
        }
        else {
            val writeBytes = Math.min(remaining.toInt(), buffer.remaining())
            var x = offset
            val end = offset + writeBytes
            while (x < end) {
                buffer.put(content.get(x))
                x += 1
            }
            return remaining - writeBytes
        }
    }
}

public class ManagedByteBufferResponseContent(private val managed: ManagedByteBuffer) : BufferedResponseContent() {
    override val size = managed.buffer.limit().toLong()

    override fun writeToBuffer(buffer: ByteBuffer,
                               remaining: Long): Long {
        val offset = getOffset(remaining).toInt()
        if (offset == 0 && size <= buffer.remaining()) {
            buffer.put(managed.buffer)
            return 0
        }
        else {
            val managedBuffer = managed.buffer
            val writeBytes = Math.min((size - offset).toInt(), managedBuffer.remaining())
            var x = offset
            val end = offset + writeBytes
            while (x < end) {
                buffer.put(managedBuffer.get(x))
                x += 1
            }

            val result = remaining - writeBytes
            if (result == 0L) {
                managed.release()
            }
            return result
        }
    }
}

public class FileResponseContent(path: Path) : DirectToSocketResponseContent() {
    private val fileChannel = FileChannel.open(path, StandardOpenOption.READ)
    override val size = fileChannel.size()

    override fun writeToSocket(socket: SocketChannel,
                               remaining: Long): Long {
        val offset = getOffset(remaining)
        val result = remaining - fileChannel.transferTo(offset, remaining, socket)
        if (result == 0L) {
            fileChannel.close()
        }
        return result
    }
}

public class FileChannelResponseContent(private val fileChannel: FileChannel) : DirectToSocketResponseContent() {
    override val size = fileChannel.size()

    override fun writeToSocket(socket: SocketChannel,
                               remaining: Long): Long {
        val offset = getOffset(remaining)
        val result = remaining - fileChannel.transferTo(offset, remaining, socket)
        if (result == 0L) {
            fileChannel.close()
        }
        return result
    }
}
