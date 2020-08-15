package app.cash.copper.rx2;

import android.database.Cursor;
import androidx.test.runner.AndroidJUnit4;
import app.cash.copper.Query;
import app.cash.copper.testing.Employee;
import app.cash.copper.testing.NullQuery;
import java.util.concurrent.atomic.AtomicInteger;
import kotlin.jvm.functions.Function1;
import org.junit.Test;
import org.junit.runner.RunWith;

import static app.cash.copper.testing.Employee.queryOf;
import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("CheckResult")
public final class AsRowsTest {
  @Test public void asRowsEmpty() {
    RxContentResolver.asRows(queryOf(), Employee.MAPPER)
        .test()
        .assertNoValues()
        .assertComplete();
  }

  @Test public void asRows() {
    Query query = queryOf("alice", "Alice Allison", "bob", "Bob Bobberson");
    RxContentResolver.asRows(query, Employee.MAPPER)
        .test()
        .assertValueAt(0, new Employee("alice", "Alice Allison"))
        .assertValueAt(1, new Employee("bob", "Bob Bobberson"))
        .assertComplete();
  }

  @Test public void asRowsStopsWhenUnsubscribed() {
    final AtomicInteger count = new AtomicInteger();
    Function1<Cursor, Employee> mapper = c -> {
      count.incrementAndGet();
      return Employee.MAPPER.invoke(c);
    };

    Query query = queryOf("alice", "Alice Allison", "bob", "Bob Bobberson");
    RxContentResolver.asRows(query, mapper)
        .take(1)
        .test()
        .assertValue(new Employee("alice", "Alice Allison"))
        .assertComplete();
    assertThat(count.get()).isEqualTo(1);
  }

  @Test public void asRowsEmptyWhenNullCursor() {
    final AtomicInteger count = new AtomicInteger();
    Function1<Cursor, Employee> mapper = cursor -> {
      count.incrementAndGet();
      return Employee.MAPPER.invoke(cursor);
    };

    RxContentResolver.asRows(NullQuery.INSTANCE, mapper)
      .test()
      .assertNoValues()
      .assertComplete();

    assertThat(count.get()).isEqualTo(0);
  }
}
