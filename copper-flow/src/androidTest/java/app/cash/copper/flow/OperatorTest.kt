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
package app.cash.copper.flow

import android.database.Cursor
import android.database.MatrixCursor
import app.cash.copper.Query
import app.cash.copper.testing.Employee
import app.cash.copper.testing.NullQuery
import app.cash.copper.testing.assert
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
class OperatorTest {
  @Test fun mapToOne() = runBlocking {
    employeesQuery("alice", "Alice Allison")
      .mapToOne(mapper = employeeMapper)
      .test {
        assertThat(expectItem()).isEqualTo(Employee("alice", "Alice Allison"))
        expectComplete()
      }
  }

  @Test fun mapToOneThrowsOnMultipleRows() = runBlocking {
    employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson")
      .mapToOne(mapper = employeeMapper)
      .test {
        expectError().assert {
          isInstanceOf(IllegalStateException::class.java)
          hasMessageThat().isEqualTo("Cursor returned more than 1 row")
        }
      }
  }

  @Test fun mapToOneIgnoresNullCursor() = runBlocking {
    flowOf(NullQuery)
      .mapToOne(mapper = employeeMapper)
      .test {
        expectComplete()
      }
  }

  @Test fun mapToOneOrDefault() = runBlocking {
    employeesQuery("alice", "Alice Allison")
      .mapToOneOrDefault(Employee("fred", "Fred Frederson"), mapper = employeeMapper)
      .test {
        assertThat(expectItem()).isEqualTo(Employee("alice", "Alice Allison"))
        expectComplete()
      }
  }

  @Test fun mapToOneOrDefaultThrowsOnMultipleRows() = runBlocking {
    employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson")
      .mapToOneOrDefault(Employee("fred", "Fred Frederson"), mapper = employeeMapper)
      .test {
        expectError().assert {
          isInstanceOf(IllegalStateException::class.java)
          hasMessageThat().isEqualTo("Cursor returned more than 1 row")
        }
      }
  }

  @Test fun mapToOneOrDefaultReturnsDefaultWhenNullCursor() = runBlocking {
    flowOf(NullQuery)
      .mapToOneOrDefault(Employee("bob", "Bob Bobberson"), mapper = employeeMapper)
      .test {
        assertThat(expectItem()).isEqualTo(Employee("bob", "Bob Bobberson"))
        expectComplete()
      }
  }

  @Test fun mapToList() = runBlocking {
    employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson", "eve", "Eve Evenson")
      .mapToList(mapper = employeeMapper)
      .test {
        assertThat(expectItem()).containsExactly(
          Employee("alice", "Alice Allison"),
          Employee("bob", "Bob Bobberson"),
          Employee("eve", "Eve Evenson")
        )
        expectComplete()
      }
  }

  @Test fun mapToListEmptyWhenNoRows() = runBlocking {
    employeesQuery()
      .mapToList(mapper = employeeMapper)
      .test {
        assertThat(expectItem()).isEmpty()
        expectComplete()
      }
  }

  @Test fun mapToListIgnoresNullCursor() = runBlocking {
    flowOf(NullQuery)
      .mapToList(mapper = employeeMapper)
      .test {
        expectComplete()
      }
  }

  @Test fun mapToNullable() = runBlocking {
    employeesQuery("alice", "Alice Allison")
      .mapToNullable(mapper = employeeMapper)
      .test {
        assertThat(expectItem()).isEqualTo(Employee("alice", "Alice Allison"))
        expectComplete()
      }
  }

  @Test fun mapToNullableThrowsOnMultipleRows() = runBlocking {
    employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson")
      .mapToNullable(mapper = employeeMapper)
      .test {
        expectError().assert {
          isInstanceOf(IllegalStateException::class.java)
          hasMessageThat().isEqualTo("Cursor returned more than 1 row")
        }
      }
  }

  @Test fun mapToNullableIgnoresNullCursor() = runBlocking {
    flowOf(NullQuery)
      .mapToNullable(mapper = employeeMapper)
      .test {
        expectComplete()
      }
  }

  private val employeeMapper = { cursor: Cursor ->
    Employee(
      cursor.getString(cursor.getColumnIndexOrThrow("username")),
      cursor.getString(cursor.getColumnIndexOrThrow("name"))
    )
  }

  private fun employeesQuery(vararg values: String): Flow<Query> {
    val query = object : Query {
      override fun run(): Cursor? {
        val cursor = MatrixCursor(arrayOf("username", "name"))
        for (i in values.indices step 2) {
          cursor.addRow(arrayOf<Any>(values[i], values[i + 1]))
        }
        return cursor
      }
    }
    return flowOf(query)
  }
}
