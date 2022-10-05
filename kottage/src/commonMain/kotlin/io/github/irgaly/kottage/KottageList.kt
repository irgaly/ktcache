package io.github.irgaly.kottage

import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KType

/**
 * List operations
 *
 * Item List is implemented by Linked List in Database.
 */
interface KottageList {
    val name: String
    val storage: KottageStorage

    /**
     * @param positionId positionId of first item.
     *                   if positionId is null:
     *                       direction = Forward: get First Page
     *                       direction = Backward: get Last Page
     */
    suspend fun <T : Any> getPageFrom(
        positionId: String?,
        pageSize: Long?,
        type: KType,
        direction: KottageListDirection = KottageListDirection.Forward
    ): KottageListPage<T>

    suspend fun getSize(): Long
    suspend fun <T : Any> getFirst(type: KType): KottageListItem<T>?
    suspend fun <T : Any> getLast(type: KType): KottageListItem<T>?

    suspend fun <T : Any> get(positionId: String, type: KType): KottageListItem<T>?

    /**
     * Get item with iteration.
     */
    @Throws(
        IndexOutOfBoundsException::class,
        CancellationException::class
    )
    suspend fun <T : Any> getByIndex(
        index: Long,
        type: KType,
        direction: KottageListDirection = KottageListDirection.Forward
    ): KottageListItem<T>?

    suspend fun <T : Any> add(
        key: String,
        value: T,
        type: KType,
        metaData: KottageListMetaData? = null
    )

    suspend fun addKey(key: String, metaData: KottageListMetaData? = null)
    suspend fun <T : Any> addAll(values: List<Pair<String, T>>, type: KType)
    suspend fun addKeys(keys: List<String>)
    suspend fun <T : Any> addFirst(
        key: String,
        value: T,
        type: KType,
        metaData: KottageListMetaData? = null
    )

    suspend fun addKeyFirst(key: String, metaData: KottageListMetaData? = null)
    suspend fun <T : Any> addAllFirst(values: List<Pair<String, T>>, type: KType)
    suspend fun addKeysFirst(keys: List<String>)

    @Throws(
        NoSuchElementException::class,
        CancellationException::class
    )
    suspend fun <T : Any> update(positionId: String, key: String, value: T, type: KType)

    @Throws(
        NoSuchElementException::class,
        CancellationException::class
    )
    suspend fun updateKey(positionId: String, key: String)
    suspend fun <T : Any> insertAfter(
        positionId: String,
        key: String,
        value: T,
        type: KType,
        metaData: KottageListMetaData?
    )

    suspend fun insertKeyAfter(positionId: String, key: String, metaData: KottageListMetaData?)
    suspend fun <T : Any> insertAllAfter(positionId: String, values: Pair<String, T>, type: KType)
    suspend fun insertKeysAfter(positionId: String, keys: List<String>)
    suspend fun <T : Any> insertBefore(
        positionId: String,
        key: String,
        value: T,
        type: KType,
        metaData: KottageListMetaData?
    )

    suspend fun insertKeyBefore(positionId: String, key: String, metaData: KottageListMetaData?)
    suspend fun <T : Any> insertAllBefore(positionId: String, values: Pair<String, T>, type: KType)
    suspend fun insertKeysBefore(positionId: String, keys: List<String>)
    suspend fun remove(positionId: String)
}
