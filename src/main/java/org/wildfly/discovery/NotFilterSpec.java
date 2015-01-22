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
class NotFilterSpec extends FilterSpec {
    private final FilterSpec child;

    NotFilterSpec(final FilterSpec child) {
        this.child = child;
    }

    public boolean matchesSimple(final Map<String, AttributeValue> attributes) {
        return ! child.matchesSimple(attributes);
    }

    public boolean matchesMulti(final Map<String, ? extends Collection<AttributeValue>> attributes) {
        return ! child.matchesMulti(attributes);
    }

    void toString(final StringBuilder builder) {
        builder.append('(').append('!');
        child.toString(builder);
        builder.append(')');
    }
}
