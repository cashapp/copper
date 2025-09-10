package app.cash.copper.testing

import android.database.Cursor
import app.cash.copper.Query
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue

class CursorAssert(private val cursor: Cursor) {
  private var row = 0

  fun hasRow(vararg values: Any?) = apply {
    assertThat(cursor.moveToNext(), name = "row ${row + 1} exists").isTrue()
    row += 1
    assertThat(cursor.columnCount, name = "column count").isEqualTo(values.size)
    for (i in values.indices) {
      assertThat(cursor.getString(i), name = "row $row column '${cursor.getColumnName(i)}'")
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
