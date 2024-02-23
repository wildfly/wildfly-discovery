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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;
import org.wildfly.common.Assert;
import org.wildfly.common.annotation.NotNull;
import org.wildfly.common.context.ContextManager;
import org.wildfly.common.context.Contextual;
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
public final class Discovery implements Contextual<Discovery> {

    private static final Logger log = Logger.getLogger("org.wildfly.discovery");
    private static final ServiceURL END_MARK = new ServiceURL.Builder().setUri(URI.create("DUMMY:DUMMY")).create();

    private static final ContextManager<Discovery> CONTEXT_MANAGER;

    protected static int discoveryCancellationDelay = 0;

    static {
        CONTEXT_MANAGER = new ContextManager<Discovery>(Discovery.class, "org.wildfly.discovery");
        CONTEXT_MANAGER.setGlobalDefaultSupplier(() -> create(ConfiguredProvider.INSTANCE));

        String delay = System.getProperty("wildfly.discovery.cancellation.delay");
        if (delay != null)
            discoveryCancellationDelay = Integer.valueOf(delay);
    }

    private final DiscoveryProvider provider;

    Discovery(final DiscoveryProvider provider) {
        this.provider = provider;
    }

    /**
     * Get the instance context manager.  Delegates to {@link #getContextManager()}.
     *
     * @return the instance context manager (not {@code null})
     */
    public ContextManager<Discovery> getInstanceContextManager() {
        return CONTEXT_MANAGER;
    }

    /**
     * Get the context manager.
     *
     * @return the context manager (not {@code null})
     */
    public static ContextManager<Discovery> getContextManager() {
        return CONTEXT_MANAGER;
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
        return discover(serviceType, filterSpec, Long.MAX_VALUE, TimeUnit.DAYS);
    }

    /**
     * Perform a service discovery.  The returned services queue is populated as discovery answers become available.
     * Answers may be cached within each provider.  The order of answers is not significant and can vary from call to
     * call, especially with asynchronous discovery mechanisms.  The returned service queue may be closed to indicate
     * no further interest in query answers, and for this purpose it implements {@link AutoCloseable} in order to
     * facilitate simple usage in a {@code try}-with-resources block.
     *
     * This method allows a timeout to be specified.
     *
     * @param serviceType the abstract or concrete type of service to search for
     * @param filterSpec the service filter specification
     * @param timeout the timeout duration
     * @param timeUnit the unit of time for the timeout
     * @return the services queue
     */
    public ServicesQueue discover(ServiceType serviceType, FilterSpec filterSpec, long timeout, TimeUnit timeUnit) {
        Assert.checkMinimumParameter("timeout", 1, timeout);
        Assert.assertNotNull(timeUnit);

        Assert.checkNotNullParam("serviceType", serviceType);
        final LinkedBlockingQueue<ServiceURL> queue = new LinkedBlockingQueue<>();
        final CopyOnWriteArrayList<Throwable> problems = new CopyOnWriteArrayList<>();
        final DiscoveryResult result = new BlockingQueueDiscoveryResult(queue, problems);

        log.tracef("Calling discover(%s, %s) with result instance %s and timeout of %s %s\n", serviceType, filterSpec, result, timeout, timeUnit);

        return new BlockingQueueServicesQueue(queue, problems, provider.discover(serviceType, filterSpec, result), timeout, timeUnit);
    }

    /**
     * Perform a service discovery.  The returned services queue is populated as discovery answers become available.
     * Answers may be cached within each provider.  The order of answers is not significant and can vary from call to
     * call, especially with asynchronous discovery mechanisms.  The returned service queue may be closed to indicate
     * no further interest in query answers, and for this purpose it implements {@link AutoCloseable} in order to
     * facilitate simple usage in a {@code try}-with-resources block.
     *
     * @param description the service description (must not be {@code null})
     * @return the services queue
     */
    public ServicesQueue discover(ServiceDescription description) {
        Assert.checkNotNullParam("description", description);
        return discover(description.getServiceType(), description.getFilterSpec());
    }

    public void processMissingTarget(URI location, Exception cause){
        provider.processMissingTarget(location, cause);
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
        private final CopyOnWriteArrayList<Throwable> problems;

        BlockingQueueDiscoveryResult(final BlockingQueue<ServiceURL> queue, final CopyOnWriteArrayList<Throwable> problems) {
            this.queue = queue;
            this.problems = problems;
        }

        public void complete() {
            if (done.compareAndSet(false, true)) {
                queue.add(END_MARK);

                log.tracef("Discovery complete on %s\n", this);
            }
        }

        public void reportProblem(final Throwable description) {
            Assert.checkNotNullParam("description", description);
            problems.add(description);
            log.tracef(description, "Reported problem on %s", this);
        }

        public void addMatch(final ServiceURL serviceURL) {
            if (serviceURL != null && ! done.get()) {
                log.tracef("Adding service URL match \"%s\" to %s", serviceURL, this);
                // if the queue is full, drop
                queue.offer(serviceURL);
            } else {
                log.tracef("Ignoring service URL match \"%s\" to %s", serviceURL, this);
            }
        }
    }

    static final class BlockingQueueServicesQueue implements ServicesQueue {
        private final LinkedBlockingQueue<ServiceURL> queue;
        private final CopyOnWriteArrayList<Throwable> problems;
        private final DiscoveryRequest request;
        private final long timeout;
        private final TimeUnit timeUnit;
        private ServiceURL next;
        private boolean done;

        BlockingQueueServicesQueue(final LinkedBlockingQueue<ServiceURL> queue, final CopyOnWriteArrayList<Throwable> problems, final DiscoveryRequest request) {
            this(queue, problems, request, Long.MAX_VALUE, TimeUnit.DAYS);
        }

        BlockingQueueServicesQueue(final LinkedBlockingQueue<ServiceURL> queue, final CopyOnWriteArrayList<Throwable> problems, final DiscoveryRequest request, final long time, final TimeUnit timeUnit) {
            this.queue = queue;
            this.problems = problems;
            this.request = request;
            this.timeout = time;
            this.timeUnit = timeUnit;
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

        @Override
        public ServiceURL takeService(long timeout, TimeUnit timeUnit) throws InterruptedException {
            if (timeout <= 0) timeout = Long.MAX_VALUE;
            await(timeout, timeUnit);
            return pollService();
        }

        public boolean isFinished() {
            return next == null && done;
        }

        public void close() {

                Thread canceller = new Thread(){
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(discoveryCancellationDelay);
                        } catch(InterruptedException e){

                        }
                        if (! isFinished()) {
                            request.cancel();
                        }
                    }
                };
                canceller.start();

        }

        @NotNull
        public List<Throwable> getProblems() {
            return problems;
        }
    }
}
