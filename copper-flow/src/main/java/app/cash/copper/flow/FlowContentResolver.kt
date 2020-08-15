/*
 * Copyright (C) 2020 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("FlowContentResolver")

package app.cash.copper.flow

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.CheckResult
import app.cash.copper.ContentResolverQuery
import app.cash.copper.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import java.util.ArrayList

/**
 * Create an observable which will notify subscribers with a [query][Query] for
 * execution. Collectors are responsible for **always** closing [Cursor] instance
 * returned from the [Query].
 *
 * Subscribers will receive an immediate notification for initial data as well as subsequent
 * notifications for when the supplied `uri`'s data changes. Unsubscribe when you no longer
 * want updates to a query.
 *
 * Note: To skip the immediate notification and only receive subsequent notifications when data
 * has changed call `drop(1)` on the returned observable.
 *
 * **Warning:** this method does not perform the query! Only by collecting the returned [Flow] will
 * the operation occur.
 *
 * @see ContentResolver.query
 * @see ContentResolver.registerContentObserver
 */
@CheckResult
fun ContentResolver.observeQuery(
  uri: Uri,
  projection: Array<String>? = null,
  selection: String? = null,
  selectionArgs: Array<String>? = null,
  sortOrder: String? = null,
  notifyForDescendants: Boolean = false
): Flow<Query> {
  val query = ContentResolverQuery(this, uri, projection, selection, selectionArgs, sortOrder)
  return flow {
    emit(query)

    val channel = Channel<Unit>(CONFLATED)
    val observer = object : ContentObserver(mainThread) {
      override fun onChange(selfChange: Boolean) {
        channel.offer(Unit)
      }
    }

    registerContentObserver(uri, notifyForDescendants, observer)
    try {
      for (item in channel) {
        emit(query)
      }
    } finally {
      unregisterContentObserver(observer)
    }
  }
}

private val mainThread = Handler(Looper.getMainLooper())

/**
 * Execute the query on the underlying database and return a flow of each row mapped to
 * `T` by `mapper`.
 *
 * Standard usage of this operation is in `flatMap`:
 * ```
 * flatMap(q -> q.asRows(Item.MAPPER).toList())
 * ```
 *
 * However, the above is a more-verbose but identical operation as
 * [mapToList]. This `asRows` function should be used when you need
 * to limit or filter the items separate from the actual query.
 * ```flatMap(q -> q.asRows(Item.MAPPER).take(5).toList())
 * // or...
 * flatMap(q -> q.asRows(Item.MAPPER).filter(i -> i.isActive).toList())
 * ```
 *
 * Note: Limiting results or filtering will almost always be faster in the database as part of
 * a query and should be preferred, where possible.
 *
 * The resulting flow will be empty if `null` is returned from [Query.run].
 */
@ExperimentalCoroutinesApi // Relies on channelFlow.
@CheckResult
fun <T : Any> Query.asRows(
  dispatcher: CoroutineDispatcher = Dispatchers.IO,
  mapper: (Cursor) -> T
): Flow<T> {
  return channelFlow {
    withContext(dispatcher) {
      run()?.use { cursor ->
        while (cursor.moveToNext()) {
          send(mapper(cursor))
        }
      }
    }
  }.buffer(RENDEZVOUS)
}

/**
 * Transforms a query flow returning a single row to a `T` using [mapper].
 *
 * It is an error for a query to pass through this operator with more than 1 row in its result
 * set. Use `LIMIT 1` on the underlying SQL query to prevent this. Result sets with 0 rows
 * emit [default], or do not emit if [default] is null.
 *
 * This operator ignores `null` cursors returned from [Query.run].
 *
 * @param mapper Maps the current [Cursor] row to `T`. May not return null.
 */
@CheckResult
fun <T : Any> Flow<Query>.mapToOne(
  default: T? = null,
  dispatcher: CoroutineDispatcher = Dispatchers.IO,
  mapper: (Cursor) -> T
): Flow<T> = transform { query ->
  val item = withContext(dispatcher) {
    query.run()?.use { cursor ->
      if (cursor.moveToNext()) {
        val item = mapper(cursor)
        check(!cursor.moveToNext()) { "Cursor returned more than 1 row" }
        item
      } else {
        default
      }
    }
  }
  if (item != null) {
    emit(item)
  }
}

/**
 * Transforms a query flow returning a single row to a `T?` using `mapper`.
 *
 * It is an error for a query to pass through this operator with more than 1 row in its result
 * set. Use `LIMIT 1` on the underlying SQL query to prevent this. Result sets with 0 rows
 * emit null.
 *
 * This operator ignores `null` cursors returned from [Query.run].
 *
 * @param mapper Maps the current [Cursor] row to `T`. May not return null.
 */
@CheckResult
fun <T : Any> Flow<Query>.mapToOneOrNull(
  dispatcher: CoroutineDispatcher = Dispatchers.IO,
  mapper: (Cursor) -> T
): Flow<T?> = transform { query ->
  val (emit, item) = withContext(dispatcher) {
    val cursor = query.run()
    if (cursor == null) {
      false to null
    } else {
      cursor.use {
        val item = if (cursor.moveToNext()) {
          val item = mapper(cursor)
          check(!cursor.moveToNext()) { "Cursor returned more than 1 row" }
          item
        } else {
          null
        }
        true to item
      }
    }
  }
  if (emit) {
    emit(item)
  }
}

/**
 * Transforms a query flow to a `List<T>` using `mapper`.
 *
 * Be careful using this operator as it will always consume the entire cursor and create objects
 * for each row, every time this observable emits a new query. On tables whose queries update
 * frequently or very large result sets this can result in the creation of many objects.
 *
 * This operator ignores `null` cursors returned from [Query.run].
 *
 * @param mapper Maps the current [Cursor] row to `T`. May not return null.
 */
@CheckResult
fun <T : Any> Flow<Query>.mapToList(
  dispatcher: CoroutineDispatcher = Dispatchers.IO,
  mapper: (Cursor) -> T
): Flow<List<T>> = transform { query ->
  val list = withContext(dispatcher) {
    query.run()?.use { cursor ->
      val items = ArrayList<T>(cursor.count)
      while (cursor.moveToNext()) {
        items.add(mapper(cursor))
      }
      items
    }
  }
  if (list != null) {
    emit(list)
  }
}
