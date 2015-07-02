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

package org.wildfly.discovery.impl;

import java.net.URISyntaxException;
import java.util.List;

import org.wildfly.discovery.FilterSpec;
import org.wildfly.discovery.ServiceType;
import org.wildfly.discovery.ServiceURL;
import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryRequest;
import org.wildfly.discovery.spi.DiscoveryResult;

/**
 * A discovery provider using a static configuration.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class StaticDiscoveryProvider implements DiscoveryProvider {
    private final List<ServiceURL> services;

    /**
     * Construct a new instance.
     *
     * @param services the list of services to advertise
     */
    public StaticDiscoveryProvider(final List<ServiceURL> services) {
        this.services = services;
    }

    public DiscoveryRequest discover(final ServiceType serviceType, final FilterSpec filterSpec, final DiscoveryResult result) {
        try {
            for (ServiceURL service : services) {
                if (serviceType.implies(service) && (filterSpec == null || service.satisfies(filterSpec))) {
                    try {
                        result.addMatch(service.getLocationURI());
                    } catch (URISyntaxException ignored) {
                        // ignored
                    }
                }
            }
            return DiscoveryRequest.NULL;
        } finally {
            result.complete();
        }
    }
}
