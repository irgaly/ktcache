package io.github.irgaly.kottage.internal

import io.github.irgaly.kottage.KottageEnvironment
import io.github.irgaly.kottage.KottageEventFlow
import io.github.irgaly.kottage.KottageList
import io.github.irgaly.kottage.KottageOptions
import io.github.irgaly.kottage.KottageStorage
import io.github.irgaly.kottage.internal.database.createDatabaseConnection
import io.github.irgaly.kottage.internal.model.ItemEvent
import io.github.irgaly.kottage.internal.model.ItemEventFlow
import io.github.irgaly.kottage.internal.repository.KottageRepositoryFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow

internal class KottageDatabaseManager(
    fileName: String,
    directoryPath: String,
    private val options: KottageOptions,
    private val environment: KottageEnvironment,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val scope: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob())
) {
    private val databaseConnection by lazy {
        createDatabaseConnection(fileName, directoryPath, environment, dispatcher)
    }

    private val calendar get() = environment.calendar
    private val _eventFlow = ItemEventFlow(environment.calendar.nowUnixTimeMillis(), scope)

    private val repositoryFactory by lazy {
        KottageRepositoryFactory(databaseConnection)
    }

    @OptIn(DelicateCoroutinesApi::class)
    val itemRepository = GlobalScope.async(dispatcher, CoroutineStart.LAZY) {
        repositoryFactory.createItemRepository()
    }

    @OptIn(DelicateCoroutinesApi::class)
    val itemListRepository = GlobalScope.async(dispatcher, CoroutineStart.LAZY) {
        repositoryFactory.createItemListRepository()
    }

    @OptIn(DelicateCoroutinesApi::class)
    val itemEventRepository = GlobalScope.async(dispatcher, CoroutineStart.LAZY) {
        repositoryFactory.createItemEventRepository()
    }

    @OptIn(DelicateCoroutinesApi::class)
    val statsRepository = GlobalScope.async(dispatcher, CoroutineStart.LAZY) {
        repositoryFactory.createStatsRepository()
    }

    @OptIn(DelicateCoroutinesApi::class)
    val operator = GlobalScope.async(dispatcher, CoroutineStart.LAZY) {
        KottageOperator(
            itemRepository.await(),
            itemListRepository.await(),
            itemEventRepository.await(),
            statsRepository.await()
        )
    }

    val eventFlow: Flow<ItemEvent> = _eventFlow.flow

    suspend fun getStorageOperator(storage: KottageStorage): KottageStorageOperator {
        return KottageStorageOperator(
            storage,
            operator.await(),
            itemRepository.await(),
            itemListRepository.await(),
            itemEventRepository.await(),
            statsRepository.await()
        )
    }

    suspend fun getListOperator(
        kottageList: KottageList,
        storage: KottageStorage
    ): KottageListOperator {
        return KottageListOperator(
            kottageList,
            storage,
            operator.await(),
            getStorageOperator(storage),
            itemRepository.await(),
            itemListRepository.await(),
            itemEventRepository.await(),
            statsRepository.await()
        )
    }

    suspend fun <R> transactionWithResult(bodyWithReturn: () -> R): R =
        databaseConnection.transactionWithResult(bodyWithReturn)

    suspend fun transaction(body: () -> Unit) = databaseConnection.transaction(body)
    suspend fun deleteAll() {
        databaseConnection.deleteAll()
    }

    suspend fun compact() {
        val operator = operator.await()
        val now = calendar.nowUnixTimeMillis()
        val beforeExpireAt = options.garbageCollectionTimeOfInvalidatedListEntries?.let {
            now - it.inWholeMilliseconds
        }
        databaseConnection.transaction {
            operator.getAllListType { listType ->
                if (beforeExpireAt != null) {
                    // List Entry の自動削除が有効
                    operator.evictExpiredListEntries(
                        listType = listType,
                        now = now,
                        beforeExpireAt = beforeExpireAt
                    )
                } else {
                    // List Entry Invalidate のみ
                    operator.invalidateExpiredListEntries(listType = listType, now = now)
                }
            }
            operator.evictCaches(now)
            operator.evictEvents(now)
            operator.updateLastEvictAt(now)
        }
        databaseConnection.compact()
    }

    suspend fun getDatabaseStatus(): String {
        return databaseConnection.getDatabaseStatus()
    }

    suspend fun backupTo(file: String, directoryPath: String) {
        databaseConnection.backupTo(file, directoryPath)
    }

    /**
     * Publish Events
     */
    suspend fun onEventCreated() {
        val operator = operator.await()
        val limit = 100L
        _eventFlow.updateWithLock { lastEvent, emit ->
            var lastEventTime = lastEvent.time
            var remains = true
            while (remains) {
                val events = databaseConnection.transactionWithResult {
                    operator.getEvents(
                        afterUnixTimeMillisAt = lastEventTime,
                        limit = limit
                    )
                }
                remains = (limit <= events.size)
                events.lastOrNull()?.let {
                    lastEventTime = it.createdAt
                }
                events.forEach {
                    emit(it)
                }
            }
        }
    }

    fun eventFlow(afterUnixTimeMillisAt: Long? = null, itemType: String? = null): KottageEventFlow {
        return KottageEventFlow(
            afterUnixTimeMillisAt,
            itemType,
            _eventFlow,
            databaseConnection,
            operator,
            dispatcher
        )
    }
}
