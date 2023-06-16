/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

public final class GreaterEqualFilterSpecTestCase extends ComparisonFilterSpecTestBase {
    @BeforeClass
    public static void beforeClass() {
        filter1 = FilterSpec.greaterOrEqual(key, node1);
    }

    @Test
    public void testBasic() {
        super.testBasic("(name>=node1)", FilterSpec::greaterOrEqual);
        assertEquals(key, ((GreaterEqualFilterSpec) filter1).getAttribute());
        assertEquals(AttributeValue.fromString(node1), ((GreaterEqualFilterSpec) filter1).getValue());
    }

    @Test
    public void testMatch() {
        super.testMatch();
    }

    @Test
    public void testMatchSimple() {
        final HashMap<String, AttributeValue> attributes = new HashMap<>(4);

        // with the same attribute key and value
        attributes.put(key, AttributeValue.fromString(node1));
        assertTrue(filter1.matchesSimple(attributes));

        // with same attribute key but smaller value (the subject filter has greater value)
        attributes.put(key, AttributeValue.fromString("node0"));
        assertTrue(filter1.matchesSimple(attributes));

        // with additional non-matching attribute key and value
        attributes.put("x", AttributeValue.fromString("node2"));
        assertTrue(filter1.matchesSimple(attributes));

        // attribute key binds to a greater value (the subject filter has smaller value)
        attributes.put(key, AttributeValue.fromString("node2"));
        assertFalse(filter1.matchesSimple(attributes));
    }

    @Test
    public void testMatchMulti() {
        final Map<String, Collection<AttributeValue>> attributes = new HashMap<>(4);
        final Set<AttributeValue> value = new HashSet<>(4);
        attributes.put(key, value);

        // with the same attribute key and value
        value.add(AttributeValue.fromString(node1));
        assertTrue(filter1.matchesMulti(attributes));

        // add a second element to the value collection
        value.add(AttributeValue.fromString("node2"));
        assertTrue(filter1.matchesMulti(attributes));

        // reset the value to a bigger value (the subject filter has smaller value)
        value.clear();
        value.add(AttributeValue.fromString("node2"));
        assertFalse(filter1.matchesMulti(attributes));

        // add a smaller value (the subject filter has greater value)
        value.add(AttributeValue.fromString("node0"));
        assertTrue(filter1.matchesMulti(attributes));
    }
}
