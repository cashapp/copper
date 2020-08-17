# Copper

A content provider wrapper for reactive queries with Kotlin coroutines `Flow` or
RxJava `Observable`.

```kotlin
contentResolver.observeQuery(uri).collect { query ->
  // ...
}
```


## Download

<!--
```groovy
// Kotlin coroutines:
implementation 'app.cash.copper:copper-flow:1.0.0'

// RxJava 3
implementation 'app.cash.copper:copper-rx3:1.0.0'

// RxJava 2
implementation 'app.cash.copper:copper-rx2:1.0.0'
```
-->

<details>
<summary>Snapshots of the development version are available in Sonatype's snapshots repository.</summary>
<p>

```groovy
repositories {
  maven {
    url 'https://oss.sonatype.org/content/repositories/snapshots/'
  }
}
dependencies {
  testImplementation 'app.cash.copper:copper-flow:1.0.0-SNAPSHOT'
  testImplementation 'app.cash.copper:copper-rx2:1.0.0-SNAPSHOT'
  testImplementation 'app.cash.copper:copper-rx3:1.0.0-SNAPSHOT'
}
```

</p>
</details>


## Usage

Given a [`ContentResolver`](https://developer.android.com/reference/android/content/ContentResolver),
change your calls from `query` to `observeQuery` for a reactive version.

```kotlin
contentResolver.observeQuery(uri).collect { query ->
  query.run()?.let { cursor ->
    // ...
  }
}
```

Unlike `query`, `observeQuery` returns a `Query` object that you must call `run()` on in order to
execute the underlying query for a `Cursor`. This ensures that intermediate operators which cache
values do not leak resources and that consumers have access to the full lifetime of the cursor.

Instead of dealing with a `Cursor` directly, operators are provided which help convert the
contained values into semantic types:

```kotlin
contentResolver.observeQuery(uri)
  .mapToOne { cursor ->
    Employee(cursor.getString(0), cursor.getString(1))
  }
  .collect {
    println(it)
  }
```
```
Employee(id=bob, name=Bob Bobberson)
```

The `mapToOne` operator takes a query which returns a single row and invokes the lambda to map the
cursor to your desired type. If your query returns zero or one rows, the coroutine artifact has a
`mapToOneOrNull` operator and the RxJava artifacts have a `mapToOptional` operator.

If your query returns a list, call `mapToList` with the same lambda:

```kotlin
contentResolver.observeQuery(uri)
  .mapToList { cursor ->
    Employee(cursor.getString(0), cursor.getString(1))
  }
  .collect {
    println(it)
  }
```
```
[Employee(id=alice, name=Alice Alison), Employee(id=bob, name=Bob Bobberson)]
```


# License

    Copyright 2015 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

Note: This library was forked from [SQLBrite](https://github.com/square/sqlbrite).
