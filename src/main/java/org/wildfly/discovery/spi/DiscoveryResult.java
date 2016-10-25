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

import java.net.URI;

import org.wildfly.discovery.ServiceURL;

/**
 * The discovery result.  Instances of this class must be safe for use from multiple threads concurrently.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface DiscoveryResult {

    /**
     * Indicate that discovery is complete.  Once this method is called, all additional calls to {@link #addMatch(ServiceURL)}
     * will be ignored.
     */
    void complete();

    /**
     * Indicate that a matching URI was discovered.  A service URL with no abstract type or type authorities and no
     * attributes is created for the service URI.
     *
     * @param uri the discovered URI
     */
    default void addMatch(URI uri) {
        addMatch(new ServiceURL.Builder().setUri(uri).create());
    }

    /**
     * Indicate that a matching service URL was discovered.
     *
     * @param serviceURL the discovered service URL
     */
    void addMatch(ServiceURL serviceURL);
}
