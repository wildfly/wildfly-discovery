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

package org.wildfly.discovery;

import org.wildfly.common.Assert;
import org.wildfly.common.context.ContextManager;
import org.wildfly.common.context.Contextual;
import org.wildfly.discovery.spi.RegistryProvider;

/**
 * A configured service registry.  A service registry is able to advertise services to local or remote clients.
 * <p>
 * Some providers only support certain types of services and will ignore others.  Some providers cannot support
 * repeated attribute values, or may be restricted as to which attribute names are recognized, or may not support
 * attributes at all.  Such providers generally must evaluate a service URL and determine if it can correctly
 * advertise it, ignoring the registration if the provider cannot.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceRegistry implements Contextual<ServiceRegistry> {

    private static final ContextManager<ServiceRegistry> CONTEXT_MANAGER = new ContextManager<ServiceRegistry>(ServiceRegistry.class, "org.wildfly.discovery.registration");

    private final RegistryProvider registryProvider;

    private ServiceRegistry(final RegistryProvider registryProvider) {
        this.registryProvider = registryProvider;
    }

    /**
     * Get the instance context manager.  Delegates to {@link #getContextManager()}.
     *
     * @return the instance context manager (not {@code null})
     */
    public ContextManager<ServiceRegistry> getInstanceContextManager() {
        return getContextManager();
    }

    /**
     * Get the context manager.
     *
     * @return the context manager (not {@code null})
     */
    public static ContextManager<ServiceRegistry> getContextManager() {
        return CONTEXT_MANAGER;
    }

    /**
     * Create a new service registry instance.
     *
     * @param registryProvider the backing registry provider (must not be {@code null})
     * @return the new service registry (not {@code null})
     */
    public static ServiceRegistry create(final RegistryProvider registryProvider) {
        Assert.checkNotNullParam("registryProvider", registryProvider);
        return new ServiceRegistry(registryProvider);
    }

    /**
     * Register a service URL.  Any valid service URL may be provided, and the registration will last until the handle
     * is closed.  If the service URL is not supported, the {@linkplain ServiceRegistration#EMPTY empty registration handle}
     * is returned.
     *
     * @param serviceURL the service to register (must not be {@code null})
     * @return the registration handle (not {@code null})
     */
    public ServiceRegistration registerService(ServiceURL serviceURL) {
        Assert.checkNotNullParam("serviceURL", serviceURL);
        return registryProvider.registerService(serviceURL);
    }

    /**
     * Register a group of service URLs to be controlled with a single handle.  Any valid service URL may be provided,
     * and the registrations will last until the handle is closed.  If the service URL is not supported, the
     * {@linkplain ServiceRegistration#EMPTY empty registration handle} is returned.
     *
     * @param serviceURLs the services to register (must not be {@code null} or contain {@code null} elements)
     * @return the registration handle (not {@code null})
     */
    public ServiceRegistration registerServices(ServiceURL... serviceURLs) {
        Assert.checkNotNullParam("serviceURLs", serviceURLs);
        final int length = serviceURLs.length;
        for (int i = 0; i < length; i++) {
            Assert.checkNotNullArrayParam("serviceURLs", i, serviceURLs[i]);
        }
        return registryProvider.registerServices(serviceURLs);
    }
}
