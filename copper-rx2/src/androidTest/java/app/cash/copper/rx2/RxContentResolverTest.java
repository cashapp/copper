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
import android.content.ContentValues;
import android.test.ProviderTestCase2;
import app.cash.copper.testing.TestContentProvider;

import static app.cash.copper.testing.TestContentProvider.AUTHORITY;
import static app.cash.copper.testing.TestContentProvider.KEY;
import static app.cash.copper.testing.TestContentProvider.TABLE;
import static app.cash.copper.testing.TestContentProvider.VALUE;

public final class RxContentResolverTest extends ProviderTestCase2<TestContentProvider> {
  private final RecordingObserver o = new BlockingRecordingObserver();
  private final TestScheduler scheduler = new TestScheduler();

  private ContentResolver contentResolver;

  public RxContentResolverTest() {
    super(TestContentProvider.class, AUTHORITY.getAuthority());
  }

  @Override protected void setUp() throws Exception {
    super.setUp();
    contentResolver = getMockContentResolver();

    getProvider().init(getContext().getContentResolver());
  }

  @Override public void tearDown() {
    o.assertNoMoreEvents();
    o.dispose();
  }

  public void testCreateQueryObservesInsert() {
    RxContentResolver.observeQuery(contentResolver, TABLE, null, null, null, null, false, scheduler)
      .subscribe(o);
    o.assertCursor().isExhausted();

    contentResolver.insert(TABLE, values("key1", "val1"));
    o.assertCursor().hasRow("key1", "val1").isExhausted();
  }

  public void testCreateQueryObservesUpdate() {
    contentResolver.insert(TABLE, values("key1", "val1"));

    RxContentResolver.observeQuery(contentResolver, TABLE, null, null, null, null, false, scheduler)
      .subscribe(o);
    o.assertCursor().hasRow("key1", "val1").isExhausted();

    contentResolver.update(TABLE, values("key1", "val2"), null, null);
    o.assertCursor().hasRow("key1", "val2").isExhausted();
  }

  public void testCreateQueryObservesDelete() {
    contentResolver.insert(TABLE, values("key1", "val1"));

    RxContentResolver.observeQuery(contentResolver, TABLE, null, null, null, null, false, scheduler)
      .subscribe(o);
    o.assertCursor().hasRow("key1", "val1").isExhausted();

    contentResolver.delete(TABLE, null, null);
    o.assertCursor().isExhausted();
  }

  public void testInitialValueAndTriggerUsesScheduler() {
    scheduler.runTasksImmediately(false);

    RxContentResolver.observeQuery(contentResolver, TABLE, null, null, null, null, false, scheduler)
      .subscribe(o);
    o.assertNoMoreEvents();
    scheduler.triggerActions();
    o.assertCursor().isExhausted();

    contentResolver.insert(TABLE, values("key1", "val1"));
    o.assertNoMoreEvents();
    scheduler.triggerActions();
    o.assertCursor().hasRow("key1", "val1").isExhausted();
  }

  private ContentValues values(String key, String value) {
    ContentValues result = new ContentValues();
    result.put(KEY, key);
    result.put(VALUE, value);
    return result;
  }
}
