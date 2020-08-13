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
import androidx.annotation.Nullable;
import androidx.test.filters.SdkSuppress;
import app.cash.copper.rx2.RxContentResolver.Query;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public final class QueryOperatorTest {
  @Test public void mapToOne() {
    Employee employees = employeesQuery("alice", "Alice Allison")
        .lift(Query.mapToOne(MAPPER))
        .blockingFirst();
    assertThat(employees).isEqualTo(new Employee("alice", "Alice Allison"));
  }

  @Test public void mapToOneThrowsWhenMapperReturnsNull() {
    employeesQuery("alice", "Alice Allison")
        .lift(Query.mapToOne(new Function<Cursor, Employee>() {
          @Override public Employee apply(Cursor cursor) throws Exception {
            return null;
          }
        }))
        .test()
        .assertError(NullPointerException.class)
        .assertErrorMessage("QueryToOne mapper returned null");
  }

  @Test public void mapToOneThrowsOnMultipleRows() {
    Observable<Employee> employees =
        employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson")
            .lift(Query.mapToOne(MAPPER));
    try {
      employees.blockingFirst();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageThat().isEqualTo("Cursor returned more than 1 row");
    }
  }

  @Test public void mapToOneIgnoresNullCursor() {
    Query nully = new Query() {
      @Nullable @Override public Cursor run() {
        return null;
      }
    };

    TestObserver<Employee> observer = new TestObserver<>();
    Observable.just(nully)
        .lift(Query.mapToOne(MAPPER))
        .subscribe(observer);

    observer.assertNoValues();
    observer.assertComplete();
  }

  @Test public void mapToOneOrDefault() {
    Employee employees = employeesQuery("alice", "Alice Allison")
        .lift(Query.mapToOneOrDefault(
            MAPPER, new Employee("fred", "Fred Frederson")))
        .blockingFirst();
    assertThat(employees).isEqualTo(new Employee("alice", "Alice Allison"));
  }

  @Test public void mapToOneOrDefaultDisallowsNullDefault() {
    try {
      Query.mapToOneOrDefault(MAPPER, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessageThat().isEqualTo("defaultValue == null");
    }
  }

  @Test public void mapToOneOrDefaultThrowsWhenMapperReturnsNull() {
    employeesQuery("alice", "Alice Allison")
        .lift(Query.mapToOneOrDefault(new Function<Cursor, Employee>() {
          @Override public Employee apply(Cursor cursor) throws Exception {
            return null;
          }
        }, new Employee("fred", "Fred Frederson")))
        .test()
        .assertError(NullPointerException.class)
        .assertErrorMessage("QueryToOne mapper returned null");
  }

  @Test public void mapToOneOrDefaultThrowsOnMultipleRows() {
    Observable<Employee> employees =
        employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson")
            .lift(Query.mapToOneOrDefault(
                MAPPER, new Employee("fred", "Fred Frederson")));
    try {
      employees.blockingFirst();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageThat().isEqualTo("Cursor returned more than 1 row");
    }
  }

  @Test public void mapToOneOrDefaultReturnsDefaultWhenNullCursor() {
    Employee defaultEmployee = new Employee("bob", "Bob Bobberson");
    Query nully = new Query() {
      @Nullable @Override public Cursor run() {
        return null;
      }
    };

    TestObserver<Employee> observer = new TestObserver<>();
    Observable.just(nully)
        .lift(Query.mapToOneOrDefault(MAPPER, defaultEmployee))
        .subscribe(observer);

    observer.assertValues(defaultEmployee);
    observer.assertComplete();
  }

  @Test public void mapToList() {
    List<Employee> employees =
        employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson", "eve", "Eve Evenson")
            .lift(Query.mapToList(MAPPER))
            .blockingFirst();
    assertThat(employees).containsExactly( //
        new Employee("alice", "Alice Allison"), //
        new Employee("bob", "Bob Bobberson"), //
        new Employee("eve", "Eve Evenson"));
  }

  @Test public void mapToListEmptyWhenNoRows() {
    List<Employee> employees = employeesQuery()
        .lift(Query.mapToList(MAPPER))
        .blockingFirst();
    assertThat(employees).isEmpty();
  }

  @Test public void mapToListReturnsNullOnMapperNull() {
    Function<Cursor, Employee> mapToNull = new Function<Cursor, Employee>() {
      private int count;

      @Override public Employee apply(Cursor cursor) throws Exception {
        return count++ == 2 ? null : MAPPER.apply(cursor);
      }
    };
    List<Employee> employees =
        employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson", "eve", "Eve Evenson")
            .lift(Query.mapToList(mapToNull)) //
            .blockingFirst();

    assertThat(employees).containsExactly(
        new Employee("alice", "Alice Allison"),
        new Employee("bob", "Bob Bobberson"),
        null);
  }

  @Test public void mapToListIgnoresNullCursor() {
    Query nully = new Query() {
      @Nullable @Override public Cursor run() {
        return null;
      }
    };

    TestObserver<List<Employee>> subscriber = new TestObserver<>();
    Observable.just(nully)
        .lift(Query.mapToList(MAPPER))
        .subscribe(subscriber);

    subscriber.assertNoValues();
    subscriber.assertComplete();
  }

  @SdkSuppress(minSdkVersion = 24)
  @Test public void mapToOptional() {
    employeesQuery("alice", "Alice Allison")
        .lift(Query.mapToOptional(MAPPER))
        .test()
        .assertValue(Optional.of(new Employee("alice", "Alice Allison")));
  }

  @SdkSuppress(minSdkVersion = 24)
  @Test public void mapToOptionalThrowsWhenMapperReturnsNull() {
    employeesQuery("alice", "Alice Allison")
        .lift(Query.mapToOptional(new Function<Cursor, Employee>() {
          @Override public Employee apply(Cursor cursor) throws Exception {
            return null;
          }
        }))
        .test()
        .assertError(NullPointerException.class)
        .assertErrorMessage("QueryToOne mapper returned null");
  }

  @SdkSuppress(minSdkVersion = 24)
  @Test public void mapToOptionalThrowsOnMultipleRows() {
    employeesQuery("alice", "Alice Allison", "bob", "Bob Bobberson")
        .lift(Query.mapToOptional(MAPPER))
        .test()
        .assertError(IllegalStateException.class)
        .assertErrorMessage("Cursor returned more than 1 row");
  }

  @SdkSuppress(minSdkVersion = 24)
  @Test public void mapToOptionalIgnoresNullCursor() {
    Query nully = new Query() {
      @Nullable @Override public Cursor run() {
        return null;
      }
    };

    Observable.just(nully)
        .lift(Query.mapToOptional(MAPPER))
        .test()
        .assertValue(Optional.<Employee>empty());
  }

  static final Function<Cursor, Employee> MAPPER = new Function<Cursor, Employee>() {
    @Override public Employee apply(Cursor cursor) {
      return new Employee(
          cursor.getString(cursor.getColumnIndexOrThrow("username")),
          cursor.getString(cursor.getColumnIndexOrThrow("name")));
    }
  };

  private static QueryObservable employeesQuery(final String... values) {
    Query query = new Query() {
      @Override public Cursor run() {
        MatrixCursor cursor = new MatrixCursor(new String[] { "username", "name" });
        for (int i = 0; i < values.length; i += 2) {
          cursor.addRow(new Object[] { values[i], values[i + 1] });
        }
        return cursor;
      }
    };
    return new QueryObservable(Observable.just(query));
  }
}
