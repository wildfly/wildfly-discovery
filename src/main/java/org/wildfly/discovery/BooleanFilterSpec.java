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
import java.util.Map;

/**
 * A filter spec which is either always true or always false.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BooleanFilterSpec extends FilterSpec {
    private final boolean value;

    BooleanFilterSpec(final boolean value) {
        this.value = value;
    }

    public boolean matchesSimple(final Map<String, AttributeValue> attributes) {
        return value;
    }

    public boolean matchesMulti(final Map<String, ? extends Collection<AttributeValue>> attributes) {
        return value;
    }

    public boolean mayMatch(final Collection<String> attributeNames) {
        return value;
    }

    public boolean mayNotMatch(final Collection<String> attributeNames) {
        return ! value;
    }

    public <P, R, E extends Exception> R accept(Visitor<P, R, E> visitor, P parameter) throws E {
        return visitor.handle(this, parameter);
    }

    public boolean getValue() {
        return value;
    }

    void toString(final StringBuilder builder) {
        if (value) {
            builder.append("*");
        } else {
            builder.append("!*");
        }
    }
}
