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

import java.nio.channels.ServerSocketChannel
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod

private const val SO_REUSEPORT = 15

public object ReusePort {
    public fun setReusePort(serverSocket: ServerSocketChannel): Boolean {
        try {
            val fdProp = serverSocket::class.declaredMemberProperties.find { field -> field.name == "fd" } ?: return false
            fdProp.isAccessible = true
            val fd = fdProp.call(serverSocket)

            val netClass = this.javaClass.classLoader.loadClass("sun.nio.ch.Net").kotlin
            val setOpt = netClass.declaredFunctions.find { function ->
                function.name == "setIntOption0"
            } ?: return false
            setOpt.isAccessible = true
            setOpt.javaMethod?.invoke(null, fd, false, 1, SO_REUSEPORT, 1, false)
            return true
        }
        catch (e: Exception) {
            return false
        }
    }
}
