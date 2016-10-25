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

import org.wildfly.common.Assert;
import org.wildfly.discovery.impl.AggregateDiscoveryProvider;
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

    static final ServiceURL END_MARK = new ServiceURL.Builder().setUri(URI.create("DUMMY:DUMMY")).create();

    private final DiscoveryProvider provider;

    Discovery(final DiscoveryProvider provider) {
        this.provider = provider;
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
        Assert.checkNotNullParam("serviceType", serviceType);
        final LinkedBlockingQueue<ServiceURL> queue = new LinkedBlockingQueue<>();
        return new BlockingQueueServicesQueue(queue, provider.discover(serviceType, filterSpec, new BlockingQueueDiscoveryResult(queue)));
    }

    /**
     * Create a discovery object with the given providers.  The given {@code providers} argument and its array
     * elements may not be {@code null}.
     *
     * @param providers the discovery providers (must not be {@code null})
     * @return the discovery object
     */
    public static Discovery create(DiscoveryProvider... providers) {
        Assert.checkNotNullParam("providers", providers);
        final DiscoveryProvider[] clone = providers.clone();
        final int length = clone.length;
        for (int i = 0; i < length; i++) {
            Assert.checkNotNullArrayParam("providers", i, clone[i]);
        }
        if (clone.length == 0) {
            return new Discovery(DiscoveryProvider.EMPTY);
        } else if (clone.length == 1) {
            return new Discovery(clone[0]);
        } else {
            return new Discovery(new AggregateDiscoveryProvider(clone));
        }
    }

    /**
     * Create a discovery object with the given single provider.  The given {@code provider} argument may not be {@code null}.
     *
     * @param provider the discovery provider (must not be {@code null})
     * @return the discovery object
     */
    public static Discovery create(DiscoveryProvider provider) {
        Assert.checkNotNullParam("provider", provider);
        return new Discovery(provider);
    }

    // Internal classes

    static final class BlockingQueueDiscoveryResult implements DiscoveryResult {
        private final AtomicBoolean done = new AtomicBoolean(false);
        private final BlockingQueue<ServiceURL> queue;

        BlockingQueueDiscoveryResult(final BlockingQueue<ServiceURL> queue) {
            this.queue = queue;
        }

        public void complete() {
            if (done.compareAndSet(false, true)) {
                queue.add(END_MARK);
            }
        }

        public void addMatch(final ServiceURL serviceURL) {
            if (serviceURL != null && ! done.get()) {
                // if the queue is full, drop
                queue.offer(serviceURL);
            }
        }
    }

    static final class BlockingQueueServicesQueue implements ServicesQueue {
        private final LinkedBlockingQueue<ServiceURL> queue;
        private final DiscoveryRequest request;
        private ServiceURL next;
        private boolean done;

        BlockingQueueServicesQueue(final LinkedBlockingQueue<ServiceURL> queue, final DiscoveryRequest request) {
            this.queue = queue;
            this.request = request;
        }

        public void await() throws InterruptedException {
            if (done) return;
            while (next == null) {
                next = queue.take();
                if (next == END_MARK) {
                    next = null;
                    // sentinel value to indicate the provider completed
                    done = true;
                    return;
                }
            }
        }

        public void await(final long time, final TimeUnit unit) throws InterruptedException {
            long remaining = unit.toNanos(time);
            long mark = System.nanoTime();
            long now;
            while (next == null && ! done && remaining > 0L) {
                next = queue.poll(remaining, TimeUnit.NANOSECONDS);
                now = System.nanoTime();
                remaining -= Math.max(1L, now - mark);
                if (next == END_MARK) {
                    next = null;
                    // sentinel value to indicate the provider completed
                    done = true;
                    return;
                }
            }
        }

        public boolean isReady() {
            return next != null || done;
        }

        public ServiceURL pollService() {
            try {
                return next;
            } finally {
                next = null;
            }
        }

        public ServiceURL takeService() throws InterruptedException {
            await();
            return pollService();
        }

        public boolean isFinished() {
            return next == null && done;
        }

        public void close() {
            if (! isFinished()) {
                request.cancel();
            }
        }
    }
}
