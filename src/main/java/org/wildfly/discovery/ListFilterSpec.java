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
class ListFilterSpec extends FilterSpec {

    private final boolean all;
    private final FilterSpec[] children;

    ListFilterSpec(final boolean all, final FilterSpec... specs) {
        if (specs.length == 0) {
            throw new IllegalArgumentException("No child filters specified");
        }
        this.all = all;
        children = specs;
    }

    public boolean matchesSimple(final Map<String, AttributeValue> attributes) {
        for (FilterSpec child : children) {
            if (child.matchesSimple(attributes) != all) {
                return ! all;
            }
        }
        return all;
    }

    public boolean matchesMulti(final Map<String, ? extends Collection<AttributeValue>> attributes) {
        for (FilterSpec child : children) {
            if (child.matchesMulti(attributes) != all) {
                return ! all;
            }
        }
        return all;
    }

    void toString(final StringBuilder builder) {
        builder.append('(').append(all ? '&' : '|');
        for (FilterSpec child : children) {
            child.toString(builder);
        }
        builder.append(')');
    }
}
