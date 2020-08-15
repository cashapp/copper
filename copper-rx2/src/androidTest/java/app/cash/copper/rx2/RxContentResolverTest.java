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
import android.test.ProviderTestCase2;
import app.cash.copper.Query;
import app.cash.copper.testing.CursorAssert;
import app.cash.copper.testing.TestContentProvider;
import io.reactivex.observers.TestObserver;

import static app.cash.copper.testing.TestContentProvider.AUTHORITY;
import static app.cash.copper.testing.TestContentProvider.TABLE;
import static app.cash.copper.testing.TestContentProvider.testValues;
import static java.util.Objects.requireNonNull;

public final class RxContentResolverTest extends ProviderTestCase2<TestContentProvider> {
  private ContentResolver contentResolver;

  public RxContentResolverTest() {
    super(TestContentProvider.class, AUTHORITY.getAuthority());
  }

  @Override protected void setUp() throws Exception {
    super.setUp();
    contentResolver = getMockContentResolver();
    getProvider().init(getContext().getContentResolver());
  }

  public void testCreateQueryObservesInsert() {
    TestObserver<Query> o = RxContentResolver.observeQuery(contentResolver, TABLE)
        .test();
    assertCursor(o).isExhausted();

    contentResolver.insert(TABLE, testValues("key1", "val1"));
    assertCursor(o).hasRow("key1", "val1").isExhausted();

    o.dispose();
  }

  public void testCreateQueryObservesUpdate() {
    contentResolver.insert(TABLE, testValues("key1", "val1"));

    TestObserver<Query> o = RxContentResolver.observeQuery(contentResolver, TABLE)
        .test();
    assertCursor(o).hasRow("key1", "val1").isExhausted();

    contentResolver.update(TABLE, testValues("key1", "val2"), null, null);
    assertCursor(o).hasRow("key1", "val2").isExhausted();
  }

  public void testCreateQueryObservesDelete() {
    contentResolver.insert(TABLE, testValues("key1", "val1"));

    TestObserver<Query> o = RxContentResolver.observeQuery(contentResolver, TABLE)
        .test();
    assertCursor(o).hasRow("key1", "val1").isExhausted();

    contentResolver.delete(TABLE, null, null);
    assertCursor(o).isExhausted();
  }

  public void testInitialValueAndTriggerUsesScheduler() throws InterruptedException {
    QueueScheduler scheduler = new QueueScheduler();

    TestObserver<Query> o =
        RxContentResolver.observeQuery(
            contentResolver, TABLE, null, null, null, null, false, scheduler)
                .test();
    o.assertValueCount(0);
    scheduler.awaitRunnable().run();
    assertCursor(o).isExhausted();

    contentResolver.insert(TABLE, testValues("key1", "val1"));
    o.assertValueCount(0);
    scheduler.awaitRunnable().run();
    assertCursor(o).hasRow("key1", "val1").isExhausted();
  }

  private static CursorAssert assertCursor(TestObserver<Query> o) {
    Query query = o.awaitCount(1).assertValueCount(1).values().remove(0);
    return new CursorAssert(requireNonNull(query.run()));
  }
}
