package app.cash.copper.flow

import androidx.test.runner.AndroidJUnit4
import app.cash.copper.testing.Employee
import app.cash.copper.testing.Employee.Companion.queryOf
import app.cash.copper.testing.NullQuery
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
@RunWith(AndroidJUnit4::class)
class AsRowsTest {
  @Test fun asRowsEmpty() = runBlocking {
    queryOf()
      .asRows(mapper = Employee.MAPPER)
      .test {
        expectComplete()
      }
  }

  @Test fun asRows() = runBlocking {
    queryOf("alice", "Alice Allison", "bob", "Bob Bobberson")
      .asRows(mapper = Employee.MAPPER)
      .test {
        assertThat(expectItem()).isEqualTo(Employee("alice", "Alice Allison"))
        assertThat(expectItem()).isEqualTo(Employee("bob", "Bob Bobberson"))
        expectComplete()
      }
  }

  @Test fun asRowsStopsWhenCanceled() = runBlocking {
    var count = 0
    queryOf("alice", "Alice Allison", "bob", "Bob Bobberson", "eve", "Eve Evenson")
      .asRows {
        count++
        Employee.MAPPER.invoke(it)
      }
      .take(1)
      .test {
        expectItem()
        expectComplete()
      }
    // This is 2 not 1 because of how the implementation works. It will always be N+1.
    assertThat(count).isEqualTo(2)
  }

  @Test fun asRowsEmptyWhenNullCursor() = runBlocking {
    var count = 0
    NullQuery
      .asRows {
        count++
        Employee.MAPPER.invoke(it)
      }
      .test {
        expectComplete()
      }
    assertThat(count).isEqualTo(0)
  }
}
