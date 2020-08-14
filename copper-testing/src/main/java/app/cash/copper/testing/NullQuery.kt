package app.cash.copper.testing

import android.database.Cursor
import app.cash.copper.Query

object NullQuery : Query {
  override fun run(): Cursor? {
    return null
  }
}
