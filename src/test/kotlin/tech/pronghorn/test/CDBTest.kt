package tech.pronghorn.test

import mu.KotlinLogging
import java.util.*

abstract class CDBTest {
    protected val logger = KotlinLogging.logger {}
    val random by lazy {
        val seed = Random().nextLong()
        println("Random seed: $seed")
        Random(seed)
    }
}
