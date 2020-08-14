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
package app.cash.copper.testing

import android.database.Cursor
import android.database.MatrixCursor
import app.cash.copper.Query

data class Employee(
  @JvmField
  val username: String,
  @JvmField
  val name: String
) {
  companion object {
    private const val columnUsername = "username"
    private const val columnName = "name"

    @JvmStatic
    fun queryOf(vararg values: String): Query {
      return object : Query {
        override fun run(): Cursor? {
          val cursor = MatrixCursor(arrayOf(columnUsername, columnName))
          for (i in values.indices step 2) {
            cursor.addRow(arrayOf<Any>(values[i], values[i + 1]))
          }
          return cursor
        }
      }
    }

    @JvmField
    val MAPPER: (Cursor) -> Employee = { cursor ->
      Employee(
        cursor.getString(cursor.getColumnIndexOrThrow(columnUsername)),
        cursor.getString(cursor.getColumnIndexOrThrow(columnName))
      )
    }
  }
}
