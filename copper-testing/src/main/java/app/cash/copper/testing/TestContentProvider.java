package app.cash.copper.testing;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.test.mock.MockContentProvider;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TestContentProvider extends MockContentProvider {
  public static final Uri AUTHORITY = Uri.parse("content://test_authority");
  public static final Uri TABLE = AUTHORITY.buildUpon().appendPath("test_table").build();
  public static final String KEY = "test_key";
  public static final String VALUE = "test_value";

  private final Map<String, String> storage = new LinkedHashMap<>();

  private ContentResolver contentResolver;

  public void init(ContentResolver contentResolver) {
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
