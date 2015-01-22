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
     * Query whether there is a value ready to be read.
     *
     * @return {@code true} if the queue has a value, {@code false} otherwise
     */
    boolean isReady();

    /**
     * Get the next entry from the queue without blocking.  Returns {@code null} if there is no entry ready, or if
     * the queue is finished (all services have been read).  Use {@link #isFinished()} to distinguish the cases.
     *
     * @return the next URI, or {@code null} if the queue is not ready or is finished
     */
    URI poll();

    /**
     * Get the next entry from the queue, blocking until one is available or the thread is interrupted.  Returns
     * {@code null} if the queue is finished (all services have been read).
     *
     * @return the next URI, or {@code null} if the queue is finished
     * @throws InterruptedException if the calling thread was interrupted while waiting for the next entry
     */
    URI take() throws InterruptedException;

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
}
