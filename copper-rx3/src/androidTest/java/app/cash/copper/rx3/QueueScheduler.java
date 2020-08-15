/*
 * Copyright (C) 2016 Square, Inc.
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
package app.cash.copper.rx3;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

final class QueueScheduler extends Scheduler {
  private final BlockingDeque<Runnable> events = new LinkedBlockingDeque<>();

  public Runnable awaitRunnable() throws InterruptedException {
    return events.pollFirst(5, TimeUnit.SECONDS);
  }

  @Override public Worker createWorker() {
    return new TestWorker();
  }

  class TestWorker extends Worker {
    @Override
    public Disposable schedule(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
      events.add(run);
      return Disposable.fromRunnable(() -> events.remove(run));
    }

    @Override public void dispose() {
    }

    @Override public boolean isDisposed() {
      return false;
    }
  }
}
