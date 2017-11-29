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

package org.wildfly.discovery;

import java.util.Objects;

import org.wildfly.common.Assert;

/**
 * An abstract service type.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceType extends ServiceDesignation {

    private static final long serialVersionUID = -6374415301360797195L;

    private final String abstractType;
    private final String abstractTypeAuthority;
    private final String uriScheme;
    private final String uriSchemeAuthority;

    private transient String toString;

    ServiceType(final String abstractType, final String abstractTypeAuthority, final String uriScheme, final String uriSchemeAuthority) {
        this.abstractType = abstractType;
        this.abstractTypeAuthority = abstractTypeAuthority;
        this.uriScheme = uriScheme;
        this.uriSchemeAuthority = uriSchemeAuthority;
    }

    public boolean implies(final ServiceDesignation other) {
        Assert.checkNotNullParam("other", other);
        if (other instanceof ServiceType) {
            return implies((ServiceType) other);
        } else {
            assert other instanceof ServiceURL;
            return implies((ServiceURL) other);
        }
    }

    /**
     * Determine if this service type implies the other service type.  This is the case for any other service type which
     * has the same abstract type and abstract type authority, and the same URL scheme and URL scheme authority (if
     * one is specified) as this service type.
     *
     * @param serviceType the other service type (must not be {@code null})
     * @return {@code true} if the other service type is implied by this type, {@code false} otherwise
     */
    public boolean implies(final ServiceType serviceType) {
        Assert.checkNotNullParam("serviceType", serviceType);
        return Objects.equals(abstractType, serviceType.abstractType)
            && Objects.equals(abstractTypeAuthority, serviceType.abstractTypeAuthority)
            && (uriScheme == null || uriScheme.equals(serviceType.uriScheme) && Objects.equals(uriSchemeAuthority, serviceType.uriSchemeAuthority));
    }

    /**
     * Determine if this service type implies the given service URL.  This is the case for any service URL which either
     * has no abstract type and whose concrete type matches this service type's abstract type, or has an abstract type
     * that is equal to this service type's abstract type and has a concrete type equal to this service type's concrete
     * type.
     *
     * @param serviceURL the service URL (must not be {@code null})
     * @return {@code true} if the service URL is implied by this type, {@code false} otherwise
     */
    public boolean implies(final ServiceURL serviceURL) {
        if (uriScheme == null) {
            if (serviceURL.getAbstractType() == null) {
                return Objects.equals(abstractType, serviceURL.getUriScheme())
                    && Objects.equals(abstractTypeAuthority, serviceURL.getUriSchemeAuthority());
            } else {
                return Objects.equals(abstractType, serviceURL.getAbstractType())
                    && Objects.equals(abstractTypeAuthority, serviceURL.getAbstractTypeAuthority());
            }
        } else {
            return Objects.equals(abstractType, serviceURL.getAbstractType())
                && Objects.equals(abstractTypeAuthority, serviceURL.getAbstractTypeAuthority())
                && Objects.equals(uriScheme, serviceURL.getUriScheme())
                && Objects.equals(uriSchemeAuthority, serviceURL.getUriSchemeAuthority());
        }
    }

    /**
     * Determine if this service type is equal to another.
     *
     * @param other the other service type
     * @return {@code true} if the service types are equal, {@code false} otherwise
     */
    public boolean equals(final ServiceType other) {
        return other != null
            && Objects.equals(abstractType, other.abstractType)
            && Objects.equals(abstractTypeAuthority, other.abstractTypeAuthority)
            && Objects.equals(uriScheme, other.uriScheme)
            && Objects.equals(uriSchemeAuthority, other.uriSchemeAuthority);
    }

    /**
     * Determine if this service type is equal to another.
     *
     * @param other the other service type
     * @return {@code true} if the service types are equal, {@code false} otherwise
     */
    public boolean equals(final ServiceDesignation other) {
        return other instanceof ServiceType && equals((ServiceType) other);
    }

    /**
     * Determine if this service type is equal to another.
     *
     * @param other the other service type
     * @return {@code true} if the service types are equal, {@code false} otherwise
     */
    public boolean equals(final Object other) {
        return other instanceof ServiceType && equals((ServiceType) other);
    }

    /**
     * Get the hash code for this service type.  Service types are immutable and are suitable for use as hash keys.
     *
     * @return the hash code for this service type
     */
    public int hashCode() {
        return getClass().hashCode() + 17 * (abstractType.hashCode() + 17 * abstractTypeAuthority.hashCode());
    }

    /**
     * Get the string representation of this service type.
     *
     * @return the string representation of this service type (not {@code null})
     */
    public String toString() {
        String toString = this.toString;
        if (toString == null) {
            toString = this.toString = toString(new StringBuilder(32)).toString();
        }
        return toString;
    }

    StringBuilder toString(StringBuilder b) {
        b.append("service:").append(abstractType);
        if (abstractTypeAuthority != null) {
            b.append('.').append(abstractTypeAuthority);
        }
        if (uriScheme != null) {
            b.append(':').append(uriScheme);
            if (uriSchemeAuthority != null) {
                b.append('.').append(uriSchemeAuthority);
            }
        }
        return b;
    }

    /**
     * Get the abstract type of this service type.  If this service type has no URL scheme, then the abstract type
     * is also the concrete type.
     *
     * @return the abstract type (not {@code null})
     */
    public final String getAbstractType() {
        return abstractType;
    }

    /**
     * Get the abstract type authority of this service type.  Abstract types with no authority will return {@code null}
     * for this value.
     *
     * @return the abstract type authority of this service type (may be {@code null})
     */
    public final String getAbstractTypeAuthority() {
        return abstractTypeAuthority;
    }

    /**
     * Get the concrete type name of this service type, if any.
     *
     * @return the concrete service type (may be {@code null})
     */
    public String getUriScheme() {
        return uriScheme;
    }

    /**
     * Get the concrete type authority name of this service type, if any.
     *
     * @return the concrete type authority name (may be {@code null})
     */
    public String getUriSchemeAuthority() {
        return uriSchemeAuthority;
    }

    /**
     * Get a service type with the given abstract type and optional abstract type authority.
     *
     * @param abstractType the abstract service type (must not be {@code null})
     * @param abstractTypeAuthority the abstract type authority (may be {@code null})
     * @return the service type (not {@code null})
     */
    public static ServiceType of(final String abstractType, final String abstractTypeAuthority) {
        Assert.checkNotNullParam("abstractType", abstractType);
        return new ServiceType(abstractType, abstractTypeAuthority, null, null);
    }

    /**
     * Get a service type with the given abstract and concrete type and optional abstract and/or concrete type authority.
     *
     * @param abstractType the abstract service type (must not be {@code null})
     * @param abstractTypeAuthority the abstract type authority (may be {@code null})
     * @param concreteType the concrete service type (must not be {@code null})
     * @param concreteTypeAuthority the concrete type authority (may be {@code null})
     * @return the service type (not {@code null})
     */
    public static ServiceType of(final String abstractType, final String abstractTypeAuthority, final String concreteType, final String concreteTypeAuthority) {
        Assert.checkNotNullParam("abstractType", abstractType);
        Assert.checkNotNullParam("concreteType", concreteType);
        return new ServiceType(abstractType, abstractTypeAuthority, concreteType, concreteTypeAuthority);
    }
}
