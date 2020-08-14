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

import android.database.Cursor;
import android.util.Log;
import app.cash.copper.Query;
import app.cash.copper.testing.CursorAssert;
import io.reactivex.observers.DisposableObserver;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

class RecordingObserver extends DisposableObserver<Query> {
  private static final Object COMPLETED = "<completed>";
  private static final String TAG = RecordingObserver.class.getSimpleName();

  final BlockingDeque<Object> events = new LinkedBlockingDeque<>();

  @Override public final void onComplete() {
    Log.d(TAG, "onCompleted");
    events.add(COMPLETED);
  }

  @Override public final void onError(Throwable e) {
    Log.d(TAG, "onError " + e.getClass().getSimpleName() + " " + e.getMessage());
    events.add(e);
  }

  @Override public final void onNext(Query value) {
    Log.d(TAG, "onNext " + value);
    events.add(value.run());
  }

  protected Object takeEvent() {
    Object item = events.removeFirst();
    if (item == null) {
      throw new AssertionError("No items.");
    }
    return item;
  }

  public final CursorAssert assertCursor() {
    Object event = takeEvent();
    assertThat(event).isInstanceOf(Cursor.class);
    return new CursorAssert((Cursor) event);
  }

  public final void assertErrorContains(String expected) {
    Object event = takeEvent();
    assertThat(event).isInstanceOf(Throwable.class);
    assertThat(((Throwable) event).getMessage()).contains(expected);
  }

  public final void assertIsCompleted() {
    Object event = takeEvent();
    assertThat(event).isEqualTo(COMPLETED);
  }

  public void assertNoMoreEvents() {
    assertThat(events).isEmpty();
  }
}
