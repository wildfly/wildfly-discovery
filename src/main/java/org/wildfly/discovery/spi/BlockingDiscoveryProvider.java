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
import java.util.function.Predicate;

import org.wildfly.common.Assert;
import org.wildfly.discovery.Discovery;
import org.wildfly.discovery.FilterSpec;
import org.wildfly.discovery.ServiceType;
import org.wildfly.discovery.ServiceURL;

/**
 * A blocking discovery provider.  This interface should be implemented by discovery providers which are not capable
 * of running asynchronously.  The {@link #toDiscoveryProvider(Executor)} method must be used to convert providers of
 * this type to a type which can be used in a {@link Discovery} instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface BlockingDiscoveryProvider {

    /**
     * Attempt to discover implementations of a service synchronously.  Matches must be written to the
     * {@link DiscoveryResult#addMatch(ServiceURL)} method on the {@code result} parameter as they are found.  The
     * {@link DiscoveryResult#complete()} method may be called when the discovery process is complete or has timed out
     * or failed for any other reason.  If the {@code complete()} method is not called, the discovery is assumed to
     * be complete when the method returns.
     *
     * @param serviceType the service type to match
     * @param filterSpec the service attribute filter expression, or {@code null} to return all matches
     * @param predicate the predicate on ServiceURLs to satisfy or (@code null) to return all matches
     * @param result the discovery result
     * @throws InterruptedException if discovery was interrupted for some reason
     */
    void discover(ServiceType serviceType, FilterSpec filterSpec, Predicate<ServiceURL> predicate, DiscoveryResult result) throws InterruptedException;

    /**
     * Convert this provider to a non-blocking provider which uses the given thread pool to dispatch discovery
     * requests.  If the task is rejected by the executor, then discovery is immediately terminated.  The task thread
     * is interrupted if discovery is to be cancelled.
     *
     * @param executor the executor to use for task dispatch (must not be {@code null})
     * @return the provider (not {@code null})
     */
    default DiscoveryProvider toDiscoveryProvider(Executor executor) {
        Assert.checkNotNullParam("executor", executor);
        return (serviceType, filterSpec, predicate, result) -> {
            AtomicReference<Object> threadRef = new AtomicReference<>();
            try {
                executor.execute(() -> {
                    try {
                        final Thread currentThread = Thread.currentThread();
                        if (threadRef.compareAndSet(null, currentThread)) try {
                            BlockingDiscoveryProvider.this.discover(serviceType, filterSpec, predicate, result);
                        } catch (InterruptedException e) {
                            currentThread.interrupt();
                        } finally {
                            threadRef.set(null);
                        }
                    } finally {
                        result.complete();
                    }
                });
            } catch (Throwable t) {
                result.complete();
            }
            return () -> {
                final Object val = threadRef.getAndSet("sentinel");
                if (val instanceof Thread) {
                    ((Thread)val).interrupt();
                }
            };
        };
    }
}
