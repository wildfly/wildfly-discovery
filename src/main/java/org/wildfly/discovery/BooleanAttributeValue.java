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
public final class BooleanAttributeValue extends AttributeValue {

    private static final long serialVersionUID = -8655824240239219380L;

    public static final BooleanAttributeValue TRUE = new BooleanAttributeValue(true);
    public static final BooleanAttributeValue FALSE = new BooleanAttributeValue(false);

    private final boolean value;

    private BooleanAttributeValue(final boolean value) {
        this.value = value;
    }

    public int hashCode() {
        return value ? 1 : 0;
    }

    public boolean equals(final Object other) {
        return other instanceof BooleanAttributeValue && equals((BooleanAttributeValue) other);
    }

    public boolean equals(final AttributeValue other) {
        return other instanceof BooleanAttributeValue && equals((BooleanAttributeValue) other);
    }

    public boolean equals(final BooleanAttributeValue other) {
        return other != null && other.value == value;
    }

    public String toString() {
        return Boolean.toString(value);
    }

    public int compareTo(final AttributeValue other) {
        throw new IllegalArgumentException("Not comparable");
    }

    void escapeTo(final StringBuilder builder) {
        builder.append(value);
    }
}
