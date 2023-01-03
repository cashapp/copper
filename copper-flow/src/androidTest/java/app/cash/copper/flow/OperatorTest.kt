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
import app.cash.copper.testing.Employee
import app.cash.copper.testing.Employee.Companion.queryOf
import app.cash.copper.testing.NullQuery
import app.cash.copper.testing.assert
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
class OperatorTest {
  @Test fun mapToOne() = runBlocking {
    flowOf(queryOf("alice", "Alice Allison"))
      .mapToOne(mapper = Employee.MAPPER)
      .test {
        assertThat(awaitItem()).isEqualTo(Employee("alice", "Alice Allison"))
        awaitComplete()
      }
  }

  @Test fun mapToOneWithDefault() = runBlocking {
    flowOf(queryOf("alice", "Alice Allison"))
      .mapToOne(default = Employee("fred", "Fred Frederson"), mapper = Employee.MAPPER)
      .test {
        assertThat(awaitItem()).isEqualTo(Employee("alice", "Alice Allison"))
        awaitComplete()
      }
  }

  @Test fun mapToOneThrowsOnMultipleRows() = runBlocking {
    flowOf(queryOf("alice", "Alice Allison", "bob", "Bob Bobberson"))
      .mapToOne(mapper = Employee.MAPPER)
      .test {
        awaitError().assert {
          isInstanceOf(IllegalStateException::class.java)
          hasMessageThat().isEqualTo("Cursor returned more than 1 row")
        }
      }
  }

  @Test fun mapToOneEmptyIgnoredWithoutDefault() = runBlocking {
    flowOf(queryOf())
      .mapToOne(mapper = Employee.MAPPER)
      .test {
        awaitComplete()
      }
  }

  @Test fun mapToOneWithDefaultEmpty() = runBlocking {
    flowOf(queryOf())
      .mapToOne(default = Employee("fred", "Fred Frederson"), mapper = Employee.MAPPER)
      .test {
        assertThat(awaitItem()).isEqualTo(Employee("fred", "Fred Frederson"))
        awaitComplete()
      }
  }

  @Test fun mapToOneIgnoresNullCursor() = runBlocking {
    flowOf(NullQuery)
      .mapToOne(mapper = Employee.MAPPER)
      .test {
        awaitComplete()
      }
  }

  @Test fun mapToOneWithDefaultIgnoresNullCursor() = runBlocking {
    flowOf(NullQuery)
      .mapToOne(default = Employee("fred", "Fred Frederson"), mapper = Employee.MAPPER)
      .test {
        awaitComplete()
      }
  }

  @Test fun mapToList() = runBlocking {
    flowOf(queryOf("alice", "Alice Allison", "bob", "Bob Bobberson", "eve", "Eve Evenson"))
      .mapToList(mapper = Employee.MAPPER)
      .test {
        assertThat(awaitItem()).containsExactly(
          Employee("alice", "Alice Allison"),
          Employee("bob", "Bob Bobberson"),
          Employee("eve", "Eve Evenson")
        )
        awaitComplete()
      }
  }

  @Test fun mapToListEmptyWhenNoRows() = runBlocking {
    flowOf(queryOf())
      .mapToList(mapper = Employee.MAPPER)
      .test {
        assertThat(awaitItem()).isEmpty()
        awaitComplete()
      }
  }

  @Test fun mapToListReturnsNullOnMapperNull() = runBlocking {
    val mapToNull = object : (Cursor) -> Employee? {
      private var count = 0

      override fun invoke(cursor: Cursor): Employee? {
        return if (count++ == 2) null else Employee.MAPPER.invoke(cursor)
      }
    }
    flowOf(queryOf("alice", "Alice Allison", "bob", "Bob Bobberson", "eve", "Eve Evenson"))
      .mapToList(mapper = mapToNull)
      .test {
        assertThat(awaitItem()).containsExactly(
          Employee("alice", "Alice Allison"),
          Employee("bob", "Bob Bobberson"),
          null
        )
        awaitComplete()
      }
  }

  @Test fun mapToListIgnoresNullCursor() = runBlocking {
    flowOf(NullQuery)
      .mapToList(mapper = Employee.MAPPER)
      .test {
        awaitComplete()
      }
  }

  @Test fun mapToOneOrNull() = runBlocking {
    flowOf(queryOf("alice", "Alice Allison"))
      .mapToOneOrNull(mapper = Employee.MAPPER)
      .test {
        assertThat(awaitItem()).isEqualTo(Employee("alice", "Alice Allison"))
        awaitComplete()
      }
  }

  @Test fun mapToOneOrNullThrowsOnMultipleRows() = runBlocking {
    flowOf(queryOf("alice", "Alice Allison", "bob", "Bob Bobberson"))
      .mapToOneOrNull(mapper = Employee.MAPPER)
      .test {
        awaitError().assert {
          isInstanceOf(IllegalStateException::class.java)
          hasMessageThat().isEqualTo("Cursor returned more than 1 row")
        }
      }
  }

  @Test fun mapToOneOrNullIgnoresNullCursor() = runBlocking {
    flowOf(NullQuery)
      .mapToOneOrNull(mapper = Employee.MAPPER)
      .test {
        awaitComplete()
      }
  }
}
