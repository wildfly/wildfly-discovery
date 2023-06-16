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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;

abstract class ComparisonFilterSpecTestBase {
    final static String key = "name";
    final static String node1 = "node1";
    static FilterSpec filter1;

    void testBasic(String toStringValue,
                   BiFunction<String, String, FilterSpec> createMethod) {
        FilterSpec filter1Again = createMethod.apply(key, node1);
        FilterSpec filter2 = createMethod.apply(key, "node2");

        assertEquals(filter1, filter1);
        assertEquals(filter1.hashCode(), filter1.hashCode());
        assertEquals(filter1, filter1Again);
        assertEquals(filter1.hashCode(), filter1Again.hashCode());

        assertNotEquals(filter1, filter2);
        assertNotEquals(filter1, key);
        assertNotEquals(filter1, FilterSpec.hasAttribute(key));

        assertEquals(toStringValue, filter1.toString());
    }

    void testMatch() {
        final Set<String> attributeNames = Collections.singleton(key);
        assertTrue(filter1.mayNotMatch(attributeNames));
        assertTrue(filter1.mayMatch(attributeNames));

        assertFalse(filter1.willMatch(attributeNames));
        assertFalse(filter1.willNotMatch(attributeNames));
    }
}
