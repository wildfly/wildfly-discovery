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

import org.wildfly.common.Assert;
import org.wildfly.discovery.ServiceRegistration;
import org.wildfly.discovery.ServiceURL;
import org.wildfly.discovery.spi.RegistryProvider;

/**
 * A registry provider which aggregates multiple other providers together.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AggregateRegistryProvider implements RegistryProvider {
    private final RegistryProvider[] delegates;

    /**
     * Construct a new instance.
     *
     * @param delegates the array of delegates (must not be {@code null})
     */
    public AggregateRegistryProvider(final RegistryProvider[] delegates) {
        Assert.checkNotNullParam("delegates", delegates);
        this.delegates = delegates;
    }

    public ServiceRegistration registerService(final ServiceURL serviceURL) {
        Assert.checkNotNullParam("serviceURL", serviceURL);
        final ServiceRegistration[] array = new ServiceRegistration[delegates.length];
        final RegistryProvider[] delegates = this.delegates;
        for (int i = 0, delegatesLength = delegates.length; i < delegatesLength; i++) {
            array[i] = delegates[i].registerService(serviceURL);
        }
        return ServiceRegistration.aggregate(array);
    }

    public ServiceRegistration registerServices(final ServiceURL... serviceURLs) {
        Assert.checkNotNullParam("serviceURLs", serviceURLs);
        final ServiceRegistration[] array = new ServiceRegistration[delegates.length];
        final RegistryProvider[] delegates = this.delegates;
        for (int i = 0, delegatesLength = delegates.length; i < delegatesLength; i++) {
            array[i] = delegates[i].registerServices(serviceURLs);
        }
        return ServiceRegistration.aggregate(array);
    }
}
