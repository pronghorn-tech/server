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

import tech.pronghorn.http.HttpResponseHeaderValue
import tech.pronghorn.util.finder.ByteBacked

public interface ContentType : ByteBacked {
    public fun getTypeName(): String

    public fun asHeaderValue(): HttpResponseHeaderValue<*>
}

public class InstanceContentType(private val value: ByteArray) : ContentType {
    constructor(name: String) : this(name.toByteArray(Charsets.US_ASCII))

    override val bytes = ByteArray(0)

    override fun getTypeName(): String = value.toString()

    override fun asHeaderValue(): HttpResponseHeaderValue<*> = HttpResponseHeaderValue.valueOf(value)
}


public enum class CommonContentTypes(private val displayName: String,
                                     val extensions: Array<String>) : ContentType {
    AudioAac("audio/aac", arrayOf("aac")),
    AudioOgg("audio/ogg", arrayOf("oga")),
    AudioWav("audio/x-wav", arrayOf("wav")),
    AudioWebm("audio/webm", arrayOf("webm")),
    ApplicationOctetStream("application/octet-stream", arrayOf("bin")),
    ApplicationBzip("application/x-bzip", arrayOf("bz")),
    ApplicationBzip2("application/x-bzip2", arrayOf("bz2")),
    ApplicationJar("application/java-archive", arrayOf("jar")),
    ApplicationJavascript("application/javascript", arrayOf("js")),
    ApplicationJson("application/json", arrayOf("json")),
    ApplicationPdf("application/pdf", arrayOf("pdf")),
    ApplicationRar("application/x-rar-compressed", arrayOf("rar")),
    ApplicationTar("application/x-tar", arrayOf("tar")),
    ApplicationXhtml("application/xhtml+xml", arrayOf("xhtml")),
    ApplicationXml("application/xml", arrayOf("xml")),
    ApplicationZip("application/zip", arrayOf("zip")),
    ApplicationSevenZip("application/x-7z-compressed", arrayOf("7z")),
    FontTrueType("font/ttf", arrayOf("ttf")),
    FontWoff("font/woff", arrayOf("woff")),
    FontWoff2("font/woff2", arrayOf("woff2")),
    ImageGif("image/gif", arrayOf("gif")),
    ImageIco("image/x-icon", arrayOf("ico")),
    ImagePng("image/png", arrayOf("png")),
    ImageSvg("image/svg+xml", arrayOf("svg")),
    ImageTiff("image/tiff", arrayOf("tiff", "tif")),
    ImageWebm("image/webp", arrayOf("webp")),
    ImageJpeg("image/jpeg", arrayOf("jpg", "jpeg")),
    TextCss("text/css", arrayOf("css")),
    TextCsv("text/csv", arrayOf("csv")),
    TextHtml("text/html", arrayOf("html", "htm")),
    TextPlain("text/plain", arrayOf("txt")),
    VideoAvi("video/x-msvideo", arrayOf("avi")),
    VideoMpeg("video/mpeg", arrayOf("mpeg")),
    VideoOgg("video/ogg", arrayOf("ogv")),
    VideoWebm("video/webm", arrayOf("webm"));

    override val bytes: ByteArray = displayName.toByteArray(Charsets.US_ASCII)
    private val headerValue = HttpResponseHeaderValue.valueOf(bytes)

    override fun asHeaderValue(): HttpResponseHeaderValue<*> = headerValue

    override fun getTypeName(): String = displayName

    companion object {
        private val extensionMap = getExtensionsMap()

        public fun getContentTypeFileName(fileName: String): ContentType? = extensionMap.get(fileName.substringAfterLast('.'))

        private fun getExtensionsMap(): Map<String, ContentType> {
            val map = mutableMapOf<String, ContentType>()
            values().forEach { contentType ->
                contentType.extensions.forEach { extension ->
                    map.put(extension, contentType)
                }
            }
            return map
        }
    }
}
