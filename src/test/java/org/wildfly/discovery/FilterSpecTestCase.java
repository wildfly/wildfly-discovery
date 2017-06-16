package org.wildfly.discovery;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.discovery.impl.StaticDiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.discovery.Utils.AttributeValuePair;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * Tests for FilterSpec functionality
 *
 * @author <a href="mailto:rachmato@redhat.com">Richard Achmatowicz</a>
 */
public final class FilterSpecTestCase {

    private static DiscoveryProvider provider = null;
    private static Discovery discovery = null;

    /**
     * Do any general setup here.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setup() throws Exception {

        List<ServiceURL> list = new ArrayList<ServiceURL>();

        // add some Service URLs
        AttributeValuePair clusterPair = new AttributeValuePair("cluster","c");
        // service:ejb.jboss:http://host:8080;cluster=c
        ServiceURL cluster = Utils.buildServiceURL("ejb","jboss", new URL("http://host:8080").toURI(), clusterPair);
        list.add(cluster);

        AttributeValuePair modulePair = new AttributeValuePair("module","m");
        // service:ejb.jboss:http://host:8080;module=m
        ServiceURL module = Utils.buildServiceURL("ejb", "jboss", new URL("http://host:8080").toURI(), modulePair);
        list.add(module);

        // service:ejb.jboss:http://host:8080;cluster=c,module=m
        ServiceURL combo = Utils.buildServiceURL("ejb", "jboss", new URL("http://host:8080").toURI(), clusterPair, modulePair);
        list.add(combo);

        // service:ejb.jboss:http-remoting://host:8080
        ServiceURL specialProtocol = Utils.buildServiceURL("ejb", "jboss", new URI("http-remoting", null, "host", 8080, null, null, null));
        list.add(specialProtocol);

        // set up a DiscoveryProvider containing the ServiceURLs
        provider = new StaticDiscoveryProvider(list);

        // create a Discovery instance to permit discovery of the SerrviceURLs based on matching attributes
        discovery = Discovery.create(provider);
    }

    /**
     * Do any test specific setup here
     */
    @Before
    public void setupTest() {
    }

    /**
     * Some basic checks on FilterSpec format
     */
    @Test
    public void testFilterSpecContents() {
        // specify attribute=*
        FilterSpec attr = FilterSpec.hasAttribute("fred");
        assertEquals(attr.toString(),"(fred=*)");
        assertTrue(attr.mayMatch(Collections.singleton("fred")));
        assertFalse(attr.mayMatch(Collections.singleton("wilma")));
        assertFalse(attr.mayNotMatch(Collections.singleton("fred")));
        assertTrue(attr.mayNotMatch(Collections.singleton("wilma")));

        // specify attribute=X
        FilterSpec equals = FilterSpec.equal("fred","barney");
        assertEquals(equals.toString(),"(fred=barney)");
        assertTrue(equals.mayMatch(Collections.singleton("fred")));
        assertFalse(equals.mayMatch(Collections.singleton("wilma")));
        assertTrue(equals.mayNotMatch(Collections.singleton("fred")));
        assertTrue(equals.mayNotMatch(Collections.singleton("wilma")));

        // specify all
        FilterSpec all = FilterSpec.all(attr, equals);
        assertEquals(all.toString(),"(&(fred=*)(fred=barney))");
        assertTrue(all.mayMatch(Collections.singleton("fred")));
        assertFalse(all.mayMatch(Collections.singleton("wilma")));
        assertTrue(all.mayNotMatch(Collections.singleton("fred")));
        assertTrue(all.mayNotMatch(Collections.singleton("wilma")));

        all = FilterSpec.fromString("(&(fred=one)(barney=two))");
        assertEquals(all.toString(),"(&(fred=one)(barney=two))");
        assertFalse(all.mayMatch(Collections.singleton("fred")));
        assertFalse(all.mayMatch(Collections.singleton("barney")));
        assertTrue(all.mayMatch(new HashSet<>(Arrays.asList("fred", "barney"))));
        assertTrue(all.mayMatch(new HashSet<>(Arrays.asList("fred", "barney", "wilma"))));
        assertFalse(all.mayMatch(new HashSet<>(Arrays.asList("fred", "wilma"))));
        assertFalse(all.mayMatch(Collections.singleton("bob")));
        assertTrue(all.mayNotMatch(Collections.singleton("fred")));
        assertTrue(all.mayNotMatch(new HashSet<>(Arrays.asList("fred", "barney"))));
        assertTrue(all.mayNotMatch(Collections.singleton("wilma")));
        assertTrue(all.mayNotMatch(new HashSet<>(Arrays.asList("fred", "wilma"))));

        // specify any
        FilterSpec any = FilterSpec.any(attr, equals);
        assertEquals(any.toString(),"(|(fred=*)(fred=barney))");
        assertTrue(any.mayMatch(Collections.singleton("fred")));
        assertFalse(any.mayMatch(Collections.singleton("bob")));
        assertFalse(any.mayNotMatch(Collections.singleton("fred")));
        assertTrue(any.mayNotMatch(Collections.singleton("bob")));
    }

    /**
     * A basic check on single attribute matching
     */
    @Test
    public void testDiscoverySingleAttribute() {

        FilterSpec cluster = FilterSpec.equal("cluster","c");
        List<ServiceURL> results = new ArrayList<ServiceURL>();

        // call discovery for single attribute
        System.out.println("Calling discover for filterspec " + cluster);
        try (final ServicesQueue servicesQueue = discover(cluster)) {
            Utils.drainServicesQueue(servicesQueue, results);
        } catch (InterruptedException ie) {
            Assert.fail("Discovery was interrupted ...");
        }
        // we should get two result back
        assertEquals("Should get two results back", 2, results.size());
    }

    /**
     * A basic check on single attribute matching
     */
    @Test
    public void testDiscoverySingleAttributeWithPredicate() {

        FilterSpec cluster = FilterSpec.equal("cluster","c");
        List<ServiceURL> results = new ArrayList<ServiceURL>();

        // check for specific protocol versions
        Predicate<ServiceURL> predicate = serviceURL -> {
           return (serviceURL.getUriScheme().equals("http-remoting") || serviceURL.getUriScheme().equals("http+remoting"));
        };

        // call discovery for single attribute
        System.out.println("Calling discover for filterspec " + cluster);
        try (final ServicesQueue servicesQueue = discover(cluster, predicate)) {
            Utils.drainServicesQueue(servicesQueue, results);
        } catch (InterruptedException ie) {
            Assert.fail("Discovery was interrupted ...");
        }
        // we should get one result back
        assertEquals("Should get one result back", 1, results.size());
    }

    /**
     *  A basic check on multi-attribute matching
     */
    @Test
    public void testDiscoveryMultipleAttributes() {

        FilterSpec cluster = FilterSpec.equal("cluster","c");
        FilterSpec module = FilterSpec.equal("module","m");
        FilterSpec all = FilterSpec.all(cluster,module);

        List<ServiceURL> results = null;

        // call discovery for multiple attribute
        System.out.println("Calling discover for filterspec " + all);
        try (final ServicesQueue servicesQueue = discover(all)) {
            results = Utils.drainServicesQueue(servicesQueue);
        } catch (InterruptedException ie) {
            Assert.fail("Discovery was interrupted ...");
        }
        // we should get one result back
        assertEquals("Should get one result back", 1, results.size());
    }

    /**
     * Do any test-specific tear down here.
     */
    @After
    public void tearDownTest() {
    }

    /**
     * Do any general tear down here.
     */
    @AfterClass
    public static void tearDown() {
        provider = null;
        discovery = null;
    }

    /**
     * Returns a queue of registered ServiceURLs which match the filter spec
     * @param filterSpec a condition on attributes to match (may be (@code null))
     * @return ServicesQueue a ServicesQueue of ServiceURLs
     */
    private static ServicesQueue discover(FilterSpec filterSpec) {
        ServiceType serviceType = new ServiceType("ejb","jboss", null, null);
        return discovery.discover(serviceType, filterSpec, p -> true);
    }

    /**
     * Returns a queue of registered ServiceURLs which match the filter spec
     * @param filterSpec a condition on attributes to match (may be (@code null))
     * @param predicate a predicate on ServiceURLs to satisfy (may be (@code null))
     * @return ServicesQueue a ServicesQueue of ServiceURLs
     */
    private static ServicesQueue discover(FilterSpec filterSpec, Predicate<ServiceURL> predicate) {
        ServiceType serviceType = new ServiceType("ejb","jboss", null, null);
        return discovery.discover(serviceType, filterSpec, predicate);
    }
}
