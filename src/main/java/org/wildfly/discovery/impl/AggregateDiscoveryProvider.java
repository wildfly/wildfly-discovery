/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
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

package org.wildfly.discovery.impl;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.wildfly.common.Assert;
import org.wildfly.discovery.FilterSpec;
import org.wildfly.discovery.ServiceType;
import org.wildfly.discovery.ServiceURL;
import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryRequest;
import org.wildfly.discovery.spi.DiscoveryResult;

/**
 * A discovery provider which aggregates multiple other providers together.  The aggregate request is complete when
 * all of the delegate provider requests are complete.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AggregateDiscoveryProvider implements DiscoveryProvider {
    private final DiscoveryProvider[] delegates;

    /**
     * Construct a new instance.
     *
     * @param delegates the array of delegates (must not be {@code null})
     */
    public AggregateDiscoveryProvider(final DiscoveryProvider[] delegates) {
        Assert.checkNotNullParam("delegates", delegates);
        this.delegates = delegates;
    }

    public DiscoveryRequest discover(final ServiceType serviceType, final FilterSpec filterSpec, final Predicate<ServiceURL> predicate, final DiscoveryResult result) {
        final AtomicInteger count = new AtomicInteger(delegates.length);
        final DiscoveryRequest[] delegateRequests = new DiscoveryRequest[delegates.length];
        for (int i = 0, delegatesLength = delegates.length; i < delegatesLength; i++) {
            final DiscoveryProvider delegate = delegates[i];
            if (delegate != null) {
                delegateRequests[i] = delegate.discover(serviceType, filterSpec, predicate, new AggregatingDiscoveryResult(result, count));
            } else {
                handleComplete(count, result);
            }
        }
        return new AggregatingDiscoveryRequest(delegateRequests);
    }

    void handleComplete(AtomicInteger count, DiscoveryResult delegate) {
        if (count.decrementAndGet() == 0) {
            delegate.complete();
        }
    }

    static class AggregatingDiscoveryRequest implements DiscoveryRequest {
        private final DiscoveryRequest[] delegateRequests;

        AggregatingDiscoveryRequest(final DiscoveryRequest[] delegateRequests) {
            this.delegateRequests = delegateRequests;
        }

        public void cancel() {
            for (DiscoveryRequest request : delegateRequests) {
                if (request != null) request.cancel();
            }
        }
    }

    @SuppressWarnings("serial")
    final class AggregatingDiscoveryResult extends AtomicBoolean implements DiscoveryResult {
        private final DiscoveryResult delegate;
        private final AtomicInteger count;

        AggregatingDiscoveryResult(final DiscoveryResult delegate, final AtomicInteger count) {
            this.delegate = delegate;
            this.count = count;
        }

        public void complete() {
            if (compareAndSet(false, true)) {
                handleComplete(count, delegate);
            }
        }

        public void reportProblem(final Throwable description) {
            if (! get()) delegate.reportProblem(description);
        }

        public void addMatch(final URI uri) {
            if (! get()) delegate.addMatch(uri);
        }

        public void addMatch(final ServiceURL serviceURL) {
            if (! get()) delegate.addMatch(serviceURL);
        }
    }
}
