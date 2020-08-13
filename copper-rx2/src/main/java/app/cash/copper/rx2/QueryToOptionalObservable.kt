/*
 * Copyright (C) 2017 Square, Inc.
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
package app.cash.copper.rx2

import android.database.Cursor
import androidx.annotation.RequiresApi
import app.cash.copper.rx2.RxContentResolver.Query
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.exceptions.Exceptions
import io.reactivex.observers.DisposableObserver
import io.reactivex.plugins.RxJavaPlugins
import java.util.Optional

@RequiresApi(24)
internal class QueryToOptionalObservable<T>(
  private val upstream: Observable<Query>,
  private val mapper: (Cursor) -> T
) : Observable<Optional<T>>() {
  override fun subscribeActual(observer: Observer<in Optional<T>>) {
    upstream.subscribe(MappingObserver(observer, mapper))
  }

  private class MappingObserver<T>(
    private val downstream: Observer<in Optional<T>>,
    private val mapper: (Cursor) -> T
  ) : DisposableObserver<Query>() {
    override fun onStart() {
      downstream.onSubscribe(this)
    }

    override fun onNext(query: Query) {
      try {
        var item: T? = null
        query.run()?.use { cursor ->
          if (cursor.moveToNext()) {
            item = mapper(cursor)
            if (item == null) {
              downstream.onError(NullPointerException("QueryToOne mapper returned null"))
              return
            }
            check(!cursor.moveToNext()) { "Cursor returned more than 1 row" }
          }
        }
        if (!isDisposed) {
          downstream.onNext(Optional.ofNullable(item))
        }
      } catch (e: Throwable) {
        Exceptions.throwIfFatal(e)
        onError(e)
      }
    }

    override fun onComplete() {
      if (!isDisposed) {
        downstream.onComplete()
      }
    }

    override fun onError(e: Throwable) {
      if (isDisposed) {
        RxJavaPlugins.onError(e)
      } else {
        downstream.onError(e)
      }
    }
  }
}
