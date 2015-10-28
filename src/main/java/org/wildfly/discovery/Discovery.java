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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryRequest;
import org.wildfly.discovery.spi.DiscoveryResult;

/**
 * The service discovery API.  Each discovery instance is associated with discovery providers which are able to
 * provide answers to discovery queries.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Discovery {

    private static final URI END_MARK = URI.create("DUMMY:DUMMY");

    private final DiscoveryProvider[] providers;

    Discovery(final DiscoveryProvider... providers) {
        this.providers = providers;
    }

    /**
     * Perform a service discovery.  The returned services queue is populated as discovery answers become available.
     * Answers may be cached within each provider.  The order of answers is not significant and can vary from call to
     * call, especially with asynchronous discovery mechanisms.  The returned service queue may be closed to indicate
     * no further interest in query answers, and for this purpose it implements {@link AutoCloseable} in order to
     * facilitate simple usage in a {@code try}-with-resources block.
     *
     * @param serviceType the abstract or concrete type of service to search for
     * @param filterSpec the service filter specification
     * @return the services queue
     */
    public ServicesQueue discover(ServiceType serviceType, FilterSpec filterSpec) {
        if (serviceType == null) {
            throw new IllegalArgumentException("serviceType is null");
        }
        final LinkedBlockingQueue<URI> queue = new LinkedBlockingQueue<>();
        final DiscoveryRequest[] requests = new DiscoveryRequest[providers.length];
        for (int i = 0; i < providers.length; i++) {
            requests[i] = providers[i].discover(serviceType, filterSpec, new ListDiscoveryResult(queue));
        }
        return new ServicesQueue() {
            private int count = providers.length;
            private URI next;

            public void await() throws InterruptedException {
                while (next == null && count > 0) {
                    next = queue.take();
                    if (next == END_MARK) {
                        next = null;
                        // sentinel value to indicate a provider completed
                        if (-- count == 0) {
                            return;
                        }
                    }
                }
            }

            public void await(final long time, final TimeUnit unit) throws InterruptedException {
                long remaining = unit.toNanos(time);
                long mark = System.nanoTime();
                long now;
                while (next == null && count > 0 && remaining > 0L) {
                    next = queue.poll(remaining, TimeUnit.NANOSECONDS);
                    now = System.nanoTime();
                    remaining -= Math.max(1L, now - mark);
                    if (next == END_MARK) {
                        next = null;
                        // sentinel value to indicate a provider completed
                        if (-- count == 0) {
                            return;
                        }
                    }
                }
            }

            public boolean isReady() {
                return next != null || count == 0;
            }

            public URI poll() {
                try {
                    return next;
                } finally {
                    next = null;
                }
            }

            public URI take() throws InterruptedException {
                await();
                return poll();
            }

            public boolean isFinished() {
                return next == null && count == 0;
            }

            public void close() {
                if (! isFinished()) for (DiscoveryRequest request : requests) {
                    request.cancel();
                }
            }
        };
    }

    /**
     * Create a discovery object with the given providers.  The given {@code providers} argument and its array
     * elements may not be {@code null}.
     *
     * @param providers the discovery providers
     * @return the discovery object
     */
    public static Discovery create(DiscoveryProvider... providers) {
        if (providers == null) {
            throw new IllegalArgumentException("providers is null");
        }
        final DiscoveryProvider[] clone = providers.clone();
        for (int i = 0; i < clone.length; i++) {
            final DiscoveryProvider discoveryProvider = clone[i];
            if (discoveryProvider == null) {
                throw new IllegalArgumentException("providers[" + i + "] is null");
            }
        }
        return new Discovery(clone);
    }

    static final class ListDiscoveryResult implements DiscoveryResult {
        private final AtomicBoolean done = new AtomicBoolean(false);
        private final BlockingQueue<URI> queue;

        ListDiscoveryResult(final BlockingQueue<URI> queue) {
            this.queue = queue;
        }

        public void complete() {
            if (done.compareAndSet(false, true)) {
                queue.add(END_MARK);
            }
        }

        public void addMatch(final URI uri) {
            if (uri != null && ! done.get()) {
                // if the queue is full, drop
                queue.offer(uri);
            }
        }
    }
}
