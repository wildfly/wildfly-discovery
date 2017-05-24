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

import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceDesignationTestCase {
    public ServiceDesignationTestCase() {
    }

    @Test
    public void testImpliesServiceTypeToServiceType() throws Exception {
        assertTrue(ServiceType.of("abstract", null).implies(ServiceType.of("abstract", null)));
        assertTrue(ServiceType.of("abstract", "ata").implies(ServiceType.of("abstract", "ata")));
        assertFalse(ServiceType.of("abstract", null).implies(ServiceType.of("abstract", "ata")));
        assertFalse(ServiceType.of("abstract", "ata").implies(ServiceType.of("abstract", null)));
    }

    @Test
    public void testImpliesServiceTypeToServiceURL1() throws Exception {
        assertTrue(ServiceType.of("abstract", "ata").implies(buildSimpleServiceURL("abstract", "ata", null, URI.create("foo://bar.com:8080"))));
        assertTrue(ServiceType.of("abstract", null).implies(buildSimpleServiceURL("abstract", null, null, URI.create("foo://bar.com:8080"))));
        assertTrue(ServiceType.of("abstract", null).implies(buildSimpleServiceURL("abstract", null, "foobar", URI.create("foo://bar.com:8080"))));
        assertFalse(ServiceType.of("abstract", "ata").implies(buildSimpleServiceURL("abstract", null, "foobar", URI.create("foo://bar.com:8080"))));
        assertTrue(ServiceType.of("abstract", "ata").implies(buildSimpleServiceURL("abstract", "ata", "foobar", URI.create("foo://bar.com:8080"))));
        assertTrue(ServiceType.of("abstract", "ata").implies(buildSimpleServiceURL("abstract", "ata", null, URI.create("foo://bar.com:8080"))));
    }

    @Test
    public void testImpliesServiceTypeToServiceURL2() throws Exception {
        assertTrue(ServiceType.of("abstract", "ata", "foo", null).implies(buildSimpleServiceURL("abstract", "ata", null, URI.create("foo://bar.com:8080"))));
        assertTrue(ServiceType.of("abstract", null, "foo", null).implies(buildSimpleServiceURL("abstract", null, null, URI.create("foo://bar.com:8080"))));
        assertTrue(ServiceType.of("abstract", null, "foo", "foobar").implies(buildSimpleServiceURL("abstract", null, "foobar", URI.create("foo://bar.com:8080"))));
        assertFalse(ServiceType.of("abstract", "ata", "foo", "foobar").implies(buildSimpleServiceURL("abstract", null, "foobar", URI.create("foo://bar.com:8080"))));
        assertTrue(ServiceType.of("abstract", "ata", "foo", "foobar").implies(buildSimpleServiceURL("abstract", "ata", "foobar", URI.create("foo://bar.com:8080"))));
        assertTrue(ServiceType.of("abstract", "ata", "foo", null).implies(buildSimpleServiceURL("abstract", "ata", null, URI.create("foo://bar.com:8080"))));
    }

    static ServiceURL buildSimpleServiceURL(String abstractType, String abstractTypeAuthority, String concreteAuthority, URI uri) {
        return new ServiceURL.Builder().setAbstractType(abstractType).setAbstractTypeAuthority(abstractTypeAuthority).setUriSchemeAuthority(concreteAuthority).setUri(uri).create();
    }
}
