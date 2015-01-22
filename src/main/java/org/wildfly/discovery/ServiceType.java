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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceType extends ServiceDesignation {

    private static final long serialVersionUID = -6374415301360797195L;

    private final String abstractType;
    private final String abstractTypeAuthority;
    private final String urlScheme;
    private final String urlSchemeAuthority;

    private transient String toString;

    ServiceType(final String abstractType, final String abstractTypeAuthority, final String urlScheme, final String urlSchemeAuthority) {
        this.abstractType = abstractType;
        this.abstractTypeAuthority = abstractTypeAuthority;
        this.urlScheme = urlScheme;
        this.urlSchemeAuthority = urlSchemeAuthority;
    }

    public boolean implies(final ServiceDesignation other) {
        if (other instanceof ServiceType) {
            return implies((ServiceType) other);
        } else {
            assert other instanceof ServiceURL;
            return implies((ServiceURL) other);
        }
    }

    public boolean implies(final ServiceType serviceType) {
        return Objects.equals(abstractType, serviceType.abstractType)
            && Objects.equals(abstractTypeAuthority, serviceType.abstractTypeAuthority)
            && (urlScheme == null || urlScheme.equals(serviceType.urlScheme) && Objects.equals(urlSchemeAuthority, serviceType.urlSchemeAuthority));
    }

    public boolean implies(final ServiceURL serviceURL) {
        if (urlScheme == null) {
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
                && Objects.equals(urlScheme, serviceURL.getUriScheme())
                && Objects.equals(urlSchemeAuthority, serviceURL.getUriSchemeAuthority());
        }
    }

    public boolean equals(final ServiceType other) {
        return other != null
            && Objects.equals(abstractType, other.abstractType)
            && Objects.equals(abstractTypeAuthority, other.abstractTypeAuthority)
            && Objects.equals(urlScheme, other.urlScheme)
            && Objects.equals(urlSchemeAuthority, other.urlSchemeAuthority);
    }

    public boolean equals(final ServiceDesignation other) {
        return other instanceof ServiceType && equals((ServiceType) other);
    }

    public boolean equals(final Object other) {
        return other instanceof ServiceType && equals((ServiceType) other);
    }

    public int hashCode() {
        return getClass().hashCode() + 17 * (abstractType.hashCode() + 17 * abstractTypeAuthority.hashCode());
    }

    public String toString() {
        String toString = this.toString;
        if (toString == null) {
            final StringBuilder b = new StringBuilder(32);
            b.append("service:").append(abstractType);
            if (abstractTypeAuthority != null) {
                b.append('.').append(abstractTypeAuthority);
            }
            if (urlScheme != null) {
                b.append(':').append(urlScheme);
                if (urlSchemeAuthority != null) {
                    b.append('.').append(urlSchemeAuthority);
                }
            }
            toString = this.toString = b.toString();
        }
        return toString;
    }

    public final String getAbstractType() {
        return abstractType;
    }

    public final String getAbstractTypeAuthority() {
        return abstractTypeAuthority;
    }

    public String getUrlScheme() {
        return urlScheme;
    }

    public String getUrlSchemeAuthority() {
        return urlSchemeAuthority;
    }

    public static ServiceType of(final String abstractType, final String abstractTypeAuthority) {
        return new ServiceType(abstractType, abstractTypeAuthority, null, null);
    }
}
