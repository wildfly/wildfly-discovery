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

package org.wildfly.discovery.spi;

import org.wildfly.discovery.FilterSpec;
import org.wildfly.discovery.ServiceType;

/**
 * A discovery provider.  This interface is implemented by all discovery provider implementations.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface DiscoveryProvider {

    /**
     * Attempt to discover implementations of a service.  Matches must be written to the
     * {@link DiscoveryResult#addMatch(java.net.URI)} method on the {@code result} parameter as they are found.  The
     * {@link DiscoveryResult#complete()} method must be called when the discovery process is complete or has timed out
     * or failed for any other reason, otherwise discovery clients may hang indefinitely.
     * <p>
     * The discovery process should be asynchronous if possible.  In this case, this method should return as soon as
     * possible in order to unblock the calling thread quickly and allow other discovery mechanisms to proceed.  The
     * discovery process will not be considered complete until the result's completion method is called, even if such a
     * call comes from a different thread.
     *
     * @param serviceType the service type to match
     * @param filterSpec the service attribute filter expression, or {@code null} to return all matches
     * @param result the discovery result
     */
    DiscoveryRequest discover(ServiceType serviceType, FilterSpec filterSpec, DiscoveryResult result);

    /**
     * The empty discovery provider.
     */
    DiscoveryProvider EMPTY = new DiscoveryProvider() {
        public DiscoveryRequest discover(final ServiceType serviceType, final FilterSpec filterSpec, final DiscoveryResult result) {
            result.complete();
            return DiscoveryRequest.NULL;
        }
    };
}
