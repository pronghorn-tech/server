package tech.pronghorn.stats

import com.google.common.math.IntMath

// Basic statistics tracker which can calculate min, max, mean, standard deviation, and a log 2 histogram in microseconds
class StatTracker {
    var count: Long = 0L
        private set
    var min: Double = Double.MAX_VALUE
        private set
    var max: Double = 0.0
        private set
    private var sum: Double = 0.0
    private var sumOfSquares: Double = 0.0
    private var histogram = LongArray(32)

    fun addValue(rawValue: Long) {
        val value: Int = if (rawValue < 0) {
            0
        }
        else {
            (rawValue / 1000).toInt()
        }

        count += 1
        sum += value
        sumOfSquares += value * value
        if (value < min) {
            min = value.toDouble()
        }
        if (value > max) {
            max = value.toDouble()
        }
        val bucket = 32 - Integer.numberOfLeadingZeros(value)
        histogram[bucket] = histogram[bucket] + 1

    }

    fun clear() {
        count = 0L
        sum = 0.0
        sumOfSquares = 0.0
        min = Double.MAX_VALUE
        max = 0.0
        var x = 0
        while (x < histogram.size) {
            histogram[x] = 0
            x++
        }
    }

    fun mean(): Double {
        if (count == 0L) {
            return 0.0
        }
        else {
            return sum / count
        }
    }

    fun average() = mean()

    fun standardDeviation(): Double {
        if (count == 0L) {
            return 0.0
        }
        else {
            val mean = sum / count
            val meanSquared = mean * mean
            val meanSumOfSquares = sumOfSquares / count

            if (meanSumOfSquares <= meanSquared) {
                // This case is technically impossible, but it is most likely the result of floating-point error. If the
                // numbers are so close enough to have to think about floating-point error, just pretend it is 0.
                return 0.0
            }
            else {
                return Math.sqrt(meanSumOfSquares - meanSquared)
            }
        }
    }

    fun minMillis() = Math.round(min / 1000)

    fun maxMillis() = Math.round(max / 1000)

    fun meanMillis() = Math.round(mean() / 1000)

    private fun getHistogramRange(lower: Int,
                                  upper: Int): String {
        if (upper > 1000000) {
            return "${lower / 1000000}-${upper / 1000000} s".padStart(10, ' ')
        }
        else if (upper > 1000) {
            return "${lower / 1000}-${upper / 1000} ms".padStart(10, ' ')
        }
        else {
            return "$lower-$upper μs".padStart(10, ' ')
        }
    }

    fun printHistogram() {
        val first = histogram.indexOfFirst { it > 0 }
        val last = histogram.indexOfLast { it > 0 }
        var x = first
        while (x <= last) {
            val percent = (histogram[x] * 100) / count
            val lower = IntMath.pow(2, x)
            val upper = Math.max(lower * 2, 1)
            println("${getHistogramRange(lower, upper)} : ${"*".repeat(Math.max(percent.toInt(), 1))}")
            x += 1
        }
    }
}
