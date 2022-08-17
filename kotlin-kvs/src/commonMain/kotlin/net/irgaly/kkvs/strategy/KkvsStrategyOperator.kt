package net.irgaly.kkvs.strategy

interface KkvsStrategyOperator {
    suspend fun updateItemLastRead(key: String, now: Long)
}
