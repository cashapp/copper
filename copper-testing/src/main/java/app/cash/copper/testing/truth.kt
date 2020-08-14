package app.cash.copper.testing

import com.google.common.truth.ThrowableSubject
import com.google.common.truth.Truth.assertThat

fun Throwable.assert(body: ThrowableSubject.() -> Unit) = assertThat(this).apply(body)
