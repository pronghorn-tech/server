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

import java.net.URLDecoder
import java.util.Arrays
import java.util.Objects

sealed class HttpUrlParseResult

object InvalidHttpUrl : HttpUrlParseResult()

object IncompleteHttpUrl : HttpUrlParseResult()

sealed class HttpUrl : HttpUrlParseResult() {
    abstract fun getPathBytes(): ByteArray
    abstract fun getPath(): String
    abstract fun isSecure(): Boolean?
    abstract fun getHostBytes(): ByteArray?
    abstract fun getHost(): String?
    abstract fun getPort(): Int?
    abstract fun getQueryParams(): List<QueryParam>

    fun getQueryParamAsBoolean(nameBytes: ByteArray): Boolean? = getQueryParams().find { param -> Arrays.equals(nameBytes, param.name) }?.valueAsBoolean()

    fun getQueryParamAsInt(nameBytes: ByteArray): Int? = getQueryParams().find { param -> Arrays.equals(nameBytes, param.name) }?.valueAsInt()

    fun getQueryParamAsLong(nameBytes: ByteArray): Long? = getQueryParams().find { param -> Arrays.equals(nameBytes, param.name) }?.valueAsLong()

    fun getQueryParamAsString(nameBytes: ByteArray): String? = getQueryParams().find { param -> Arrays.equals(nameBytes, param.name) }?.valueAsString()

    fun getQueryParamAsBoolean(name: String): Boolean? = getQueryParams().find { param -> Arrays.equals(name.toByteArray(Charsets.US_ASCII), param.name) }?.valueAsBoolean()

    fun getQueryParamAsInt(name: String): Int? = getQueryParams().find { param -> Arrays.equals(name.toByteArray(Charsets.US_ASCII), param.name) }?.valueAsInt()

    fun getQueryParamAsLong(name: String): Long? = getQueryParams().find { param -> Arrays.equals(name.toByteArray(Charsets.US_ASCII), param.name) }?.valueAsLong()

    fun getQueryParamAsString(name: String): String? = getQueryParams().find { param -> Arrays.equals(name.toByteArray(Charsets.US_ASCII), param.name) }?.valueAsString()


    override fun equals(other: Any?): Boolean {
        return when (other) {
            is HttpUrl -> {
                return Arrays.equals(getPathBytes(), other.getPathBytes()) &&
                        isSecure() == other.isSecure() &&
                        Arrays.equals(getHostBytes(), other.getHostBytes()) &&
                        getPort() == other.getPort() &&
                        getQueryParams() == other.getQueryParams()
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(getPath(), isSecure(), getHost(), getPort(), getQueryParams())
    }

    override fun toString(): String {
        return "[path='${getPath()}',isSecure=${isSecure()},host='${getHost()}',port=${getPort()},queryParams='${getQueryParams()}']"
    }
}

private val rootBytes = byteArrayOf(forwardSlashByte)

class ByteArrayHttpUrl(private val path: ByteArray?,
                       private val isSecure: Boolean? = null,
                       private val host: ByteArray? = null,
                       private val port: Int? = null,
                       private val queryParams: ByteArray? = null,
                       private val pathContainsPercentEncoding: Boolean) : HttpUrl() {
    private val parsedQueryParams by lazy(LazyThreadSafetyMode.NONE) { parseQueryParams() }

    override fun getPathBytes(): ByteArray {
        if (path == null) {
            return rootBytes
        }
        else {
            return path
        }
    }

    override fun getPath(): String {
        if (path == null) {
            return "/"
        }
        val pathString = String(path, Charsets.US_ASCII)
        if (!pathContainsPercentEncoding) {
            return pathString
        }
        else {
            return URLDecoder.decode(pathString, Charsets.UTF_8.name())
        }
    }

    override fun isSecure(): Boolean? = isSecure

    override fun getHostBytes(): ByteArray? = host

    override fun getHost(): String? {
        if (host == null) {
            return null
        }

        return String(host, Charsets.US_ASCII)
    }

    override fun getPort(): Int? = port

    private fun parseQueryParams(): List<QueryParam> {
        if(queryParams == null) {
            return emptyList()
        }

        val params = mutableListOf<QueryParam>()
        var x = 0
        var nameStart = 0
        var valueStart = -1
        while(x < queryParams.size){
            val byte = queryParams[x]
            if(byte == equalsByte){
                valueStart = x + 1
            }
            else if(byte == ampersandByte){
                if(valueStart != -1){
                    val name = Arrays.copyOfRange(queryParams, nameStart, valueStart - 1)
                    val value = Arrays.copyOfRange(queryParams, valueStart, x)
                    params.add(QueryParam(name, value))
                }
                nameStart = x + 1
                valueStart = -1
            }
            x += 1
        }

        if(valueStart != -1){
            val name = Arrays.copyOfRange(queryParams, nameStart, valueStart - 1)
            val value = Arrays.copyOfRange(queryParams, valueStart, x)
            params.add(QueryParam(name, value))
        }

        return params
    }

    override fun getQueryParams(): List<QueryParam> = parsedQueryParams
}

class ValueHttpUrl(private val path: String,
                   private val containsPercentEncoding: Boolean = false,
                   private val isSecure: Boolean? = null,
                   private val host: String? = null,
                   private val port: Int? = null,
                   private val queryParams: List<QueryParam> = emptyList()) : HttpUrl() {

    override fun getPathBytes(): ByteArray = path.toByteArray(Charsets.US_ASCII)

    override fun getPath(): String {
        if (!containsPercentEncoding) {
            return path
        }
        else {
            return URLDecoder.decode(path, Charsets.UTF_8.name())
        }
    }

    override fun isSecure(): Boolean? = isSecure

    override fun getHostBytes(): ByteArray? = host?.toByteArray(Charsets.US_ASCII)

    override fun getHost(): String? = host

    override fun getPort(): Int? = port

    override fun getQueryParams(): List<QueryParam> = queryParams
}
