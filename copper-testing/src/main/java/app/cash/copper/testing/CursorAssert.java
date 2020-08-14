package app.cash.copper.testing;

import android.database.Cursor;

import static com.google.common.truth.Truth.assertWithMessage;

public final class CursorAssert {
  private final Cursor cursor;
  private int row = 0;

  public CursorAssert(Cursor cursor) {
    this.cursor = cursor;
  }

  public CursorAssert hasRow(Object... values) {
    assertWithMessage("row " + (row + 1) + " exists").that(cursor.moveToNext()).isTrue();
    row += 1;
    assertWithMessage("column count").that(cursor.getColumnCount()).isEqualTo(values.length);
    for (int i = 0; i < values.length; i++) {
      assertWithMessage("row " + row + " column '" + cursor.getColumnName(i) + "'")
          .that(cursor.getString(i)).isEqualTo(values[i]);
    }
    return this;
  }

  public void isExhausted() {
    if (cursor.moveToNext()) {
      StringBuilder data = new StringBuilder();
      for (int i = 0; i < cursor.getColumnCount(); i++) {
        if (i > 0) data.append(", ");
        data.append(cursor.getString(i));
      }
      throw new AssertionError("Expected no more rows but was: " + data);
    }
    cursor.close();
  }
}
