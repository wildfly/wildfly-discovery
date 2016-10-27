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

package org.wildfly.discovery.spi;

import org.wildfly.discovery.ServiceRegistration;
import org.wildfly.discovery.ServiceURL;

/**
 * A provider for service registration.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface RegistryProvider {
    /**
     * Register a service with this provider.  Service URLs which are unsupported must be ignored; in this case,
     * the {@linkplain ServiceRegistration#EMPTY empty registration handle} must be returned.
     *
     * @param serviceURL the service to register (not {@code null})
     * @return the service registration handle (must not be {@code null})
     */
    ServiceRegistration registerService(ServiceURL serviceURL);

    /**
     * Register multiple services with this provider at once.  Service URLs which are unsupported must be ignored.  If
     * none of the service URLs are supported, then the {@linkplain ServiceRegistration#EMPTY empty registration handle}
     * must be returned.
     * <p>
     * The default implementation calls {@link #registerService(ServiceURL)} for each service in the array and
     * returns an aggregated handle.
     *
     * @param serviceURLs the services to register (not {@code null}, will not contain {@code null} elements)
     * @return the service registration handle (must not be {@code null})
     */
    default ServiceRegistration registerServices(ServiceURL... serviceURLs) {
        final ServiceRegistration[] registrations = new ServiceRegistration[serviceURLs.length];
        for (int i = 0, serviceURLsLength = serviceURLs.length; i < serviceURLsLength; i++) {
            registrations[i] = registerService(serviceURLs[i]);
        }
        return ServiceRegistration.aggregate(registrations);
    }

    /**
     * A registry provider which ignores all registrations.
     */
    RegistryProvider EMPTY = serviceURL -> ServiceRegistration.EMPTY;
}
