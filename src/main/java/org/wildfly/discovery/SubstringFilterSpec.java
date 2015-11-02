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
final class SubstringFilterSpec extends FilterSpec {

    private final String attribute;
    private final String initialPart;
    private final String finalPart;

    public SubstringFilterSpec(final String attribute, final String initialPart, final String finalPart) {
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
