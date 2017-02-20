/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.discovery;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.wildfly.common.Assert;
import org.wildfly.common.annotation.NotNull;

/**
 * A queue for receiving service query answers.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServicesQueue extends AutoCloseable {

    /**
     * Wait for a queue entry to become available.  When this method returns, {@link #poll()} will return a value (or
     * {@code null} if all services have been read) and {@link #take()} will return without blocking.
     *
     * @throws InterruptedException if the calling thread was interrupted while waiting for the next entry
     */
    void await() throws InterruptedException;

    /**
     * Wait for a certain amount of time for a queue entry to become available.
     *
     * @param time the amount of time to wait
     * @param unit the unit of time (must not be {@code null})
     * @throws InterruptedException if the calling thread was interrupted while waiting for the next entry
     */
    void await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Query whether there is a value ready to be read.
     *
     * @return {@code true} if the queue has a value, {@code false} otherwise
     */
    boolean isReady();

    /**
     * Get the location URI of the next entry from the queue without blocking.  Returns {@code null} if there is no entry ready, or if
     * the queue is finished (all services have been read).  Use {@link #isFinished()} to distinguish the cases.
     *
     * @return the next URI, or {@code null} if the queue is not ready or is finished
     */
    default URI poll() {
        final ServiceURL serviceURL = pollService();
        return serviceURL == null ? null : serviceURL.getLocationURI();
    }

    /**
     * Get the location URI of the next entry from the queue, blocking until one is available or the thread is
     * interrupted.  Returns
     * {@code null} if the queue is finished (all services have been read).
     *
     * @return the next URI, or {@code null} if the queue is finished
     * @throws InterruptedException if the calling thread was interrupted while waiting for the next entry
     */
    default URI take() throws InterruptedException {
        final ServiceURL serviceURL = takeService();
        return serviceURL == null ? null : serviceURL.getLocationURI();
    }

    /**
     * Get the next entry from the queue without blocking.  Returns {@code null} if there is no entry ready, or if
     * the queue is finished (all services have been read).  Use {@link #isFinished()} to distinguish the cases.
     *
     * @return the next service URL, or {@code null} if the queue is not ready or is finished
     */
    ServiceURL pollService();

    /**
     * Get the next entry from the queue, blocking until one is available or the thread is interrupted.  Returns
     * {@code null} if the queue is finished (all services have been read).
     *
     * @return the next service URL, or {@code null} if the queue is finished
     * @throws InterruptedException if the calling thread was interrupted while waiting for the next entry
     */
    ServiceURL takeService() throws InterruptedException;

    /**
     * Query whether this queue is finished (all services have been read).
     *
     * @return {@code true} if the queue is finished, {@code false} otherwise
     */
    boolean isFinished();

    /**
     * Cancel any in-progress discovery for this queue.  This method is idempotent.
     */
    void close();

    /**
     * Get a list of problems that occurred during discovery.
     *
     * @return a list of problems that occurred during discovery (not {@code null})
     */
    @NotNull
    List<Throwable> getProblems();

    /**
     * Create a version of this queue which has an absolute timeout, relative to when this method is called.
     *
     * @param time the timeout time
     * @param unit the timeout unit (must not be {@code null})
     * @return the services queue with a timeout (not {@code null})
     */
    default ServicesQueue withTimeout(final long time, final TimeUnit unit) {
        Assert.checkNotNullParam("unit", unit);
        final long timeoutNanos = unit.toNanos(time);
        final long start = System.nanoTime();
        return new ServicesQueue() {

            public void await() throws InterruptedException {
                long elapsed = System.nanoTime() - start;
                if (elapsed < timeoutNanos) {
                    ServicesQueue.this.await(elapsed, TimeUnit.NANOSECONDS);
                }
            }

            public void await(final long time, final TimeUnit unit) throws InterruptedException {
                long elapsed = System.nanoTime() - start;
                long callerNs = unit.toNanos(time);
                ServicesQueue.this.await(Math.min(timeoutNanos - elapsed, callerNs), TimeUnit.NANOSECONDS);
            }

            public boolean isReady() {
                return ServicesQueue.this.isReady();
            }

            public URI poll() {
                return ServicesQueue.this.poll();
            }

            public URI take() throws InterruptedException {
                await();
                return poll();
            }

            public ServiceURL pollService() {
                return ServicesQueue.this.pollService();
            }

            public ServiceURL takeService() throws InterruptedException {
                await();
                return pollService();
            }

            public boolean isFinished() {
                return ServicesQueue.this.isFinished() || ! isReady() && timeoutNanos > System.nanoTime() - start;
            }

            public void close() {
                ServicesQueue.this.close();
            }

            @NotNull
            public List<Throwable> getProblems() {
                return ServicesQueue.this.getProblems();
            }
        };
    }
}
