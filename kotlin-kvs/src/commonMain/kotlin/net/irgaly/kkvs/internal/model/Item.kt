package net.irgaly.kkvs.internal.model

data class Item (
    val key: String,
    val type: String,
    val stringValue: String?,
    val longValue: Long?,
    val doubleValue: Double?,
    @Suppress("ArrayInDataClass")
    val bytesValue: ByteArray?,
    val createdAt: Long,
    val lastReadAt: Long,
    val expireAt: Long
) {
    fun isAvailable(now: Long): Boolean {
        return (now < expireAt)
    }

    fun isExpired(now: Long): Boolean {
        return (expireAt <= now)
    }
}
