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

package org.wildfly.discovery;

import java.io.Serializable;

import org.wildfly.common.Assert;

/**
 * An abstract location description for a service, consisting of a service type and a filter specification.
 */
public final class ServiceDescription implements Serializable {
    private static final long serialVersionUID = -3131354380283645867L;

    private final ServiceType serviceType;
    private final FilterSpec filterSpec;
    private transient int hashCode;

    /**
     * Construct a new instance.
     *
     * @param serviceType the service type (must not be {@code null})
     * @param filterSpec the filter specification (must not be {@code null})
     */
    public ServiceDescription(final ServiceType serviceType, final FilterSpec filterSpec) {
        Assert.checkNotNullParam("serviceType", serviceType);
        Assert.checkNotNullParam("filterSpec", filterSpec);
        this.serviceType = serviceType;
        this.filterSpec = filterSpec;
    }

    /**
     * Get the service type.
     *
     * @return the service type (not {@code null})
     */
    public ServiceType getServiceType() {
        return serviceType;
    }

    /**
     * Get the filter specification.
     *
     * @return the filter specification (not {@code null})
     */
    public FilterSpec getFilterSpec() {
        return filterSpec;
    }

    public boolean equals(final Object obj) {
        return obj instanceof ServiceDescription && equals((ServiceDescription) obj);
    }

    public boolean equals(final ServiceDescription other) {
        return other == this || other != null && serviceType.equals(other.serviceType) && filterSpec.equals(other.filterSpec);
    }

    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = serviceType.hashCode() * 19 + filterSpec.hashCode();
            if (hashCode == 0) hashCode = 1;
            return this.hashCode = hashCode;
        } else {
            return hashCode;
        }
    }

    public String toString() {
        StringBuilder b = new StringBuilder(48);
        serviceType.toString(b);
        b.append('(');
        filterSpec.toString(b);
        b.append(')');
        return b.toString();
    }
}
