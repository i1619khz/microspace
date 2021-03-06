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

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author i1619kHz
 */
final class ExecutorServiceScheduler implements Scheduler, Serializable {
    static final Logger logger = LoggerFactory.getLogger(ExecutorServiceScheduler.class.getName());
    static final long serialVersionUID = 1;

    final ScheduledExecutorService scheduledExecutorService;

    ExecutorServiceScheduler(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = requireNonNull(scheduledExecutorService);
    }

    @Override
    public Future<?> schedule(Executor executor, Runnable command, long delay, TimeUnit unit) {
        requireNonNull(executor);
        requireNonNull(command);
        requireNonNull(unit);

        if (scheduledExecutorService.isShutdown()) {
            return DisabledFuture.INSTANCE;
        }
        return scheduledExecutorService.schedule(() -> {
            try {
                executor.execute(command);
            } catch (Throwable t) {
                logger.warn("Exception thrown when submitting scheduled task", t);
                throw t;
            }
        }, delay, unit);
    }
}
