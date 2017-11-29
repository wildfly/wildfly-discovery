/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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
final class NumericAttributeValue extends AttributeValue {
    private static final long serialVersionUID = - 5223377657098950186L;

    private final int value;

    NumericAttributeValue(final int value) {
        this.value = value;
    }

    String generateToString() {
        return Integer.toString(value);
    }

    public int compareTo(final AttributeValue other) {
        if (other instanceof NumericAttributeValue) {
            return signum(value - ((NumericAttributeValue) other).value);
        } else {
            return super.compareTo(other);
        }
    }

    public boolean equals(final Object obj) {
        return obj instanceof NumericAttributeValue && equals((NumericAttributeValue) obj);
    }

    public boolean equals(final AttributeValue obj) {
        return obj instanceof NumericAttributeValue && equals((NumericAttributeValue) obj);
    }

    public boolean equals(final NumericAttributeValue obj) {
        return obj == this || obj != null && value == obj.value;
    }

    public int hashCode() {
        return value;
    }

    int getKind() {
        return K_NUMERIC;
    }

    public boolean isNumeric() {
        return true;
    }

    @Override
    public int asInt() {
        return this.value;
    }
}
