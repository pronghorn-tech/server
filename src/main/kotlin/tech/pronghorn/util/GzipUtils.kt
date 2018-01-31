package tech.pronghorn.util

import java.nio.ByteBuffer
import java.util.zip.Deflater

internal const val GZIP_MAGIC: Short = 0x8b1f.toShort()
internal const val GZIP_HEADER_SIZE = 10
internal const val GZIP_FOOTER_SIZE = 8
internal const val GZIP_EXTRA_SIZE = GZIP_HEADER_SIZE + GZIP_FOOTER_SIZE

internal fun writeGzipHeader(buffer: ByteBuffer) {
    buffer.putShort(GZIP_MAGIC)
    buffer.put(Deflater.DEFLATED.toByte())
    buffer.putInt(0)
    buffer.putShort(0)
    buffer.put(0)
}
