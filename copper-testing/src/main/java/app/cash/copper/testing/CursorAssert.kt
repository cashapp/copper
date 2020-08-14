package app.cash.copper.testing

import android.database.Cursor
import app.cash.copper.Query
import com.google.common.truth.Truth.assertWithMessage

class CursorAssert(private val cursor: Cursor) {
  private var row = 0

  fun hasRow(vararg values: Any?) = apply {
    assertWithMessage("row " + (row + 1) + " exists")
      .that(cursor.moveToNext())
      .isTrue()
    row += 1
    assertWithMessage("column count").that(cursor.columnCount).isEqualTo(values.size)
    for (i in values.indices) {
      assertWithMessage("row " + row + " column '" + cursor.getColumnName(i) + "'")
        .that(cursor.getString(i))
        .isEqualTo(values[i])
    }
  }

  fun isExhausted() {
    if (cursor.moveToNext()) {
      val data = StringBuilder()
      for (i in 0 until cursor.columnCount) {
        if (i > 0) data.append(", ")
        data.append(cursor.getString(i))
      }
      throw AssertionError("Expected no more rows but was: $data")
    }
    cursor.close()
  }
}

fun Query.assert(body: CursorAssert.() -> Unit) {
  CursorAssert(run()!!).apply(body).isExhausted()
}
