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

import android.content.ContentResolver
import android.test.ProviderTestCase2
import app.cash.copper.testing.TestContentProvider
import app.cash.copper.testing.TestContentProvider.AUTHORITY
import app.cash.copper.testing.TestContentProvider.TABLE
import app.cash.copper.testing.TestContentProvider.testValues
import app.cash.copper.testing.assert
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
class FlowContentResolverTest : ProviderTestCase2<TestContentProvider>(
  TestContentProvider::class.java, AUTHORITY.authority
) {
  private lateinit var contentResolver: ContentResolver

  override fun setUp() {
    super.setUp()
    contentResolver = mockContentResolver
    provider.init(context.contentResolver)
  }

  fun testCreateQueryObservesInsert() = runBlocking {
    contentResolver.observeQuery(TABLE).test {
      awaitItem().assert {
        isExhausted()
      }

      contentResolver.insert(TABLE, testValues("key1", "val1"))
      awaitItem().assert {
        hasRow("key1", "val1")
        isExhausted()
      }

      cancel()
    }
  }

  fun testCreateQueryObservesUpdate() = runBlocking {
    contentResolver.insert(TABLE, testValues("key1", "val1"))

    contentResolver.observeQuery(TABLE).test {
      awaitItem().assert {
        hasRow("key1", "val1")
        isExhausted()
      }

      contentResolver.update(TABLE, testValues("key1", "val2"), null, null)
      awaitItem().assert {
        hasRow("key1", "val2")
        isExhausted()
      }

      cancel()
    }
  }

  fun testCreateQueryObservesDelete() = runBlocking {
    contentResolver.insert(TABLE, testValues("key1", "val1"))

    contentResolver.observeQuery(TABLE).test {
      awaitItem().assert {
        hasRow("key1", "val1")
        isExhausted()
      }

      contentResolver.delete(TABLE, null, null)
      awaitItem().assert {
        isExhausted()
      }

      cancel()
    }
  }
}
