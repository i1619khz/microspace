/*
 * MIT License
 *
 * Copyright (c) 2021 1619kHz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.microspace.context.scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author i1619kHz
 */
@FunctionalInterface
public interface Scheduler {
    /**
     * Returns a scheduler that always returns a successfully completed future.
     *
     * @return a scheduler that always returns a successfully completed future
     */
    static Scheduler disabledScheduler() {
        return DisabledScheduler.INSTANCE;
    }

    /**
     * Returns a scheduler that uses the system-wide scheduling thread if available, or else returns
     * {@link #disabledScheduler()} if not present. This scheduler is provided in Java 9 or above
     * by using {@link CompletableFuture} {@code delayedExecutor}.
     *
     * @return a scheduler that uses the system-wide scheduling thread if available, or else a
     * disabled scheduler
     */
    static Scheduler systemScheduler() {
        return SystemScheduler.isPresent() ? SystemScheduler.INSTANCE : disabledScheduler();
    }

    /**
     * Returns a scheduler that delegates to the a {@link ScheduledExecutorService}.
     *
     * @param scheduledExecutorService the executor to schedule on
     * @return a scheduler that delegates to the a {@link ScheduledExecutorService}
     */
    static Scheduler forScheduledExecutorService(
            ScheduledExecutorService scheduledExecutorService) {
        return new ExecutorServiceScheduler(scheduledExecutorService);
    }

    /**
     * Returns a scheduler that suppresses and logs any exception thrown by the delegate
     * {@code scheduler}.
     *
     * @param scheduler the scheduler to delegate to
     * @return an scheduler that suppresses and logs any exception thrown by the delegate
     */
    static Scheduler guardedScheduler(Scheduler scheduler) {
        return (scheduler instanceof GuardedScheduler) ? scheduler : new GuardedScheduler(scheduler);
    }

    /**
     * Returns a future that will submit the task to the given executor after the given delay.
     *
     * @param executor the executor to run the task
     * @param command  the runnable task to schedule
     * @param delay    how long to delay, in units of {@code unit}
     * @param unit     a {@code TimeUnit} determining how to interpret the {@code delay} parameter
     * @return a scheduled future representing pending completion of the task
     */
    Future<?> schedule(Executor executor, Runnable command, long delay, TimeUnit unit);
}
