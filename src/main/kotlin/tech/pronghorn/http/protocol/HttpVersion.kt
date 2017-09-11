package tech.pronghorn.http.protocol

import tech.pronghorn.util.finder.*
import java.nio.ByteBuffer

interface HttpVersion {
    val majorVersion: Int
    val minorVersion: Int
}

class InstanceHttpVersion(override val majorVersion: Int,
                          override val minorVersion: Int) : HttpVersion {
    companion object {
        fun parse(buffer: ByteBuffer,
                  offset: Int,
                  length: Int): InstanceHttpVersion? {
            var majorVersion = 0
            var minorVersion = 0
            var afterColon = false
            var read = 0
            buffer.position(offset)
            while (read < length) {
                if (!buffer.hasRemaining()) {
                    return null
                }

                val byte = buffer.get()
                if (byte == colonByte) {
                    if (majorVersion == 0) {
                        return null
                    }
                    afterColon = true
                }
                else if (byte < 48 || byte > 57) {
                    return null
                }
                else if (!afterColon) {
                    majorVersion = (majorVersion * 10) + (byte - 48)
                }
                else {
                    minorVersion = (minorVersion * 10) + (byte - 48)
                }
                read += 1
            }

            return InstanceHttpVersion(majorVersion, minorVersion)
        }
    }
}

enum class SupportedHttpVersions(val versionName: String,
                                 override val majorVersion: Int,
                                 override val minorVersion: Int) : ByteBacked, HttpVersion {
    HTTP11("HTTP/1.1", 1, 1),
    HTTP10("HTTP/1.0", 1, 0);

    override val bytes: ByteArray = versionName.toByteArray(Charsets.US_ASCII)

    companion object : ByteBackedFinder<HttpVersion> by httpVersionFinder
}

private val httpVersionFinder = FinderGenerator.generateFinder(SupportedHttpVersions.values())
