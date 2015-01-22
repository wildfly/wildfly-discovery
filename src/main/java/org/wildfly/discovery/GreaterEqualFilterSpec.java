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
class GreaterEqualFilterSpec extends FilterSpec {

    private final String attribute;
    private final AttributeValue value;

    GreaterEqualFilterSpec(final String attribute, final AttributeValue value) {
        this.attribute = attribute;
        this.value = value;
    }

    public boolean matchesSimple(final Map<String, AttributeValue> attributes) {
        final AttributeValue other = attributes.get(attribute);
        return value instanceof Comparable<?> && value.isSameTypeAs(other) && value.compareTo(other) >= 0;
    }

    public boolean matchesMulti(final Map<String, ? extends Collection<AttributeValue>> attributes) {
        if (value instanceof Comparable<?>) {
            final Collection<AttributeValue> collection = attributes.get(attributes);
            if (collection != null) for (AttributeValue value : collection) {
                if (this.value.isSameTypeAs(value) && this.value.compareTo(value) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    void toString(final StringBuilder builder) {
        builder.append('(');
        FilterSpec.escapeTo(attribute, builder);
        builder.append('>');
        builder.append('=');
        value.escapeTo(builder);
        builder.append(')');
    }
}
