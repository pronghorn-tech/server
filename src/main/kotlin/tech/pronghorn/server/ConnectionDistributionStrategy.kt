package tech.pronghorn.server

interface ConnectionDistributionStrategy {
    val workers: Set<HttpServerWorker>
    fun getWorker(): HttpServerWorker
}

class RoundRobinConnectionDistributionStrategy(override val workers: Set<HttpServerWorker>) : ConnectionDistributionStrategy {
    private var lastWorkerID = 0
    val workerCount by lazy { workers.size }

    override fun getWorker(): HttpServerWorker {
        return workers.elementAt(lastWorkerID++ % workerCount)
    }
}
