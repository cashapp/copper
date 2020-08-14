package app.cash.copper.rx2;

import androidx.test.runner.AndroidJUnit4;
import app.cash.copper.Query;
import app.cash.copper.testing.Employee;
import app.cash.copper.testing.NullQuery;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;

import static app.cash.copper.testing.Employee.queryOf;
import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("CheckResult")
public final class QueryAsRowsTest {
  @Test public void asRowsEmpty() {
    List<Employee> employees =
      RxContentResolver.asRows(queryOf(), Employee.MAPPER).toList().blockingGet();
    assertThat(employees).isEmpty();
  }

  @Test public void asRows() {
    Query query = queryOf("alice", "Alice Allison", "bob", "Bob Bobberson");
    List<Employee> employees =
      RxContentResolver.asRows(query, Employee.MAPPER).toList().blockingGet();
    assertThat(employees).containsExactly(
      new Employee("alice", "Alice Allison"),
      new Employee("bob", "Bob Bobberson"));
  }

  @Test public void asRowsStopsWhenUnsubscribed() {
    Query query = queryOf("alice", "Alice Allison", "bob", "Bob Bobberson");
    final AtomicInteger count = new AtomicInteger();
    RxContentResolver.asRows(query, c -> {
      count.incrementAndGet();
      return Employee.MAPPER.invoke(c);
    }).take(1).blockingFirst();
    assertThat(count.get()).isEqualTo(1);
  }

  @Test public void asRowsEmptyWhenNullCursor() {
    final AtomicInteger count = new AtomicInteger();
    RxContentResolver.asRows(NullQuery.INSTANCE, cursor -> {
      count.incrementAndGet();
      return Employee.MAPPER.invoke(cursor);
    }).test().assertNoValues().assertComplete();

    assertThat(count.get()).isEqualTo(0);
  }
}
