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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    private transient Set<String> attributeNames;
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
        private Map<String, LinkedHashSet<AttributeValue>> attributes;

        /**
         * Construct a new instance.
         */
        public Builder() {
            attributes = new HashMap<>();
        }

        /**
         * Construct a new instance from an original template.
         *
         * @param original the original service URL (must not be {@code null})
         */
        public Builder(ServiceURL original) {
            abstractType = original.getAbstractType();
            abstractTypeAuthority = original.getAbstractTypeAuthority();
            uri = original.getLocationURI();
            uriSchemeAuthority = original.getUriSchemeAuthority();
            final Map<String, List<AttributeValue>> attributes = original.getAttributes();
            Map<String, LinkedHashSet<AttributeValue>> map = new HashMap<>(attributes.size());
            for (Map.Entry<String, List<AttributeValue>> entry : attributes.entrySet()) {
                map.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
            }
            this.attributes = map;
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
         * @return this builder
         */
        public Builder setAbstractType(final String abstractType) {
            this.abstractType = abstractType;
            return this;
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
         * @return this builder
         */
        public Builder setAbstractTypeAuthority(final String abstractTypeAuthority) {
            this.abstractTypeAuthority = abstractTypeAuthority;
            return this;
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
         * @return this builder
         */
        public Builder setUri(final URI uri) {
            Assert.checkNotNullParam("uri", uri);
            final String fragment = uri.getFragment();
            if (fragment != null && ! fragment.isEmpty()) {
                throw new IllegalArgumentException("Service URI " + uri + " may not have a fragment");
            }
            if (! uri.isAbsolute()) {
                throw new IllegalArgumentException("Service URI " + uri + " must be absolute");
            }
            final String query = uri.getQuery();
            // sanitized URI
            try {
                this.uri = uri.isOpaque() ?
                           new URI(uri.getScheme(), uri.getSchemeSpecificPart(), null) :
                           new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), query != null && query.isEmpty() ? null : query, null);
            } catch (URISyntaxException e) {
                // should be impossible as the original URI was valid
                throw new IllegalStateException(e);
            }
            return this;
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
         * @return this builder
         */
        public Builder setUriSchemeAuthority(final String uriSchemeAuthority) {
            this.uriSchemeAuthority = uriSchemeAuthority;
            return this;
        }

        /**
         * Add an attribute.
         *
         * @param name the attribute name (must not be {@code null})
         * @param value the attribute value (must not be {@code null})
         * @return this builder
         */
        public Builder addAttribute(final String name, final AttributeValue value) {
            Assert.checkNotNullParam("name", name);
            Assert.checkNotNullParam("value", value);
            attributes.computeIfAbsent(name, n -> new LinkedHashSet<>()).add(value);
            return this;
        }

        /**
         * Add a valueless attribute.
         *
         * @param name the attribute name (must not be {@code null})
         * @return this builder
         */
        public Builder addAttribute(final String name) {
            Assert.checkNotNullParam("name", name);
            attributes.computeIfAbsent(name, n -> new LinkedHashSet<>()).add(null);
            return this;
        }

        /**
         * Remove all values of the given attribute name.
         *
         * @param name the attribute name (must not be {@code null})
         * @return the removed attribute values, or {@code null} if the attribute was not present in the builder
         */
        public List<AttributeValue> removeAttribute(final String name) {
            Assert.checkNotNullParam("name", name);
            final LinkedHashSet<AttributeValue> removed = attributes.remove(name);
            if (removed == null) {
                return Collections.emptyList();
            }
            final Iterator<AttributeValue> iterator = removed.iterator();
            if (! iterator.hasNext()) {
                return Collections.emptyList();
            }
            final AttributeValue first = iterator.next();
            if (! iterator.hasNext()) {
                return Collections.singletonList(first);
            }
            final ArrayList<AttributeValue> list = new ArrayList<>(removed.size());
            list.add(first);
            do {
                list.add(iterator.next());
            } while (iterator.hasNext());
            return list;
        }

        /**
         * Remove the given value of the given attribute name.
         *
         * @param name the attribute name (must not be {@code null})
         * @param value the value to remove (must not be {@code null})
         * @return {@code true} if the value was present, or {@code false} otherwise
         */
        public boolean removeAttributeValue(final String name, final AttributeValue value) {
            Assert.checkNotNullParam("name", name);
            Assert.checkNotNullParam("value", value);
            final LinkedHashSet<AttributeValue> set = attributes.get(name);
            if (set == null) {
                return false;
            }
            if (set.remove(value)) {
                if (set.isEmpty()) {
                    attributes.remove(name, set);
                }
                return true;
            }
            return false;
        }

        /**
         * Construct the service URL.
         *
         * @return the service URL
         * @throws IllegalArgumentException if one or more builder property values is not acceptable
         */
        public ServiceURL create() {
            final HashMap<String, List<AttributeValue>> map = new HashMap<>(attributes.size());
            for (Map.Entry<String, LinkedHashSet<AttributeValue>> entry : attributes.entrySet()) {
                map.put(entry.getKey(), unmodList(entry.getValue()));
            }
            return new ServiceURL(this, map);
        }

        static <T> List<T> unmodList(Collection<T> original) {
            if (original.isEmpty()) {
                return Collections.emptyList();
            } else if (original.size() == 1) {
                return Collections.singletonList(original.iterator().next());
            } else {
                return Collections.unmodifiableList(new ArrayList<>(original));
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
        return filterSpec == null || filterSpec.matchesMulti(attributes);
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
                    b.append(iterator.next());
                    while (iterator.hasNext()) {
                        b.append(',');
                        b.append(iterator.next());
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

    /**
     * Get the first attribute value for the given name.
     *
     * @param name the attribute name (must not be {@code null})
     * @return the first attribute value for the given name, or {@code null} if no such attribute exists
     */
    public AttributeValue getFirstAttributeValue(String name) {
        Assert.checkNotNullParam("name", name);
        final List<AttributeValue> list = attributes.getOrDefault(name, Collections.emptyList());
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Get the first attribute value for the given name.
     *
     * @param name the attribute name (must not be {@code null})
     * @param defaultValue the value to return if no such attribute exists
     * @return the first attribute value for the given name, or {@code defaultValue} if no such attribute exists
     */
    public AttributeValue getFirstAttributeValue(String name, AttributeValue defaultValue) {
        Assert.checkNotNullParam("name", name);
        final List<AttributeValue> list = attributes.getOrDefault(name, Collections.emptyList());
        return list.isEmpty() ? defaultValue : list.get(0);
    }

    /**
     * Get the last attribute value for the given name.
     *
     * @param name the attribute name (must not be {@code null})
     * @return the last attribute value for the given name, or {@code null} if no such attribute exists
     */
    public AttributeValue getLastAttributeValue(String name) {
        Assert.checkNotNullParam("name", name);
        final List<AttributeValue> list = attributes.getOrDefault(name, Collections.emptyList());
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    /**
     * Get the last attribute value for the given name.
     *
     * @param name the attribute name (must not be {@code null})
     * @param defaultValue the value to return if no such attribute exists
     * @return the last attribute value for the given name, or {@code null} if no such attribute exists
     */
    public AttributeValue getLastAttributeValue(String name, AttributeValue defaultValue) {
        Assert.checkNotNullParam("name", name);
        final List<AttributeValue> list = attributes.getOrDefault(name, Collections.emptyList());
        return list.isEmpty() ? defaultValue : list.get(list.size() - 1);
    }

    /**
     * Get the values of the attribute with the given name.  If no such attribute exists, an empty list is returned.
     *
     * @param name the attribute name (must not be {@code null})
     * @return the values of the attribute with the given name (not {@code null})
     */
    public List<AttributeValue> getAttributeValues(String name) {
        Assert.checkNotNullParam("name", name);
        return attributes.getOrDefault(name, Collections.emptyList());
    }

    /**
     * Get the attribute names.  If no attributes exist, an empty set is returned.
     * @return an unmodifiable set of attribute names (not {@code null})
     */
    public Set<String> getAttributeNames() {
        Set<String> attributeNames = this.attributeNames;
        if (attributeNames == null) {
            attributeNames = this.attributeNames = Collections.unmodifiableSet(this.attributes.keySet());
        }
        return attributeNames;
    }

    Map<String, List<AttributeValue>> getAttributes() {
        return attributes;
    }
}
