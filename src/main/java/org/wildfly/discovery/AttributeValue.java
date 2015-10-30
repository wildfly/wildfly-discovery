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

import java.io.Serializable;
import java.util.regex.Pattern;

import org.wildfly.common.Assert;

/**
 * A service attribute value.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AttributeValue implements Serializable {
    private static final long serialVersionUID = -686442104196584084L;

    AttributeValue() {
    }

    /**
     * Get the hash code of this attribute value.  Attribute values are suitable for usage as hash table keys.
     *
     * @return the hash code
     */
    public abstract int hashCode();

    /**
     * Determine whether this object is equal to the other.
     *
     * @param other the other object
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    public abstract boolean equals(Object other);

    /**
     * Determine whether this object is equal to the other.
     *
     * @param other the other object
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    public abstract boolean equals(AttributeValue other);

    /**
     * Get the string representation of this attribute value.
     *
     * @return the string representation of this attribute value (not {@code null})
     */
    public abstract String toString();

    /**
     * Determine whether this attribute value has the same type as another.
     *
     * @param other the other attribute value (must not be {@code null})
     * @return {@code true} if the other value is of the same type as this one, {@code false} otherwise
     */
    public boolean isSameTypeAs(AttributeValue other) {
        Assert.checkNotNullParam("other", other);
        return getClass() == other.getClass();
    }

    /**
     * Compare this attribute value with another for sorting purposes.
     *
     * @param other the other value (must not be {@code null})
     * @return a negative, zero, or positive value if this value comes before, is equal to, or comes after the given one
     */
    public abstract int compareTo(AttributeValue other);

    private static final Pattern POSSIBLE_INTEGER = Pattern.compile("-?\\d{1,10}");

    /**
     * Get an attribute value from the given {@code int}.
     *
     * @param value the {@code int} value
     * @return the attribute value
     */
    public static IntegerAttributeValue fromInt(int value) {
        return new IntegerAttributeValue(value);
    }


    public static AttributeValue fromEncodedString(String str) {
        if (str.equals("true")) {
            return BooleanAttributeValue.TRUE;
        } else if (str.equals("false")) {
            return BooleanAttributeValue.FALSE;
        } else if (POSSIBLE_INTEGER.matcher(str).matches()) {
            try {
                int val = Integer.parseInt(str);
                return new IntegerAttributeValue(val);
            } catch (NumberFormatException ignored) {
                return new StringAttributeValue(str);
            }
        } else {
            return new StringAttributeValue(str);
        }
    }

    public static AttributeValue fromBytes(final byte[] value) {
        return new OpaqueAttributeValue(value, 0, value.length);
    }

    abstract void escapeTo(final StringBuilder builder);


}
