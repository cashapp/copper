package app.cash.copper.rx2;

import android.database.Cursor;
import android.database.MatrixCursor;
import androidx.annotation.Nullable;
import androidx.test.runner.AndroidJUnit4;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import kotlin.jvm.functions.Function1;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("CheckResult")
public final class QueryAsRowsTest {
  private static final String FIRST_NAME = "first_name";
  private static final String LAST_NAME = "last_name";
  private static final String[] COLUMN_NAMES = { FIRST_NAME, LAST_NAME };

  @Test public void asRowsEmpty() {
    MatrixCursor cursor = new MatrixCursor(COLUMN_NAMES);
    Query query = new CursorQuery(cursor);
    List<Name> names = query.asRows(Name.MAP).toList().blockingGet();
    assertThat(names).isEmpty();
  }

  @Test public void asRows() {
    MatrixCursor cursor = new MatrixCursor(COLUMN_NAMES);
    cursor.addRow(new Object[] { "Alice", "Allison" });
    cursor.addRow(new Object[] { "Bob", "Bobberson" });

    Query query = new CursorQuery(cursor);
    List<Name> names = query.asRows(Name.MAP).toList().blockingGet();
    assertThat(names).containsExactly(new Name("Alice", "Allison"), new Name("Bob", "Bobberson"));
  }

  @Test public void asRowsStopsWhenUnsubscribed() {
    MatrixCursor cursor = new MatrixCursor(COLUMN_NAMES);
    cursor.addRow(new Object[] { "Alice", "Allison" });
    cursor.addRow(new Object[] { "Bob", "Bobberson" });

    Query query = new CursorQuery(cursor);
    final AtomicInteger count = new AtomicInteger();
    query.asRows(c -> {
      count.incrementAndGet();
      return Name.MAP.invoke(c);
    }).take(1).blockingFirst();
    assertThat(count.get()).isEqualTo(1);
  }

  @Test public void asRowsEmptyWhenNullCursor() {
    Query nully = new Query() {
      @Nullable @Override public Cursor run() {
        return null;
      }
    };

    final AtomicInteger count = new AtomicInteger();
    nully.asRows(cursor -> {
      count.incrementAndGet();
      return Name.MAP.invoke(cursor);
    }).test().assertNoValues().assertComplete();

    assertThat(count.get()).isEqualTo(0);
  }

  static final class Name {
    static final Function1<Cursor, Name> MAP = cursor -> new Name( //
        cursor.getString(cursor.getColumnIndexOrThrow(FIRST_NAME)),
        cursor.getString(cursor.getColumnIndexOrThrow(LAST_NAME)));

    final String first;
    final String last;

    Name(String first, String last) {
      this.first = first;
      this.last = last;
    }

    @Override public boolean equals(Object o) {
      if (o == this) return true;
      if (!(o instanceof Name)) return false;
      Name other = (Name) o;
      return first.equals(other.first) && last.equals(other.last);
    }

    @Override public int hashCode() {
      return first.hashCode() * 17 + last.hashCode();
    }

    @Override public String toString() {
      return "Name[" + first + ' ' + last + ']';
    }
  }

  static final class CursorQuery extends Query {
    private final Cursor cursor;

    CursorQuery(Cursor cursor) {
      this.cursor = cursor;
    }

    @Override public Cursor run() {
      return cursor;
    }
  }
}
