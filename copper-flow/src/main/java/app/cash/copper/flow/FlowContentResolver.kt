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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
