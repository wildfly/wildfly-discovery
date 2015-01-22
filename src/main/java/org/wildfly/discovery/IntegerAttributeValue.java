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

import static java.lang.Integer.signum;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class IntegerAttributeValue extends AttributeValue implements Comparable<IntegerAttributeValue> {

    private static final long serialVersionUID = -1726933123983910572L;

    private final int value;

    IntegerAttributeValue(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public int hashCode() {
        return value;
    }

    public boolean equals(final Object other) {
        return other instanceof IntegerAttributeValue && equals((IntegerAttributeValue) other);
    }

    public boolean equals(final AttributeValue other) {
        return other instanceof IntegerAttributeValue && equals((IntegerAttributeValue) other);
    }

    public boolean equals(final IntegerAttributeValue other) {
        return other != null && value == other.value;
    }

    public String toString() {
        return Integer.toString(value);
    }

    public int compareTo(final AttributeValue other) {
        if (! (other instanceof IntegerAttributeValue)) throw new IllegalArgumentException("Not comparable");
        return compareTo((IntegerAttributeValue) other);
    }

    void escapeTo(final StringBuilder builder) {
        builder.append(value);
    }

    public int compareTo(final IntegerAttributeValue other) {
        return signum(value - other.value);
    }
}
