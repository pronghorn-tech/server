package tech.pronghorn.util.finder

object FinderGenerator {
    fun <T : ByteBacked> generateFinder(toLookup: Array<T>): ByteBackedFinder<T> {
        val byBytes = toLookup.map(ByteBacked::bytes)
        val groupedBySize = byBytes.groupBy { bytes -> bytes.size }
        val maxOfOneLength = groupedBySize.values.map { list -> list.size }.max() ?: 0

        return when {
            byBytes.size < 4 -> SequentialFinder(toLookup)
            maxOfOneLength < 5 -> LengthPartitionedFinder(toLookup)
            else -> DifferentiatedFinder(toLookup)
        }
    }
}

