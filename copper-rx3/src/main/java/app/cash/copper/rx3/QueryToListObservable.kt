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
package app.cash.copper.rx3

import android.database.Cursor
import app.cash.copper.Query
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.exceptions.Exceptions
import io.reactivex.rxjava3.observers.DisposableObserver
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import java.util.ArrayList

internal class QueryToListObservable<T : Any>(
  private val upstream: ObservableSource<out Query>,
  private val mapper: (Cursor) -> T
) : Observable<List<T>>() {
  override fun subscribeActual(observer: Observer<in List<T>>) {
    upstream.subscribe(MappingObserver(observer, mapper))
  }

  private class MappingObserver<T : Any>(
    private val downstream: Observer<in List<T>>,
    private val mapper: (Cursor) -> T
  ) : DisposableObserver<Query>() {
    override fun onStart() {
      downstream.onSubscribe(this)
    }

    override fun onNext(query: Query) {
      try {
        val cursor = query.run()
        if (cursor == null || isDisposed) {
          return
        }
        val items = ArrayList<T>(cursor.count)
        cursor.use {
          while (cursor.moveToNext()) {
            items.add(mapper(cursor))
          }
        }
        if (!isDisposed) {
          downstream.onNext(items)
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
