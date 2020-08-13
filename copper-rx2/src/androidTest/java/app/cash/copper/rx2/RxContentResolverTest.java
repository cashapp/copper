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
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentProvider;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RxContentResolverTest
    extends ProviderTestCase2<RxContentResolverTest.TestContentProvider> {
  private static final Uri AUTHORITY = Uri.parse("content://test_authority");
  private static final Uri TABLE = AUTHORITY.buildUpon().appendPath("test_table").build();
  private static final String KEY = "test_key";
  private static final String VALUE = "test_value";

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

  public static final class TestContentProvider extends MockContentProvider {
    private final Map<String, String> storage = new LinkedHashMap<>();

    private ContentResolver contentResolver;

    void init(ContentResolver contentResolver) {
      this.contentResolver = contentResolver;
    }

    @Override public Uri insert(Uri uri, ContentValues values) {
      storage.put(values.getAsString(KEY), values.getAsString(VALUE));
      contentResolver.notifyChange(uri, null);
      return Uri.parse(AUTHORITY + "/" + values.getAsString(KEY));
    }

    @Override public int update(Uri uri, ContentValues values, String selection,
        String[] selectionArgs) {
      for (String key : storage.keySet()) {
        storage.put(key, values.getAsString(VALUE));
      }
      contentResolver.notifyChange(uri, null);
      return storage.size();
    }

    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
      int result = storage.size();
      storage.clear();
      contentResolver.notifyChange(uri, null);
      return result;
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
        String[] selectionArgs, String sortOrder) {
      MatrixCursor result = new MatrixCursor(new String[] { KEY, VALUE });
      for (Map.Entry<String, String> entry : storage.entrySet()) {
        result.addRow(new Object[] { entry.getKey(), entry.getValue() });
      }
      return result;
    }
  }
}
