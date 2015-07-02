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
        this.uri = builder.uri;
        this.uriSchemeAuthority = builder.uriSchemeAuthority;
        this.attributes = attributes;
    }

    public static class Builder {
        private String abstractType;
        private String abstractTypeAuthority;
        private URI uri;
        private String uriSchemeAuthority;
        private List<Attr> attributes = new ArrayList<>();

        public String getAbstractType() {
            return abstractType;
        }

        public void setAbstractType(final String abstractType) {
            this.abstractType = abstractType;
        }

        public String getAbstractTypeAuthority() {
            return abstractTypeAuthority;
        }

        public void setAbstractTypeAuthority(final String abstractTypeAuthority) {
            this.abstractTypeAuthority = abstractTypeAuthority;
        }

        public URI getUri() {
            return uri;
        }

        public void setUri(final URI uri) {
            if (uri.getFragment() != null) {
                throw new IllegalArgumentException("Service URIs may not have a fragment");
            }
            if (! uri.isAbsolute()) {
                throw new IllegalArgumentException("Service URIs must be absolute");
            }
            this.uri = uri;
        }

        public String getUriSchemeAuthority() {
            return uriSchemeAuthority;
        }

        public void setUriSchemeAuthority(final String uriSchemeAuthority) {
            this.uriSchemeAuthority = uriSchemeAuthority;
        }

        public void addAttribute(final String name, final AttributeValue value) {
            attributes.add(new Attr(name, value));
        }

        public void addAttribute(final String name) {
            attributes.add(new Attr(name, null));
        }

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

    public boolean satisfies(final FilterSpec filterSpec) {
        return filterSpec.matchesMulti(attributes);
    }

    public boolean implies(final ServiceURL other) {
        return equals(other);
    }

    public boolean implies(final ServiceDesignation other) {
        return other instanceof ServiceURL && implies((ServiceURL) other);
    }

    public boolean equals(final ServiceURL other) {
        return hashCode() == other.hashCode()
            && Objects.equals(abstractType, other.abstractType)
            && Objects.equals(abstractTypeAuthority, other.abstractTypeAuthority)
            && Objects.equals(uri, other.uri)
            && Objects.equals(uriSchemeAuthority, other.uriSchemeAuthority)
            && attributes.equals(other.attributes);
    }

    public boolean equals(final ServiceDesignation other) {
        return other instanceof ServiceURL && equals((ServiceURL) other);
    }

    public boolean equals(final Object other) {
        return other instanceof ServiceURL && equals((ServiceURL) other);
    }

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

    public URI toServiceURI() throws URISyntaxException {
        URI toServiceURI = this.toServiceURI;
        if (toServiceURI == null) {
            toServiceURI = this.toServiceURI = new URI(toString());
        }
        return toServiceURI;
    }

    public URI getLocationURI() throws URISyntaxException {
        return uri;
    }

    public ServiceType getServiceType() {
        return abstractType != null ? new ServiceType(abstractType, abstractTypeAuthority, uri.getScheme(), uriSchemeAuthority) : new ServiceType(uri.getScheme(), uriSchemeAuthority, null, null);
    }

    public String getAbstractType() {
        return abstractType;
    }

    public String getAbstractTypeAuthority() {
        return abstractTypeAuthority;
    }

    public String getUriScheme() {
        return uri.getScheme();
    }

    public String getUriSchemeAuthority() {
        return uriSchemeAuthority;
    }

    public String getUserName() {
        return uri.getUserInfo();
    }

    public String getHostName() {
        return uri.getHost();
    }

    public int getPort() {
        return uri.getPort();
    }

    public String getPath() {
        return uri.getPath();
    }
}
