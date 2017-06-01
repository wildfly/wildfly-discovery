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

import java.util.Collection;
import java.util.ListIterator;
import java.util.Map;

/**
 * A filter specification matching any of a set of sub-filters.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AnyFilterSpec extends FilterSpec implements Iterable<FilterSpec> {

    private final FilterSpec[] children;

    AnyFilterSpec(final FilterSpec... specs) {
        if (specs.length == 0) {
            throw new IllegalArgumentException("No child filters specified");
        }
        children = specs;
    }

    public boolean matchesSimple(final Map<String, AttributeValue> attributes) {
        for (FilterSpec child : children) {
            if (child.matchesSimple(attributes)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesMulti(final Map<String, ? extends Collection<AttributeValue>> attributes) {
        for (FilterSpec child : children) {
            if (child.matchesMulti(attributes)) {
                return true;
            }
        }
        return false;
    }

    public boolean mayMatch(final Collection<String> attributeNames) {
        for (FilterSpec child : children) {
            if (child.mayMatch(attributeNames)) {
                return true;
            }
        }
        return false;
    }

    public boolean mayNotMatch(final Collection<String> attributeNames) {
        for (FilterSpec child : children) {
            if (child.mayNotMatch(attributeNames)) {
                return false;
            }
        }
        return true;
    }

    public <P, R, E extends Exception> R accept(Visitor<P, R, E> visitor, P parameter) throws E {
        return visitor.handle(this, parameter);
    }

    void toString(final StringBuilder builder) {
        builder.append('(').append('|');
        for (FilterSpec child : children) {
            child.toString(builder);
        }
        builder.append(')');
    }

    public ListIterator<FilterSpec> iterator() {
        return new ArrayIterator<FilterSpec>(children);
    }
}
