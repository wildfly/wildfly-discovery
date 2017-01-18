/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * An external discovery configurator.  Instances of this class are found by way of {@link ServiceLoader} from the
 * class loader of this library.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ExternalDiscoveryConfigurator {
    /**
     * Configure extra discovery and registry providers and supply them to the given consumers.  The provider values
     * should not be {@code null}.
     *
     * @param discoveryProviderConsumer the discovery provider consumer (not {@code null})
     * @param registryProviderConsumer the registry provider consumer (not {@code null})
     */
    void configure(Consumer<DiscoveryProvider> discoveryProviderConsumer, Consumer<RegistryProvider> registryProviderConsumer);
}
