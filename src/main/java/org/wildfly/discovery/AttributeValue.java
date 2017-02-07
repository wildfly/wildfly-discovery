/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
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

import java.io.Serializable;

import org.wildfly.common.Assert;

/**
 * An attribute value describing some aspect of a service.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AttributeValue implements Comparable<AttributeValue>, Serializable {
    private static final long serialVersionUID = - 9125386269584070124L;

    // sorted in order of sorted appearance
    static final int K_OPAQUE = 0;
    static final int K_NUMERIC = 1;
    static final int K_STRING = 2;
    static final int K_BOOLEAN_TRUE = 3;
    static final int K_BOOLEAN_FALSE = 4;

    private transient String toString;

    AttributeValue() {
    }

    AttributeValue(final String initialToString) {
        toString = initialToString;
    }

    /**
     * The "true" boolean attribute value.
     */
    @SuppressWarnings("StaticInitializerReferencesSubClass")
    public static final BooleanAttributeValue TRUE = BooleanAttributeValue.TRUE;

    /**
     * The "false" boolean attribute value.
     */
    @SuppressWarnings("StaticInitializerReferencesSubClass")
    public static final BooleanAttributeValue FALSE = BooleanAttributeValue.FALSE;


    /**
     * Create an attribute value from a string.  The resultant value will return {@code true} from {@link #isString()}.
     *
     * @param string the attribute value string (must not be {@code null})
     * @return the attribute value object
     */
    public static AttributeValue fromString(String string) {
        Assert.checkNotNullParam("string", string);
        return new StringAttributeValue(string);
    }

    /**
     * Create an attribute value from a byte sequence.  The resultant value will return {@code true} from {@link #isOpaque()}.
     *
     * @param bytes the bytes to read (must not be {@code null})
     * @return the attribute value object
     */
    public static AttributeValue fromBytes(byte[] bytes) {
        Assert.checkNotNullParam("bytes", bytes);
        return new OpaqueAttributeValue(bytes, true);
    }

    /**
     * Create an attribute value from an integer.  The resultant value will return {@code true} from {@link #isNumeric()}.
     *
     * @param value the value to use
     * @return the attribute value object
     */
    public static AttributeValue fromInt(int value) {
        return new NumericAttributeValue(value);
    }

    /**
     * Determine if this value is a boolean value.
     *
     * @return {@code true} if the value is a boolean, {@code false} otherwise
     */
    public boolean isBoolean() {
        return false;
    }

    /**
     * Determine if this value is numeric.
     *
     * @return {@code true} if the value is numeric, {@code false} otherwise
     */
    public boolean isNumeric() {
        return false;
    }

    /**
     * Determine if this value is opaque (binary).
     *
     * @return {@code true} if the value is opaque, {@code false} otherwise
     */
    public boolean isOpaque() {
        return false;
    }

    /**
     * Determine if this value is a text string.
     *
     * @return {@code true} if the value is a string, {@code false} otherwise
     */
    public boolean isString() {
        return false;
    }

    /**
     * Get the value as an integer, throwing an exception if it is not numeric.
     *
     * @return the integer value
     * @throws IllegalArgumentException if the value is not numeric
     */
    public int asInt() throws IllegalArgumentException {
        throw new IllegalArgumentException();
    }

    abstract int getKind();

    /**
     * Compare this value to another.
     *
     * @param other the other value
     * @return -1, 0, or 1 if the value comes before, is the same as, or comes after the given value
     */
    public int compareTo(final AttributeValue other) {
        Assert.checkNotNullParam("other", other);
        return signum(other.getKind() - getKind());
    }

    /**
     * Determine if this attribute value is equal to another.
     *
     * @param obj the other object
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    public abstract boolean equals(Object obj);

    /**
     * Determine if this attribute value is equal to another.
     *
     * @param obj the other object
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    public abstract boolean equals(AttributeValue obj);

    /**
     * Get a string representation of this attribute value.
     *
     * @return the string (not {@code null})
     */
    public String toString() {
        final String toString = this.toString;
        if (toString == null) {
            return this.toString = generateToString();
        }
        return toString;
    }

    abstract String generateToString();
}
