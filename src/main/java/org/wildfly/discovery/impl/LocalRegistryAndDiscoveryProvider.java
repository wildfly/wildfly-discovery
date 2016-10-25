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

import static java.lang.Math.max;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.wildfly.common.Assert;
import org.wildfly.discovery.AggregateServiceRegistration;
import org.wildfly.discovery.FilterSpec;
import org.wildfly.discovery.ServiceRegistration;
import org.wildfly.discovery.ServiceType;
import org.wildfly.discovery.ServiceURL;
import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryRequest;
import org.wildfly.discovery.spi.DiscoveryResult;
import org.wildfly.discovery.spi.RegistryProvider;

/**
 * A local in-memory service registry and discovery provider.  Services registered with this provider can be subsequently
 * discovered.  Since all operations happen in-memory, the discovery provider always completes immediately.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LocalRegistryAndDiscoveryProvider implements RegistryProvider, DiscoveryProvider {
    private final CopyOnWriteArrayList<Handle> handles = new CopyOnWriteArrayList<>();

    public ServiceRegistration registerService(final ServiceURL serviceURL) {
        Assert.checkNotNullParam("serviceURL", serviceURL);
        final Handle handle = new Handle(serviceURL, true);
        handles.add(handle);
        return handle;
    }

    public ServiceRegistration registerServices(final ServiceURL... serviceURLs) {
        Assert.checkNotNullParam("serviceURLs", serviceURLs);
        Handle[] array = new Handle[serviceURLs.length];
        for (int i = 0; i < serviceURLs.length; i++) {
            final ServiceURL serviceURL = serviceURLs[i];
            array[i] = new Handle(serviceURL, false);
        }
        final List<Handle> list = Arrays.asList(array);
        handles.addAll(list);
        return new AggregateHandle(list, array);
    }

    public DiscoveryRequest discover(final ServiceType serviceType, final FilterSpec filterSpec, final DiscoveryResult result) {
        ServiceURL serviceURL;
        for (Handle handle : handles) {
            if (! handle.isOpenAndActive()) {
                continue;
            }
            serviceURL = handle.getServiceURL();
            if (serviceType.implies(serviceURL) && serviceURL.satisfies(filterSpec)) {
                result.addMatch(serviceURL);
            }
        }
        result.complete();
        return DiscoveryRequest.NULL;
    }

    private static final long stamp = System.nanoTime();

    static long nowMicros() {
        return max(0L, (System.nanoTime() - stamp >>> 1) / 500L);
    }

    final class AggregateHandle extends AggregateServiceRegistration {
        private final List<Handle> registrations;

        AggregateHandle(final List<Handle> asList, final Handle... registrations) {
            super((ServiceRegistration[]) registrations);
            this.registrations = asList;
        }

        public void close() {
            // remove far more efficiently
            handles.removeAll(this.registrations);
            super.close();
        }
    }

    final class Handle implements ServiceRegistration {
        @SuppressWarnings("NumericOverflow")
        private static final long FLAG_CLOSED       = 1L << 63;
        private static final long FLAG_DEACTIVATED  = 1L << 62;
        private static final long TIME_MASK         = (1L << 62) - 1;

        private final AtomicLong state = new AtomicLong(0);
        private final ServiceURL serviceURL;
        private final boolean remove;

        Handle(final ServiceURL serviceURL, final boolean remove) {
            this.serviceURL = serviceURL;
            this.remove = remove;
        }

        public void close() {
            if (remove) handles.remove(this);
            state.set(FLAG_CLOSED);
        }

        public void deactivate() {
            final AtomicLong state = this.state;
            long oldVal;
            do {
                oldVal = state.get();
                if ((oldVal & (FLAG_CLOSED | FLAG_DEACTIVATED)) != 0L) {
                    // nothing to do
                    return;
                }
            } while (! state.compareAndSet(oldVal, FLAG_DEACTIVATED));
        }

        public void activate() {
            final AtomicLong state = this.state;
            long oldVal;
            do {
                oldVal = state.get();
                if ((oldVal & FLAG_CLOSED) != 0L) {
                    // nothing to do
                    return;
                }
                if (oldVal == 0L) {
                    // already indefinitely active
                    return;
                }
            } while (! state.compareAndSet(oldVal, 0L));
        }

        public void activateFor(final long time, final TimeUnit unit) {
            final long micros = unit.toMicros(time);
            // convert to stamp
            final long now = nowMicros();
            if (micros > TIME_MASK) {
                activate();
                return;
            }
            final long ts = now + micros;
            if (ts > TIME_MASK || ts < 0) {
                activate();
                return;
            }
            final AtomicLong state = this.state;
            long oldVal;
            do {
                oldVal = state.get();
                if ((oldVal & FLAG_CLOSED) != 0L) {
                    // nothing to do
                    return;
                }
                if (oldVal == ts) {
                    // unlikely but it costs us nothing to check
                    return;
                }
            } while (! state.compareAndSet(oldVal, ts));
        }

        ServiceURL getServiceURL() {
            return serviceURL;
        }

        boolean isOpenAndActive() {
            final long val = state.get();
            if ((val & (FLAG_CLOSED | FLAG_DEACTIVATED)) != 0L) {
                return false;
            }
            long stamp = val & TIME_MASK;
            return stamp == 0L || nowMicros() < stamp;
        }
    }
}
