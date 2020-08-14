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
package app.cash.copper.rx2;

import android.database.Cursor;
import android.database.MatrixCursor;
import androidx.test.filters.SdkSuppress;
import app.cash.copper.Query;
import app.cash.copper.testing.Employee;
import app.cash.copper.testing.NullQuery;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import java.util.List;
import java.util.Optional;
import kotlin.jvm.functions.Function1;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public final class QueryOperatorTest {
  @Test public void mapToOne() {
    Employee employees = employeesQuery("alice", "Alice Allison")
        .to(o -> RxContentResolver.mapToOne(o, MAPPER))
        .blockingFirst();
    assertThat(employees).isEqualTo(new Employee("alice", "Alice Allison"));
  }

  @Test public void mapToOneThrowsWhenMapperReturnsNull() {
    employeesQuery("alice", "Alice Allison")
        .to(o -> RxContentResolver.mapToOne(o, c -> null))
        .test()
        .assertError(NullPointerException.class)
        .assertErrorMessage("QueryToOne mapper returned null");
  }

  @Test public void mapToOneThrowsOnMultipleRows() {
    Observable<Employee> employees =
        employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson")
            .to(o -> RxContentResolver.mapToOne(o, MAPPER));
    try {
      employees.blockingFirst();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageThat().isEqualTo("Cursor returned more than 1 row");
    }
  }

  @Test public void mapToOneIgnoresNullCursor() {
    TestObserver<Employee> observer = new TestObserver<>();
    Observable.just(NullQuery.INSTANCE)
        .to(o -> RxContentResolver.mapToOne(o, MAPPER))
        .subscribe(observer);

    observer.assertNoValues();
    observer.assertComplete();
  }

  @Test public void mapToOneOrDefault() {
    Employee employees = employeesQuery("alice", "Alice Allison")
        .to(o -> RxContentResolver.mapToOneOrDefault(o, new Employee("fred", "Fred Frederson"),
          MAPPER))
        .blockingFirst();
    assertThat(employees).isEqualTo(new Employee("alice", "Alice Allison"));
  }

  @Test public void mapToOneOrDefaultThrowsWhenMapperReturnsNull() {
    employeesQuery("alice", "Alice Allison")
        .to(o -> RxContentResolver.mapToOneOrDefault(o, new Employee("fred", "Fred Frederson"), c -> null))
        .test()
        .assertError(NullPointerException.class)
        .assertErrorMessage("QueryToOne mapper returned null");
  }

  @Test public void mapToOneOrDefaultThrowsOnMultipleRows() {
    Observable<Employee> employees =
        employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson")
            .to(o -> RxContentResolver.mapToOneOrDefault(o, new Employee("fred", "Fred Frederson"),
              MAPPER));
    try {
      employees.blockingFirst();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageThat().isEqualTo("Cursor returned more than 1 row");
    }
  }

  @Test public void mapToOneOrDefaultReturnsDefaultWhenNullCursor() {
    Employee defaultEmployee = new Employee("bob", "Bob Bobberson");

    TestObserver<Employee> observer = new TestObserver<>();
    Observable.just(NullQuery.INSTANCE)
        .to(o -> RxContentResolver.mapToOneOrDefault(o, defaultEmployee, MAPPER))
        .subscribe(observer);

    observer.assertValues(defaultEmployee);
    observer.assertComplete();
  }

  @Test public void mapToList() {
    List<Employee> employees =
        employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson", "eve", "Eve Evenson")
            .to(o -> RxContentResolver.mapToList(o, MAPPER))
            .blockingFirst();
    assertThat(employees).containsExactly( //
        new Employee("alice", "Alice Allison"), //
        new Employee("bob", "Bob Bobberson"), //
        new Employee("eve", "Eve Evenson"));
  }

  @Test public void mapToListEmptyWhenNoRows() {
    List<Employee> employees = employeesQuery()
        .to(o -> RxContentResolver.mapToList(o, MAPPER))
        .blockingFirst();
    assertThat(employees).isEmpty();
  }

  @Test public void mapToListReturnsNullOnMapperNull() {
    Function1<Cursor, Employee> mapToNull = new Function1<Cursor, Employee>() {
      private int count;

      @Override public Employee invoke(Cursor cursor) {
        return count++ == 2 ? null : MAPPER.invoke(cursor);
      }
    };
    List<Employee> employees =
        employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson", "eve", "Eve Evenson")
            .to(o -> RxContentResolver.mapToList(o, mapToNull)) //
            .blockingFirst();

    assertThat(employees).containsExactly(
        new Employee("alice", "Alice Allison"),
        new Employee("bob", "Bob Bobberson"),
        null);
  }

  @Test public void mapToListIgnoresNullCursor() {
    TestObserver<List<Employee>> subscriber = new TestObserver<>();
    Observable.just(NullQuery.INSTANCE)
        .to(o -> RxContentResolver.mapToList(o, MAPPER))
        .subscribe(subscriber);

    subscriber.assertNoValues();
    subscriber.assertComplete();
  }

  @SdkSuppress(minSdkVersion = 24)
  @Test public void mapToOptional() {
    employeesQuery("alice", "Alice Allison")
        .to(o -> RxContentResolver.mapToOptional(o, MAPPER))
        .test()
        .assertValue(Optional.of(new Employee("alice", "Alice Allison")));
  }

  @SdkSuppress(minSdkVersion = 24)
  @Test public void mapToOptionalThrowsWhenMapperReturnsNull() {
    employeesQuery("alice", "Alice Allison")
        .to(o -> RxContentResolver.mapToOptional(o, c -> null))
        .test()
        .assertError(NullPointerException.class)
        .assertErrorMessage("QueryToOne mapper returned null");
  }

  @SdkSuppress(minSdkVersion = 24)
  @Test public void mapToOptionalThrowsOnMultipleRows() {
    employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson")
        .to(o -> RxContentResolver.mapToOptional(o, MAPPER))
        .test()
        .assertError(IllegalStateException.class)
        .assertErrorMessage("Cursor returned more than 1 row");
  }

  @SdkSuppress(minSdkVersion = 24)
  @Test public void mapToOptionalIgnoresNullCursor() {
    Observable.just(NullQuery.INSTANCE)
        .to(o -> RxContentResolver.mapToOptional(o, MAPPER))
        .test()
        .assertValue(Optional.empty());
  }

  static final Function1<Cursor, Employee> MAPPER = cursor -> new Employee(
      cursor.getString(cursor.getColumnIndexOrThrow("username")),
      cursor.getString(cursor.getColumnIndexOrThrow("name")));

  private static Observable<Query> employeesQuery(final String... values) {
    Query query = () -> {
      MatrixCursor cursor = new MatrixCursor(new String[] { "username", "name" });
      for (int i = 0; i < values.length; i += 2) {
        cursor.addRow(new Object[] { values[i], values[i + 1] });
      }
      return cursor;
    };
    return Observable.just(query);
  }
}
