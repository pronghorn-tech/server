package tech.pronghorn.util.finder

import tech.pronghorn.http.isEqual
import java.nio.ByteBuffer
import java.util.*

internal class SequentialFinder<T : ByteBacked>(private val toLookup: Array<T>): ByteBackedFinder<T> {
    private val maxLength = toLookup.map(ByteBacked::bytes).map { b -> b.size }.max() ?: 0

    override fun find(buffer: ByteBuffer, offset: Int, size: Int): T? {
        if(size > maxLength){
            return null
        }

        var x = 0
        while(x < toLookup.size){
            if(isEqual(toLookup[x].bytes, buffer, offset, size)){
                return toLookup[x]
            }
            x += 1
        }
        return null
    }

    override fun find(bytes: ByteArray): T? {
        if (bytes.size > maxLength) {
            return null
        }

        var x = 0
        while(x < toLookup.size){
            if(Arrays.equals(toLookup[x].bytes, bytes)){
                return toLookup[x]
            }
            x += 1
        }
        return null
    }

}
