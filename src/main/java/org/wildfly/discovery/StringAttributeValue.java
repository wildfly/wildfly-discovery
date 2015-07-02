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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class StringAttributeValue extends AttributeValue implements Comparable<StringAttributeValue> {

    private static final long serialVersionUID = -1404364375885406297L;

    private final String value;

    StringAttributeValue(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(final Object other) {
        return other instanceof StringAttributeValue && equals((StringAttributeValue) other);
    }

    public boolean equals(final AttributeValue other) {
        return other instanceof StringAttributeValue && equals((StringAttributeValue) other);
    }

    public boolean equals(final StringAttributeValue other) {
        return other != null && value.equals(other.value);
    }

    public String toString() {
        return value;
    }

    public int compareTo(final AttributeValue other) {
        if (! (other instanceof StringAttributeValue)) throw new IllegalArgumentException("Not comparable");
        return compareTo((StringAttributeValue) other);
    }

    void escapeTo(final StringBuilder builder) {
        FilterSpec.escapeTo(value, builder);
    }

    public int compareTo(final StringAttributeValue other) {
        return value.compareTo(other.value);
    }
}
