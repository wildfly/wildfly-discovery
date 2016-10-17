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

import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.common.Assert;
import org.wildfly.discovery.FilterSpec;
import org.wildfly.discovery.ServiceType;
import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryRequest;
import org.wildfly.discovery.spi.DiscoveryResult;

/**
 * A discovery provider which can be mutated at run time to delegate to a new provider.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class MutableDiscoveryProvider implements DiscoveryProvider {
    private final AtomicReference<DiscoveryProvider> delegateRef;

    /**
     * Construct a new instance.
     *
     * @param initialProvider the initial provider to use (must not be {@code null})
     */
    public MutableDiscoveryProvider(DiscoveryProvider initialProvider) {
        Assert.checkNotNullParam("initialProvider", initialProvider);
        delegateRef = new AtomicReference<>(initialProvider);
    }

    /**
     * Construct a new instance using an empty provider.
     */
    public MutableDiscoveryProvider() {
        this(EMPTY);
    }

    /**
     * Set the discovery provider instance.
     *
     * @param delegateProvider the discovery provider instance (must not be {@code null})
     */
    public void setDiscoveryProvider(DiscoveryProvider delegateProvider) {
        Assert.checkNotNullParam("delegateProvider", delegateProvider);
        delegateRef.set(delegateProvider);
    }

    public DiscoveryRequest discover(final ServiceType serviceType, final FilterSpec filterSpec, final DiscoveryResult result) {
        return delegateRef.get().discover(serviceType, filterSpec, result);
    }
}
