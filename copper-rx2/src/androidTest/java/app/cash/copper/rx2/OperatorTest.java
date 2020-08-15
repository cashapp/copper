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
import androidx.test.filters.SdkSuppress;
import app.cash.copper.testing.Employee;
import app.cash.copper.testing.NullQuery;
import java.util.Optional;
import kotlin.jvm.functions.Function1;
import org.junit.Test;

import static app.cash.copper.testing.Employee.queryOf;
import static io.reactivex.Observable.just;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public final class OperatorTest {
  @Test public void mapToOne() {
    just(queryOf("alice", "Alice Allison"))
        .to(o -> RxContentResolver.mapToOne(o, Employee.MAPPER))
        .test()
        .assertValue(new Employee("alice", "Alice Allison"));
  }

  @Test public void mapToOneThrowsWhenMapperReturnsNull() {
    just(queryOf("alice", "Alice Allison"))
        .to(o -> RxContentResolver.mapToOne(o, c -> null))
        .test()
        .assertError(NullPointerException.class)
        .assertErrorMessage("QueryToOne mapper returned null");
  }

  @Test public void mapToOneWithDefault() {
    just(queryOf("alice", "Alice Allison"))
        .to(o -> RxContentResolver.mapToOne(o, new Employee("fred", "Fred Frederson"), Employee.MAPPER))
        .test()
        .assertValue(new Employee("alice", "Alice Allison"));
  }

  @Test public void mapToOneThrowsOnMultipleRows() {
    just(queryOf("alice", "Alice Allison", "bob", "Bob Bobberson"))
        .to(o -> RxContentResolver.mapToOne(o, Employee.MAPPER))
        .test()
        .assertError(IllegalStateException.class)
        .assertErrorMessage("Cursor returned more than 1 row");
  }

  @Test public void mapToOneEmptyIgnoredWithoutDefault() {
    just(queryOf())
        .to(o -> RxContentResolver.mapToOne(o, Employee.MAPPER))
        .test()
        .assertNoValues()
        .assertComplete();
  }

  @Test public void mapToOneWithDefaultEmpty() {
    Employee defaultValue = new Employee("fred", "Fred Frederson");
    just(queryOf())
        .to(o -> RxContentResolver.mapToOne(o, defaultValue, Employee.MAPPER))
        .test()
        .assertValue(defaultValue)
        .assertComplete();
  }

  @Test public void mapToOneIgnoresNullCursor() {
    just(NullQuery.INSTANCE)
        .to(o -> RxContentResolver.mapToOne(o, Employee.MAPPER))
        .test()
        .assertNoValues()
        .assertComplete();
  }

  @Test public void mapToOneWithDefaultIgnoresNullCursor() {
    just(NullQuery.INSTANCE)
        .to(o -> RxContentResolver.mapToOne(o, new Employee("fred", "Fred Frederson"), Employee.MAPPER))
        .test()
        .assertNoValues()
        .assertComplete();
  }

  @Test public void mapToList() {
    just(queryOf("alice", "Alice Allison", "bob", "Bob Bobberson", "eve", "Eve Evenson"))
        .to(o -> RxContentResolver.mapToList(o, Employee.MAPPER))
        .test()
        .assertValue(asList(
            new Employee("alice", "Alice Allison"), //
            new Employee("bob", "Bob Bobberson"), //
            new Employee("eve", "Eve Evenson")))
        .assertComplete();
  }

  @Test public void mapToListEmptyWhenNoRows() {
    just(queryOf())
        .to(o -> RxContentResolver.mapToList(o, Employee.MAPPER))
        .test()
        .assertValue(emptyList())
        .assertComplete();
  }

  @Test public void mapToListReturnsNullOnMapperNull() {
    Function1<Cursor, Employee> mapToNull = new Function1<Cursor, Employee>() {
      private int count;

      @Override public Employee invoke(Cursor cursor) {
        return count++ == 2 ? null : Employee.MAPPER.invoke(cursor);
      }
    };
    just(queryOf("alice", "Alice Allison", "bob", "Bob Bobberson", "eve", "Eve Evenson"))
        .to(o -> RxContentResolver.mapToList(o, mapToNull)) //
        .test()
        .assertValue(asList(
            new Employee("alice", "Alice Allison"),
            new Employee("bob", "Bob Bobberson"),
            null))
        .assertComplete();
  }

  @Test public void mapToListIgnoresNullCursor() {
    just(NullQuery.INSTANCE)
        .to(o -> RxContentResolver.mapToList(o, Employee.MAPPER))
        .test()
        .assertNoValues()
        .assertComplete();
  }

  @SdkSuppress(minSdkVersion = 24)
  @Test public void mapToOptional() {
    just(queryOf("alice", "Alice Allison"))
        .to(o -> RxContentResolver.mapToOptional(o, Employee.MAPPER))
        .test()
        .assertValue(Optional.of(new Employee("alice", "Alice Allison")));
  }

  @SdkSuppress(minSdkVersion = 24)
  @Test public void mapToOptionalThrowsWhenMapperReturnsNull() {
    just(queryOf("alice", "Alice Allison"))
        .to(o -> RxContentResolver.mapToOptional(o, c -> null))
        .test()
        .assertError(NullPointerException.class)
        .assertErrorMessage("QueryToOne mapper returned null");
  }

  @SdkSuppress(minSdkVersion = 24)
  @Test public void mapToOptionalThrowsOnMultipleRows() {
    just(queryOf("alice", "Alice Allison", "bob", "Bob Bobberson"))
        .to(o -> RxContentResolver.mapToOptional(o, Employee.MAPPER))
        .test()
        .assertError(IllegalStateException.class)
        .assertErrorMessage("Cursor returned more than 1 row");
  }

  @SdkSuppress(minSdkVersion = 24)
  @Test public void mapToOptionalIgnoresNullCursor() {
    just(NullQuery.INSTANCE)
        .to(o -> RxContentResolver.mapToOptional(o, Employee.MAPPER))
        .test()
        .assertValue(Optional.empty());
  }
}
