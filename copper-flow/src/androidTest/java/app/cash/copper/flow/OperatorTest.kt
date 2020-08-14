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
        assertThat(expectItem()).isEqualTo(Employee("alice", "Alice Allison"))
        expectComplete()
      }
  }

  @Test fun mapToOneThrowsOnMultipleRows() = runBlocking {
    flowOf(queryOf("alice", "Alice Allison", "bob", "Bob Bobberson"))
      .mapToOne(mapper = Employee.MAPPER)
      .test {
        expectError().assert {
          isInstanceOf(IllegalStateException::class.java)
          hasMessageThat().isEqualTo("Cursor returned more than 1 row")
        }
      }
  }

  @Test fun mapToOneIgnoresNullCursor() = runBlocking {
    flowOf(NullQuery)
      .mapToOne(mapper = Employee.MAPPER)
      .test {
        expectComplete()
      }
  }

  @Test fun mapToOneOrDefault() = runBlocking {
    flowOf(queryOf("alice", "Alice Allison"))
      .mapToOneOrDefault(Employee("fred", "Fred Frederson"), mapper = Employee.MAPPER)
      .test {
        assertThat(expectItem()).isEqualTo(Employee("alice", "Alice Allison"))
        expectComplete()
      }
  }

  @Test fun mapToOneOrDefaultThrowsOnMultipleRows() = runBlocking {
    flowOf(queryOf("alice", "Alice Allison", "bob", "Bob Bobberson"))
      .mapToOneOrDefault(Employee("fred", "Fred Frederson"), mapper = Employee.MAPPER)
      .test {
        expectError().assert {
          isInstanceOf(IllegalStateException::class.java)
          hasMessageThat().isEqualTo("Cursor returned more than 1 row")
        }
      }
  }

  @Test fun mapToOneOrDefaultReturnsDefaultWhenNullCursor() = runBlocking {
    flowOf(NullQuery)
      .mapToOneOrDefault(Employee("bob", "Bob Bobberson"), mapper = Employee.MAPPER)
      .test {
        assertThat(expectItem()).isEqualTo(Employee("bob", "Bob Bobberson"))
        expectComplete()
      }
  }

  @Test fun mapToList() = runBlocking {
    flowOf(queryOf("alice", "Alice Allison", "bob", "Bob Bobberson", "eve", "Eve Evenson"))
      .mapToList(mapper = Employee.MAPPER)
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
    flowOf(queryOf())
      .mapToList(mapper = Employee.MAPPER)
      .test {
        assertThat(expectItem()).isEmpty()
        expectComplete()
      }
  }

  @Test fun mapToListIgnoresNullCursor() = runBlocking {
    flowOf(NullQuery)
      .mapToList(mapper = Employee.MAPPER)
      .test {
        expectComplete()
      }
  }

  @Test fun mapToNullable() = runBlocking {
    flowOf(queryOf("alice", "Alice Allison"))
      .mapToNullable(mapper = Employee.MAPPER)
      .test {
        assertThat(expectItem()).isEqualTo(Employee("alice", "Alice Allison"))
        expectComplete()
      }
  }

  @Test fun mapToNullableThrowsOnMultipleRows() = runBlocking {
    flowOf(queryOf("alice", "Alice Allison", "bob", "Bob Bobberson"))
      .mapToNullable(mapper = Employee.MAPPER)
      .test {
        expectError().assert {
          isInstanceOf(IllegalStateException::class.java)
          hasMessageThat().isEqualTo("Cursor returned more than 1 row")
        }
      }
  }

  @Test fun mapToNullableIgnoresNullCursor() = runBlocking {
    flowOf(NullQuery)
      .mapToNullable(mapper = Employee.MAPPER)
      .test {
        expectComplete()
      }
  }
}
