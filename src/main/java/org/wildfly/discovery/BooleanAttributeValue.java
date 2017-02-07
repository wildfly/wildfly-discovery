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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class BooleanAttributeValue extends AttributeValue {
    private static final long serialVersionUID = - 5223377657098950186L;

    private final boolean value;

    static final BooleanAttributeValue TRUE = new BooleanAttributeValue(true);
    static final BooleanAttributeValue FALSE = new BooleanAttributeValue(false);

    private BooleanAttributeValue(final boolean value) {
        super(Boolean.toString(value));
        this.value = value;
    }

    String generateToString() {
        return Boolean.toString(value);
    }

    public boolean equals(final Object obj) {
        return obj instanceof BooleanAttributeValue && equals((BooleanAttributeValue) obj);
    }

    public boolean equals(final AttributeValue obj) {
        return obj instanceof BooleanAttributeValue && equals((BooleanAttributeValue) obj);
    }

    public boolean equals(final BooleanAttributeValue obj) {
        return obj == this || obj != null && value == obj.value;
    }

    public int hashCode() {
        return Boolean.hashCode(value);
    }

    int getKind() {
        return value ? K_BOOLEAN_TRUE : K_BOOLEAN_FALSE;
    }

    public boolean isBoolean() {
        return true;
    }

    Object readResolve() {
        return value ? TRUE : FALSE;
    }

    Object writeReplace() {
        return value ? TRUE : FALSE;
    }
}
