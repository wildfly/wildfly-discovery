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

import java.util.Collection;
import java.util.Map;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SubstringFilterSpec extends FilterSpec {

    private final String attribute;
    private final String initialPart;
    private final String finalPart;
    private transient int hashCode;

    SubstringFilterSpec(final String attribute, final String initialPart, final String finalPart) {
        this.attribute = attribute;
        this.initialPart = initialPart;
        this.finalPart = finalPart;
    }

    public boolean matchesSimple(final Map<String, AttributeValue> attributes) {
        final String value = attributes.get(attribute).toString();
        return value.startsWith(initialPart) && value.endsWith(finalPart);
    }

    public boolean matchesMulti(final Map<String, ? extends Collection<AttributeValue>> attributes) {
        final Collection<AttributeValue> collection = attributes.get(attribute);
        if (collection != null) for (AttributeValue matchValue : collection) {
            final String value = matchValue.toString();
            if (value.startsWith(initialPart) && value.endsWith(finalPart)) {
                return true;
            }
        }
        return false;
    }

    public boolean mayMatch(final Collection<String> attributeNames) {
        return attributeNames.contains(attribute);
    }

    public boolean mayNotMatch(final Collection<String> attributeNames) {
        return true;
    }

    public <P, R, E extends Exception> R accept(Visitor<P, R, E> visitor, P parameter) throws E {
        return visitor.handle(this, parameter);
    }

    /**
     * Get the attribute to compare against.
     *
     * @return the attribute to compare against
     */
    public String getAttribute() {
        return attribute;
    }

    /**
     * Get the initial part which must match.
     *
     * @return the initial part which must match
     */
    public String getInitialPart() {
        return initialPart;
    }

    /**
     * Get the final part which must match.
     *
     * @return the final part which must match.
     */
    public String getFinalPart() {
        return finalPart;
    }

    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = (attribute.hashCode() * 19 + initialPart.hashCode()) * 19 + finalPart.hashCode();
            if (hashCode == 0) hashCode = 1 << 30;
            return this.hashCode = hashCode;
        }
        return hashCode;
    }

    public boolean equals(final FilterSpec other) {
        return other instanceof SubstringFilterSpec && equals((SubstringFilterSpec) other);
    }

    public boolean equals(final SubstringFilterSpec other) {
        return this == other || other != null && attribute.equals(other.attribute) && initialPart.equals(other.initialPart) && finalPart.equals(other.finalPart);
    }

    void toString(final StringBuilder builder) {
        builder.append('(');
        FilterSpec.escapeTo(attribute, builder);
        builder.append('=');
        FilterSpec.escapeTo(initialPart, builder);
        builder.append('*');
        FilterSpec.escapeTo(finalPart, builder);
        builder.append(')');
    }
}
