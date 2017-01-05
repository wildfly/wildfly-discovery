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

import java.time.Instant;

import org.wildfly.common.Assert;

/**
 * An aggregate service registration.
 *
 * @see ServiceRegistration#aggregate(ServiceRegistration...)
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class AggregateServiceRegistration implements ServiceRegistration {
    private final ServiceRegistration[] registrations;

    /**
     * Construct a new instance.
     *
     * @param registrations the registrations (must not be {@code null})
     */
    protected AggregateServiceRegistration(final ServiceRegistration... registrations) {
        Assert.checkNotNullParam("registrations", registrations);
        this.registrations = registrations;
    }

    public void close() {
        for (ServiceRegistration registration : registrations) {
            if (registration != null) registration.close();
        }
    }

    public void deactivate() {
        for (ServiceRegistration registration : registrations) {
            if (registration != null) registration.deactivate();
        }
    }

    public void activate() {
        for (ServiceRegistration registration : registrations) {
            if (registration != null) registration.activate();
        }
    }

    public void hintDeactivateAt(final Instant instant) {
        for (ServiceRegistration registration : registrations) {
            if (registration != null) registration.hintDeactivateAt(instant);
        }
    }
}
