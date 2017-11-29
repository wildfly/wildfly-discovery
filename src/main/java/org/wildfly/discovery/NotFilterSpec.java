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
public final class NotFilterSpec extends FilterSpec {
    private final FilterSpec child;
    private transient int hashCode;

    NotFilterSpec(final FilterSpec child) {
        this.child = child;
    }

    public boolean matchesSimple(final Map<String, AttributeValue> attributes) {
        return ! child.matchesSimple(attributes);
    }

    public boolean matchesMulti(final Map<String, ? extends Collection<AttributeValue>> attributes) {
        return ! child.matchesMulti(attributes);
    }

    public boolean mayMatch(final Collection<String> attributeNames) {
        return child.mayNotMatch(attributeNames);
    }

    public boolean mayNotMatch(final Collection<String> attributeNames) {
        return child.mayMatch(attributeNames);
    }

    public <P, R, E extends Exception> R accept(Visitor<P, R, E> visitor, P parameter) throws E {
        return visitor.handle(this, parameter);
    }

    /**
     * Get the child (inverted) filter spec.
     *
     * @return the child (inverted) filter spec
     */
    public FilterSpec getChild() {
        return child;
    }

    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = getClass().hashCode() * 19 + child.hashCode();
            if (hashCode == 0) hashCode = 1 << 30;
            return this.hashCode = hashCode;
        }
        return hashCode;
    }

    public boolean equals(final FilterSpec other) {
        return other instanceof NotFilterSpec && equals((NotFilterSpec) other);
    }

    public boolean equals(final NotFilterSpec other) {
        return this == other || other != null && child.equals(other.child);
    }

    void toString(final StringBuilder builder) {
        builder.append('(').append('!');
        child.toString(builder);
        builder.append(')');
    }
}
