/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
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

package org.wildfly.discovery.spi;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.discovery.FilterSpec;
import org.wildfly.discovery.ServiceType;

/**
 * A discovery provider.  This interface is implemented by all discovery provider implementations.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface BlockingDiscoveryProvider {

    /**
     * Attempt to discover implementations of a service synchronously.  Matches must be written to the
     * {@link DiscoveryResult#addMatch(java.net.URI)} method on the {@code result} parameter as they are found.  The
     * {@link DiscoveryResult#complete()} method must be called when the discovery process is complete or has timed out
     * or failed for any other reason, otherwise discovery clients may hang indefinitely.
     *
     * @param serviceType the service type to match
     * @param filterSpec the service attribute filter expression, or {@code null} to return all matches
     * @param result the discovery result
     * @throws InterruptedException if discovery was interrupted for some reason
     */
    void discover(ServiceType serviceType, FilterSpec filterSpec, DiscoveryResult result) throws InterruptedException;

    /**
     * Convert this provider to a non-blocking provider which uses a shared, private thread pool to dispatch discovery
     * requests.
     *
     * @return the provider (not {@code null})
     */
    default DiscoveryProvider toDiscoveryProvider() {
        return (serviceType, filterSpec, result) -> {
            Executor executor = BlockingThreadPool.EXECUTOR;
            AtomicReference<Object> threadRef = new AtomicReference<>();
            executor.execute(() -> {
                try {
                    final Thread currentThread = Thread.currentThread();
                    if (threadRef.compareAndSet(null, currentThread)) try {
                        BlockingDiscoveryProvider.this.discover(serviceType, filterSpec, result);
                    } catch (InterruptedException e) {
                        currentThread.interrupt();
                    } finally {
                        threadRef.set(null);
                    }
                } finally {
                    result.complete();
                }
            });
            return () -> {
                final Object val = threadRef.getAndSet("sentinel");
                if (val instanceof Thread) {
                    ((Thread)val).interrupt();
                }
            };
        };
    }
}
