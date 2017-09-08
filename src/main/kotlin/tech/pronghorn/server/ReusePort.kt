package tech.pronghorn.server

import java.nio.channels.ServerSocketChannel
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod

private const val SO_REUSEPORT = 15

object ReusePort {
    fun setReusePort(serverSocket: ServerSocketChannel): Boolean {
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
