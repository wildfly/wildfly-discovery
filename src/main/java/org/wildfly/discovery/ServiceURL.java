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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.wildfly.common.Assert;

/**
 * An <a href="http://tools.ietf.org/html/rfc2609">RFC 2609</a>-compliant service description URL.  This implementation
 * only deviates from the specification in that it does not support AppleTalk or IPX address schemes.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceURL extends ServiceDesignation {

    private static final long serialVersionUID = -5463002934752940723L;

    private final String abstractType;
    private final String abstractTypeAuthority;

    private final URI uri;
    private final String uriSchemeAuthority;

    private final Map<String, List<AttributeValue>> attributes;

    private transient int hashCode;
    private transient String toString;
    private transient URI toServiceURI;

    ServiceURL(Builder builder, Map<String, List<AttributeValue>> attributes) {
        this.abstractType = builder.abstractType;
        this.abstractTypeAuthority = builder.abstractType == null ? null : builder.abstractTypeAuthority;
        this.uri = Assert.checkNotNullParam("uri", builder.uri);
        this.uriSchemeAuthority = builder.uriSchemeAuthority;
        this.attributes = attributes;
    }

    /**
     * A builder for service URLs.
     */
    public static class Builder {
        private String abstractType;
        private String abstractTypeAuthority;
        private URI uri;
        private String uriSchemeAuthority;
        private List<Attr> attributes = new ArrayList<>();

        /**
         * Construct a new instance.
         */
        public Builder() {
        }

        /**
         * Get the abstract type.
         *
         * @return the abstract type, or {@code null} if it is not set
         */
        public String getAbstractType() {
            return abstractType;
        }

        /**
         * Set the abstract type.
         *
         * @param abstractType the abstract type
         */
        public void setAbstractType(final String abstractType) {
            this.abstractType = abstractType;
        }

        /**
         * Get the abstract type authority.
         *
         * @return the abstract type authority, or {@code null} if it is not set
         */
        public String getAbstractTypeAuthority() {
            return abstractTypeAuthority;
        }

        /**
         * Set the abstract authority.
         *
         * @param abstractTypeAuthority the abstract authority
         */
        public void setAbstractTypeAuthority(final String abstractTypeAuthority) {
            this.abstractTypeAuthority = abstractTypeAuthority;
        }

        /**
         * Get the concrete URI.
         *
         * @return the concrete URI
         */
        public URI getUri() {
            return uri;
        }

        /**
         * Set the concrete URI.
         *
         * @param uri the concrete URI (must not be {@code null})
         */
        public void setUri(final URI uri) {
            Assert.checkNotNullParam("uri", uri);
            if (uri.getFragment() != null) {
                throw new IllegalArgumentException("Service URIs may not have a fragment");
            }
            if (! uri.isAbsolute()) {
                throw new IllegalArgumentException("Service URIs must be absolute");
            }
            this.uri = uri;
        }

        /**
         * Get the URI scheme authority, if any.
         *
         * @return the URI scheme authority, or {@code null} if none is set
         */
        public String getUriSchemeAuthority() {
            return uriSchemeAuthority;
        }

        /**
         * Set the URI scheme authority.
         *
         * @param uriSchemeAuthority the URI scheme authority
         */
        public void setUriSchemeAuthority(final String uriSchemeAuthority) {
            this.uriSchemeAuthority = uriSchemeAuthority;
        }

        /**
         * Add an attribute.
         *
         * @param name the attribute name (must not be {@code null})
         * @param value the attribute value (must not be {@code null})
         */
        public void addAttribute(final String name, final AttributeValue value) {
            Assert.checkNotNullParam("name", name);
            Assert.checkNotNullParam("value", value);
            attributes.add(new Attr(name, value));
        }

        /**
         * Add a valueless attribute.
         *
         * @param name the attribute name (must not be {@code null})
         */
        public void addAttribute(final String name) {
            Assert.checkNotNullParam("name", name);
            attributes.add(new Attr(name, null));
        }

        /**
         * Construct the service URL.
         *
         * @return the service URL
         * @throws IllegalArgumentException if one or more builder property values is not acceptable
         */
        public ServiceURL create() {
            final HashMap<String, List<AttributeValue>> map = new HashMap<>();
            List<AttributeValue> list;
            for (Attr attr : attributes) {
                list = map.get(attr.name);
                if (list == null) {
                    map.put(attr.name, list = new ArrayList<AttributeValue>(attr.value == null ? 0 : 1));
                }
                if (attr.value != null) {
                    list.add(attr.value);
                }
            }
            return new ServiceURL(this, map);
        }

        static class Attr {
            final String name;
            final AttributeValue value;

            Attr(final String name, final AttributeValue value) {
                this.name = name;
                this.value = value;
            }
        }
    }

    /**
     * Determine whether this service URL satisfies the given filter specification.
     *
     * @param filterSpec the filter specification
     * @return {@code true} if this service satisfies the filter specification, {@code false} if it does not
     */
    public boolean satisfies(final FilterSpec filterSpec) {
        return filterSpec.matchesMulti(attributes);
    }

    /**
     * Determine if this service URL implies the other service URL.  This is true only when the two are equal.
     *
     * @param other the other service URL
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean implies(final ServiceURL other) {
        return equals(other);
    }

    /**
     * Determine if this service URL implies the other service designation.  This is true only when the other designation
     * is a service URL and the two are equal.
     *
     * @param other the other service designation
     * @return {@code true} if they are equal service URLs, {@code false} otherwise
     */
    public boolean implies(final ServiceDesignation other) {
        return other instanceof ServiceURL && implies((ServiceURL) other);
    }

    /**
     * Determine whether this service URL is equal to the given object.  This is true when the other object is a service
     * URL with the same abstract type and authority, the same concrete URI, and the same attributes.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final ServiceURL other) {
        return hashCode() == other.hashCode()
            && Objects.equals(abstractType, other.abstractType)
            && Objects.equals(abstractTypeAuthority, other.abstractTypeAuthority)
            && Objects.equals(uri, other.uri)
            && Objects.equals(uriSchemeAuthority, other.uriSchemeAuthority)
            && attributes.equals(other.attributes);
    }

    /**
     * Determine whether this service URL is equal to the given object.  This is true when the other object is a service
     * URL with the same abstract type and authority, the same concrete URI, and the same attributes.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final ServiceDesignation other) {
        return other instanceof ServiceURL && equals((ServiceURL) other);
    }

    /**
     * Determine whether this service URL is equal to the given object.  This is true when the other object is a service
     * URL with the same abstract type and authority, the same concrete URI, and the same attributes.
     *
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(final Object other) {
        return other instanceof ServiceURL && equals((ServiceURL) other);
    }

    /**
     * Get the hash code of this service URL.  Service URLs are suitable for use as hash table keys.
     *
     * @return the hash code
     */
    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            if (abstractType != null) {
                hashCode = abstractType.hashCode();
            }
            hashCode *= 17;
            if (abstractTypeAuthority != null) {
                hashCode += abstractTypeAuthority.hashCode();
            }
            hashCode *= 17;
            hashCode += uri.hashCode();
            hashCode *= 17;
            if (uriSchemeAuthority != null) {
                hashCode += uriSchemeAuthority.hashCode();
            }
            hashCode *= 17;
            hashCode += attributes.hashCode();
            if (hashCode == 0) {
                hashCode = -1;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    /**
     * Get the string representation of this service URL.
     *
     * @return the string representation
     */
    public String toString() {
        String toString = this.toString;
        if (toString == null) {
            StringBuilder b = new StringBuilder(40);
            b.append("service:");
            if (abstractType != null) {
                b.append(abstractType);
                if (abstractTypeAuthority != null) {
                    b.append('.').append(abstractTypeAuthority);
                }
                b.append(':');
            }
            b.append(uri.getScheme());
            if (uriSchemeAuthority != null) {
                b.append('.').append(uriSchemeAuthority);
            }
            b.append(':').append(uri.getRawSchemeSpecificPart());
            for (Map.Entry<String, List<AttributeValue>> entry : attributes.entrySet()) {
                b.append(';').append(entry.getKey());
                Iterator<AttributeValue> iterator = entry.getValue().iterator();
                if (iterator.hasNext()) {
                    b.append('=');
                    iterator.next().escapeTo(b);
                    while (iterator.hasNext()) {
                        b.append(',');
                        iterator.next().escapeTo(b);
                    }
                }
            }
            this.toString = toString = b.toString();
        }
        return toString;
    }

    /**
     * Convert this service URL into a URI whose contents are exactly equal to this object's.
     *
     * @return the URI (not {@code null})
     * @throws URISyntaxException if there was some syntactical problem in the URI being constructed
     */
    public URI toServiceURI() throws URISyntaxException {
        URI toServiceURI = this.toServiceURI;
        if (toServiceURI == null) {
            toServiceURI = this.toServiceURI = new URI(toString());
        }
        return toServiceURI;
    }

    /**
     * Get the concrete location URI of this service URL.
     *
     * @return the concrete location (not {@code null})
     */
    public URI getLocationURI() {
        return uri;
    }

    /**
     * Get the service type of this URL.
     *
     * @return the service type (not {@code null})
     */
    public ServiceType getServiceType() {
        return abstractType != null ? new ServiceType(abstractType, abstractTypeAuthority, uri.getScheme(), uriSchemeAuthority) : new ServiceType(uri.getScheme(), uriSchemeAuthority, null, null);
    }

    /**
     * Get the abstract type, if any.
     *
     * @return the abstract type, or {@code null} if no abstract type is set
     */
    public String getAbstractType() {
        return abstractType;
    }

    /**
     * Get the abstract type authority, if any.
     *
     * @return the abstract type authority, or {@code null} if no abstract type authority is set
     */
    public String getAbstractTypeAuthority() {
        return abstractTypeAuthority;
    }

    /**
     * Get the concrete URI scheme.
     *
     * @return the concrete URI scheme (not {@code null})
     */
    public String getUriScheme() {
        return uri.getScheme();
    }

    /**
     * Get the concrete URI scheme authority, if any.
     *
     * @return the concrete URI scheme authority, or {@code null} if none is set
     */
    public String getUriSchemeAuthority() {
        return uriSchemeAuthority;
    }

    /**
     * Get the user name of the concrete URI, if any.
     *
     * @return the user name of the concrete URI, or {@code null} if none is set
     */
    public String getUserName() {
        return uri.getUserInfo();
    }

    /**
     * Get the host name of the concrete URI.
     *
     * @return the host name of the concrete URI (not {@code null})
     */
    public String getHostName() {
        return uri.getHost();
    }

    /**
     * Get the port number of the concrete URI, if any.
     *
     * @return the port number of the concrete URI, or -1 if it is undefined
     */
    public int getPort() {
        return uri.getPort();
    }

    /**
     * Get the path name of the concrete URI, if any.
     *
     * @return the path name of the concrete URI, or {@code null} if none is set
     */
    public String getPath() {
        return uri.getPath();
    }
}
