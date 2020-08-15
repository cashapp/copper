/*
 * Copyright (C) 2015 Square, Inc.
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
@file:JvmName("RxContentResolver")

package app.cash.copper.rx3

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
import app.cash.copper.ContentResolverQuery
import app.cash.copper.Query
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Optional

/**
 * Create an observable which will notify subscribers with a [query][Query] for
 * execution. Subscribers are responsible for **always** closing [Cursor] instance
 * returned from the [Query].
 *
 * Subscribers will receive an immediate notification for initial data as well as subsequent
 * notifications for when the supplied `uri`'s data changes. Unsubscribe when you no longer
 * want updates to a query.
 *
 * Since content resolver triggers are inherently asynchronous, items emitted from the returned
 * observable use [scheduler] which defaults to [Schedulers.io]. For consistency, the immediate
 * notification sent on subscribe also uses this scheduler. As such, calling
 * [subscribeOn][Observable.subscribeOn] on the returned observable has no effect.
 *
 * Note: To skip the immediate notification and only receive subsequent notifications when data
 * has changed call `skip(1)` on the returned observable.
 *
 * **Warning:** this method does not perform the query! Only by subscribing to the returned
 * [Observable] will the operation occur.
 *
 * @see ContentResolver.query
 * @see ContentResolver.registerContentObserver
 */
@CheckResult
@JvmOverloads
fun ContentResolver.observeQuery(
  uri: Uri,
  projection: Array<String>? = null,
  selection: String? = null,
  selectionArgs: Array<String>? = null,
  sortOrder: String? = null,
  notifyForDescendants: Boolean = false,
  scheduler: Scheduler = Schedulers.io()
): Observable<Query> {
  val query = ContentResolverQuery(this, uri, projection, selection, selectionArgs, sortOrder)
  val queries =
    Observable.create<Query> { e ->
      val observer = object : ContentObserver(mainThread) {
        override fun onChange(selfChange: Boolean) {
          if (!e.isDisposed) {
            e.onNext(query)
          }
        }
      }
      registerContentObserver(uri, notifyForDescendants, observer)
      e.setCancellable { unregisterContentObserver(observer) }
      if (!e.isDisposed) {
        e.onNext(query) // Trigger initial query.
      }
    }
  return queries.observeOn(scheduler)
}

private val mainThread = Handler(Looper.getMainLooper())

/**
 * Execute the query on the underlying database and return an Observable of each row mapped to
 * `T` by `mapper`.
 *
 * Standard usage of this operation is in `flatMap`:
 * ```
 * flatMap(q -> q.asRows(Item.MAPPER).toList())
 * ```
 *
 * However, the above is a more-verbose but identical operation as
 * [mapToList]. This `asRows` method should be used when you need
 * to limit or filter the items separate from the actual query.
 * ```flatMap(q -> q.asRows(Item.MAPPER).take(5).toList())
 * // or...
 * flatMap(q -> q.asRows(Item.MAPPER).filter(i -> i.isActive).toList())
 * ```
 *
 * Note: Limiting results or filtering will almost always be faster in the database as part of
 * a query and should be preferred, where possible.
 *
 * The resulting observable will be empty if `null` is returned from [run].
 */
@CheckResult
fun <T : Any> Query.asRows(mapper: (Cursor) -> T): Observable<T> {
  return Observable.create { e ->
    run()?.use { cursor ->
      while (cursor.moveToNext() && !e.isDisposed) {
        e.onNext(mapper(cursor))
      }
    }
    if (!e.isDisposed) {
      e.onComplete()
    }
  }
}

/**
 * Transforms a query observable returning a single row to a `T` using [mapper].
 *
 * It is an error for a query to pass through this operator with more than 1 row in its result
 * set. Use `LIMIT 1` on the underlying SQL query to prevent this. Result sets with 0 rows
 * emit [default], or do not emit if [default] is null.
 *
 * This operator ignores `null` cursors returned from [run].
 *
 * @param mapper Maps the current [Cursor] row to `T`. May not return null.
 */
@CheckResult
@JvmOverloads
fun <T : Any> ObservableSource<out Query>.mapToOne(
  default: T? = null,
  mapper: (Cursor) -> T
): Observable<T> {
  return QueryToOneObservable(this, mapper, default)
}

/**
 * Transforms a query observable returning a single row to a `Optional<T>` using `mapper`.
 *
 * It is an error for a query to pass through this operator with more than 1 row in its result
 * set. Use `LIMIT 1` on the underlying SQL query to prevent this. Result sets with 0 rows
 * emit [Optional.empty()][Optional.empty].
 *
 * This operator ignores `null` cursors returned from [run].
 *
 * @param mapper Maps the current [Cursor] row to `T`. May not return null.
 */
@RequiresApi(24)
@CheckResult
fun <T : Any> ObservableSource<out Query>.mapToOptional(
  mapper: (Cursor) -> T
): Observable<Optional<T>> {
  return QueryToOptionalObservable(this, mapper)
}

/**
 * Transforms a query observable to a `List<T>` using `mapper`.
 *
 * Be careful using this operator as it will always consume the entire cursor and create objects
 * for each row, every time this observable emits a new query. On tables whose queries update
 * frequently or very large result sets this can result in the creation of many objects.
 *
 * This operator ignores `null` cursors returned from [run].
 *
 * @param mapper Maps the current [Cursor] row to `T`. May not return null.
 */
@CheckResult
fun <T : Any> ObservableSource<out Query>.mapToList(
  mapper: (Cursor) -> T
): Observable<List<T>> {
  return QueryToListObservable(this, mapper)
}
