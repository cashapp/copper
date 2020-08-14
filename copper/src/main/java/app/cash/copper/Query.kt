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
package app.cash.copper

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import androidx.annotation.CheckResult
import androidx.annotation.WorkerThread

/** An executable query. */
interface Query {
  /**
   * Execute the query on the underlying provider and return the resulting cursor.
   *
   * @return A [Cursor] with query results, or `null` when the query could not be
   * executed due to a problem with the underlying store. Unfortunately it is not well documented
   * when `null` is returned. It usually involves a problem in communicating with the
   * underlying store and should either be treated as failure or ignored for retry at a later
   * time.
   */
  @CheckResult
  @WorkerThread
  fun run(): Cursor?
}

/** [Query] wrapper around [ContentResolver.query] */
class ContentResolverQuery(
  private val contentResolver: ContentResolver,
  private val uri: Uri,
  private val projection: Array<String>?,
  private val selection: String?,
  private val selectionArgs: Array<String>?,
  private val sortOrder: String?
) : Query {
  override fun run(): Cursor? {
    return contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
  }
}
