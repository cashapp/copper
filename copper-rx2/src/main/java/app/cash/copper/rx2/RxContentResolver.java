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
package app.cash.copper.rx2;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableOperator;
import io.reactivex.Scheduler;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Function;
import java.util.List;
import java.util.Optional;

/**
 * A lightweight wrapper around {@link ContentResolver} which allows for continuously observing
 * the result of a query.
 */
public final class RxContentResolver {
  @NonNull @CheckResult
  public static RxContentResolver create(ContentResolver contentResolver, Scheduler scheduler) {
    return new RxContentResolver(contentResolver, scheduler);
  }

  final Handler contentObserverHandler = new Handler(Looper.getMainLooper());

  final ContentResolver contentResolver;
  private final Scheduler scheduler;

  private RxContentResolver(ContentResolver contentResolver, Scheduler scheduler) {
    this.contentResolver = contentResolver;
    this.scheduler = scheduler;
  }

  /**
   * Create an observable which will notify subscribers with a {@linkplain Query query} for
   * execution. Subscribers are responsible for <b>always</b> closing {@link Cursor} instance
   * returned from the {@link Query}.
   * <p>
   * Subscribers will receive an immediate notification for initial data as well as subsequent
   * notifications for when the supplied {@code uri}'s data changes. Unsubscribe when you no longer
   * want updates to a query.
   * <p>
   * Since content resolver triggers are inherently asynchronous, items emitted from the returned
   * observable use the {@link Scheduler} supplied to {@link #create}. For
   * consistency, the immediate notification sent on subscribe also uses this scheduler. As such,
   * calling {@link Observable#subscribeOn subscribeOn} on the returned observable has no effect.
   * <p>
   * Note: To skip the immediate notification and only receive subsequent notifications when data
   * has changed call {@code skip(1)} on the returned observable.
   * <p>
   * <b>Warning:</b> this method does not perform the query! Only by subscribing to the returned
   * {@link Observable} will the operation occur.
   *
   * @see ContentResolver#query(Uri, String[], String, String[], String)
   * @see ContentResolver#registerContentObserver(Uri, boolean, ContentObserver)
   */
  @CheckResult @NonNull
  public Observable<Query> createQuery(@NonNull final Uri uri, @Nullable final String[] projection,
      @Nullable final String selection, @Nullable final String[] selectionArgs, @Nullable
      final String sortOrder, final boolean notifyForDescendants) {
    final Query query = new Query() {
      @Override public Cursor run() {
        return contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
      }
    };
    Observable<Query> queries = Observable.create(new ObservableOnSubscribe<Query>() {
      @Override public void subscribe(final ObservableEmitter<Query> e) throws Exception {
        final ContentObserver observer = new ContentObserver(contentObserverHandler) {
          @Override public void onChange(boolean selfChange) {
            if (!e.isDisposed()) {
              e.onNext(query);
            }
          }
        };
        contentResolver.registerContentObserver(uri, notifyForDescendants, observer);
        e.setCancellable(new Cancellable() {
          @Override public void cancel() throws Exception {
            contentResolver.unregisterContentObserver(observer);
          }
        });

        if (!e.isDisposed()) {
          e.onNext(query); // Trigger initial query.
        }
      }
    });
    return queries.observeOn(scheduler);
  }

  /** An executable query. */
  public static abstract class Query {
    /**
     * Creates an {@linkplain ObservableOperator operator} which transforms a query returning a
     * single row to a {@code T} using {@code mapper}. Use with {@link Observable#lift}.
     * <p>
     * It is an error for a query to pass through this operator with more than 1 row in its result
     * set. Use {@code LIMIT 1} on the underlying SQL query to prevent this. Result sets with 0 rows
     * do not emit an item.
     * <p>
     * This operator ignores {@code null} cursors returned from {@link #run()}.
     *
     * @param mapper Maps the current {@link Cursor} row to {@code T}. May not return null.
     */
    @CheckResult @NonNull //
    public static <T> ObservableOperator<T, Query> mapToOne(@NonNull Function<Cursor, T> mapper) {
      return new QueryToOneOperator<>(mapper, null);
    }

    /**
     * Creates an {@linkplain ObservableOperator operator} which transforms a query returning a
     * single row to a {@code T} using {@code mapper}. Use with {@link Observable#lift}.
     * <p>
     * It is an error for a query to pass through this operator with more than 1 row in its result
     * set. Use {@code LIMIT 1} on the underlying SQL query to prevent this. Result sets with 0 rows
     * emit {@code defaultValue}.
     * <p>
     * This operator emits {@code defaultValue} if {@code null} is returned from {@link #run()}.
     *
     * @param mapper Maps the current {@link Cursor} row to {@code T}. May not return null.
     * @param defaultValue Value returned if result set is empty
     */
    @SuppressWarnings("ConstantConditions") // Public API contract.
    @CheckResult @NonNull
    public static <T> ObservableOperator<T, Query> mapToOneOrDefault(
        @NonNull Function<Cursor, T> mapper, @NonNull T defaultValue) {
      if (defaultValue == null) throw new NullPointerException("defaultValue == null");
      return new QueryToOneOperator<>(mapper, defaultValue);
    }

    /**
     * Creates an {@linkplain ObservableOperator operator} which transforms a query returning a
     * single row to a {@code Optional<T>} using {@code mapper}. Use with {@link Observable#lift}.
     * <p>
     * It is an error for a query to pass through this operator with more than 1 row in its result
     * set. Use {@code LIMIT 1} on the underlying SQL query to prevent this. Result sets with 0 rows
     * emit {@link Optional#empty() Optional.empty()}.
     * <p>
     * This operator ignores {@code null} cursors returned from {@link #run()}.
     *
     * @param mapper Maps the current {@link Cursor} row to {@code T}. May not return null.
     */
    @RequiresApi(24) //
    @CheckResult @NonNull //
    public static <T> ObservableOperator<Optional<T>, Query> mapToOptional(
        @NonNull Function<Cursor, T> mapper) {
      return new QueryToOptionalOperator<>(mapper);
    }

    /**
     * Creates an {@linkplain ObservableOperator operator} which transforms a query to a
     * {@code List<T>} using {@code mapper}. Use with {@link Observable#lift}.
     * <p>
     * Be careful using this operator as it will always consume the entire cursor and create objects
     * for each row, every time this observable emits a new query. On tables whose queries update
     * frequently or very large result sets this can result in the creation of many objects.
     * <p>
     * This operator ignores {@code null} cursors returned from {@link #run()}.
     *
     * @param mapper Maps the current {@link Cursor} row to {@code T}. May not return null.
     */
    @CheckResult @NonNull
    public static <T> ObservableOperator<List<T>, Query> mapToList(
        @NonNull Function<Cursor, T> mapper) {
      return new QueryToListOperator<>(mapper);
    }

    /**
     * Execute the query on the underlying database and return the resulting cursor.
     *
     * @return A {@link Cursor} with query results, or {@code null} when the query could not be
     * executed due to a problem with the underlying store. Unfortunately it is not well documented
     * when {@code null} is returned. It usually involves a problem in communicating with the
     * underlying store and should either be treated as failure or ignored for retry at a later
     * time.
     */
    @CheckResult @WorkerThread
    @Nullable
    public abstract Cursor run();

    /**
     * Execute the query on the underlying database and return an Observable of each row mapped to
     * {@code T} by {@code mapper}.
     * <p>
     * Standard usage of this operation is in {@code flatMap}:
     * <pre>{@code
     * flatMap(q -> q.asRows(Item.MAPPER).toList())
     * }</pre>
     * However, the above is a more-verbose but identical operation as
     * {@link Query#mapToList}. This {@code asRows} method should be used when you need
     * to limit or filter the items separate from the actual query.
     * <pre>{@code
     * flatMap(q -> q.asRows(Item.MAPPER).take(5).toList())
     * // or...
     * flatMap(q -> q.asRows(Item.MAPPER).filter(i -> i.isActive).toList())
     * }</pre>
     * <p>
     * Note: Limiting results or filtering will almost always be faster in the database as part of
     * a query and should be preferred, where possible.
     * <p>
     * The resulting observable will be empty if {@code null} is returned from {@link #run()}.
     */
    @CheckResult @NonNull
    public final <T> Observable<T> asRows(final Function<Cursor, T> mapper) {
      return Observable.create(new ObservableOnSubscribe<T>() {
        @Override public void subscribe(ObservableEmitter<T> e) throws Exception {
          Cursor cursor = run();
          if (cursor != null) {
            try {
              while (cursor.moveToNext() && !e.isDisposed()) {
                e.onNext(mapper.apply(cursor));
              }
            } finally {
              cursor.close();
            }
          }
          if (!e.isDisposed()) {
            e.onComplete();
          }
        }
      });
    }
  }
}
