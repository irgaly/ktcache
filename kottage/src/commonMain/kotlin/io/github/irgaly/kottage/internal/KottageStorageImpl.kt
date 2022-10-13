package io.github.irgaly.kottage.internal

import io.github.irgaly.kottage.KottageEntry
import io.github.irgaly.kottage.KottageEvent
import io.github.irgaly.kottage.KottageEventFlow
import io.github.irgaly.kottage.KottageList
import io.github.irgaly.kottage.KottageListOptions
import io.github.irgaly.kottage.KottageOptions
import io.github.irgaly.kottage.KottageStorage
import io.github.irgaly.kottage.KottageStorageOptions
import io.github.irgaly.kottage.internal.encoder.Encoder
import io.github.irgaly.kottage.internal.encoder.encodeItem
import io.github.irgaly.kottage.internal.property.KottageStorageStore
import io.github.irgaly.kottage.platform.KottageCalendar
import io.github.irgaly.kottage.property.KottageStore
import io.github.irgaly.kottage.strategy.KottageStrategy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

internal class KottageStorageImpl(
    override val name: String,
    json: Json,
    override val options: KottageStorageOptions,
    private val kottageOptions: KottageOptions,
    private val databaseManager: KottageDatabaseManager,
    private val calendar: KottageCalendar,
    private val onCompactionRequired: suspend () -> Unit,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : KottageStorage {
    private val encoder = Encoder(json, options.encoder)

    private val strategy: KottageStrategy = options.strategy

    private val itemType: String = name

    private suspend fun operator() = databaseManager.operator.await()

    @OptIn(DelicateCoroutinesApi::class)
    private val storageOperator = GlobalScope.async(dispatcher, CoroutineStart.LAZY) {
        databaseManager.getStorageOperator(this@KottageStorageImpl)
    }

    override val defaultExpireTime: Duration? get() = options.defaultExpireTime

    override suspend fun <T : Any> get(key: String, type: KType): T {
        return getOrNullInternal(key, type)
            ?: throw NoSuchElementException("key = $key, storage name = $name")
    }

    override suspend fun <T : Any> getOrNull(key: String, type: KType): T? {
        return getOrNullInternal(key, type)
    }

    /**
     * @throws ClassCastException when decode failed
     * @throws SerializationException when json decode failed
     */
    private suspend fun <T : Any> getOrNullInternal(
        key: String, type: KType
    ): T? = withContext(dispatcher) {
        val storageOperator = storageOperator.await()
        transactionWithAutoCompaction { operator, now ->
            operator.invalidateExpiredListEntries(now)
            storageOperator.getOrNull(key, now)?.also {
                strategy.onItemRead(key, itemType, now, operator)
            }
        }?.let { encoder.decode(it, type) }
    }

    override suspend fun <T : Any> getEntry(key: String, type: KType): KottageEntry<T> {
        return getEntryOrNullInternal(key, type)
            ?: throw NoSuchElementException("key = $key, storage name = $name")
    }

    override suspend fun <T : Any> getEntryOrNull(key: String, type: KType): KottageEntry<T>? {
        return getEntryOrNullInternal(key, type)
    }

    private suspend fun <T : Any> getEntryOrNullInternal(
        key: String,
        type: KType
    ): KottageEntry<T>? = withContext(dispatcher) {
        val storageOperator = storageOperator.await()
        transactionWithAutoCompaction { operator, now ->
            operator.invalidateExpiredListEntries(now)
            storageOperator.getOrNull(key, now)?.also {
                strategy.onItemRead(key, itemType, now, operator)
            }
        }?.let { KottageEntry(it, type, encoder) }
    }

    override suspend fun exists(key: String): Boolean = withContext(dispatcher) {
        val storageOperator = storageOperator.await()
        transactionWithAutoCompaction { operator, now ->
            operator.invalidateExpiredListEntries(now)
            (storageOperator.getOrNull(key, now) != null)
        }
    }

    override suspend fun <T : Any> put(
        key: String, value: T, type: KType, expireTime: Duration?
    ) = withContext(dispatcher) {
        val storageOperator = storageOperator.await()
        val now = calendar.nowUnixTimeMillis()
        val item =
            encoder.encodeItem(this@KottageStorageImpl, key, value, type, now, expireTime)
        transactionWithAutoCompaction(now) { _, _ ->
            storageOperator.upsertItem(item, now)
        }
        databaseManager.onEventCreated()
    }

    override suspend fun remove(key: String): Boolean = withContext(dispatcher) {
        val storageOperator = storageOperator.await()
        var eventCreated = false
        val exists = transactionWithAutoCompaction { _, now ->
            val exists = storageOperator.exists(key)
            if (exists) {
                storageOperator.deleteItem(key = key, now = now) {
                    eventCreated = true
                }
            }
            exists
        }
        if (eventCreated) {
            databaseManager.onEventCreated()
        }
        exists
    }

    override suspend fun removeAll(key: String): Unit = withContext(dispatcher) {
        val storageOperator = storageOperator.await()
        var eventCreated = false
        transactionWithAutoCompaction { _, now ->
            storageOperator.getAllKeys { key ->
                storageOperator.deleteItem(key = key, now = now) {
                    eventCreated = true
                }
            }
        }
        if (eventCreated) {
            databaseManager.onEventCreated()
        }
    }

    override suspend fun compact() = withContext(dispatcher) {
        val operator = operator()
        val now = calendar.nowUnixTimeMillis()
        databaseManager.transaction {
            var itemsEmpty = false
            var eventsEmpty = false
            operator.invalidateExpiredListEntries(now)
            operator.evictCaches(now, itemType) { _ ->
                itemsEmpty = true
            }
            operator.evictEvents(now, itemType) {
                eventsEmpty = true
            }
            if (itemsEmpty && eventsEmpty) {
                // item と event が空になったら stats 削除
                operator.deleteItemStats(itemType = itemType)
            }
        }
    }

    override suspend fun clear() = withContext(dispatcher) {
        val storageOperator = storageOperator.await()
        val now = calendar.nowUnixTimeMillis()
        databaseManager.transaction {
            storageOperator.clear(now)
        }
    }

    override suspend fun getEvents(
        afterUnixTimeMillisAt: Long, limit: Long?
    ): List<KottageEvent> = withContext(dispatcher) {
        val operator = operator()
        databaseManager.transactionWithResult {
            operator.getEvents(
                afterUnixTimeMillisAt = afterUnixTimeMillisAt,
                itemType = itemType,
                limit = limit
            ).map {
                KottageEvent.from(it)
            }
        }
    }

    override fun eventFlow(afterUnixTimeMillisAt: Long?): KottageEventFlow {
        return databaseManager.eventFlow(afterUnixTimeMillisAt, itemType)
    }

    override fun list(
        name: String,
        optionsBuilder: (KottageListOptions.Builder.() -> Unit)?
    ): KottageList {
        val options = KottageListOptions.Builder(
            itemExpireTime = 30.days
        ).apply {
            optionsBuilder?.invoke(this)
        }.build()
        return KottageListImpl(
            name,
            this,
            encoder,
            options,
            kottageOptions,
            databaseManager,
            calendar,
            onCompactionRequired,
            dispatcher
        )
    }

    override fun <T : Any> property(
        type: KType,
        key: String?,
        expireTime: Duration?,
        defaultValue: () -> T
    ): ReadOnlyProperty<Any?, KottageStore<T>> {
        return ReadOnlyProperty<Any?, KottageStore<T>> { _, property ->
            KottageStorageStore(
                storage = this,
                key = key ?: property.name,
                type = type,
                expireTime = expireTime,
                defaultValue = defaultValue
            )
        }
    }

    override fun <T : Any> nullableProperty(
        type: KType,
        key: String?,
        expireTime: Duration?
    ): ReadOnlyProperty<Any?, KottageStore<T?>> {
        return ReadOnlyProperty<Any?, KottageStore<T?>> { _, property ->
            KottageStorageStore(
                storage = this,
                key = key ?: property.name,
                type = type,
                expireTime = expireTime,
                defaultValue = { null }
            )
        }
    }

    override suspend fun getDebugStatus(): String = withContext(dispatcher) {
        val storageOperator = storageOperator.await()
        databaseManager.transactionWithResult {
            storageOperator.getDebugStatus()
        }
    }

    private suspend fun <R> transactionWithAutoCompaction(
        now: Long? = null,
        bodyWithReturn: (operator: KottageOperator, now: Long) -> R
    ): R {
        val operator = operator()
        val receivedNow = now ?: calendar.nowUnixTimeMillis()
        var compactionRequired = false
        val result = databaseManager.transactionWithResult {
            compactionRequired = operator.getAutoCompactionNeeded(receivedNow)
            bodyWithReturn(operator, receivedNow)
        }
        if (compactionRequired) {
            onCompactionRequired()
        }
        return result
    }
}
