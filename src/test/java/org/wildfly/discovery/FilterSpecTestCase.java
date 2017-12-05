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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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
        ServiceURL cluster = buildAttributeServiceURL(clusterPair);
        list.add(cluster);

        AttributeValuePair modulePair = new AttributeValuePair("module","m");
        ServiceURL module = buildAttributeServiceURL(modulePair);
        list.add(module);

        ServiceURL combo = buildAttributeServiceURL(clusterPair, modulePair);
        list.add(combo);

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

        FilterSpec emptyAll = FilterSpec.all();
        assertEquals("(&)", emptyAll.toString());
        emptyAll = FilterSpec.fromString("(&)");
        assertEquals("(&)", emptyAll.toString());
        emptyAll = FilterSpec.fromString("*");
        assertEquals("(&)", emptyAll.toString());

        FilterSpec emptyNotAll = FilterSpec.not(FilterSpec.all());
        assertEquals("(!(&))", emptyNotAll.toString());
        emptyNotAll = FilterSpec.fromString("(!(&))");
        assertEquals("(!(&))", emptyNotAll.toString());
        emptyNotAll = FilterSpec.fromString("(!*)");
        assertEquals("(!(&))", emptyNotAll.toString());

        FilterSpec emptyAny = FilterSpec.none();
        assertEquals("(|)", emptyAny.toString());
        emptyAny = FilterSpec.fromString("(|)");
        assertEquals("(|)", emptyAny.toString());
        emptyAny = FilterSpec.fromString("!*");
        assertEquals("(|)", emptyAny.toString());

        FilterSpec emptyNotAny = FilterSpec.not(FilterSpec.none());
        assertEquals("(!(|))", emptyNotAny.toString());
        emptyNotAny = FilterSpec.fromString("(!(|))");
        assertEquals("(!(|))", emptyNotAny.toString());
        emptyNotAny = FilterSpec.fromString("(!!*)");
        assertEquals("(!(|))", emptyNotAny.toString());
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
            ServiceURL serviceURL = servicesQueue.takeService();
            do {
                System.out.println("ServiceURL found = " + serviceURL);
                results.add(serviceURL);

                serviceURL = servicesQueue.takeService();
            } while (serviceURL != null) ;
        } catch (InterruptedException ie) {
            Assert.fail("Discovery was interrupted ...");
        }
        // we should get two result back
        assertEquals(results.size(),2);
    }

    /**
     *  A basic check on multi-attribute matching
     */
    @Test
    public void testDiscoveryMultipleAttributes() {

        FilterSpec cluster = FilterSpec.equal("cluster","c");
        FilterSpec module = FilterSpec.equal("module","m");
        FilterSpec all = FilterSpec.all(cluster,module);

        List<ServiceURL> results = new ArrayList<ServiceURL>();

        // call discovery for single attribute
        System.out.println("Calling discover for filterspec " + all);
        try (final ServicesQueue servicesQueue = discover(all)) {
            ServiceURL serviceURL = servicesQueue.takeService();
            do {
                System.out.println("ServiceURL found = " + serviceURL);
                results.add(serviceURL);

                serviceURL = servicesQueue.takeService();
            } while (serviceURL != null) ;
        } catch (InterruptedException ie) {
            Assert.fail("Discovery was interrupted ...");
        }
        // we should get one results back
        assertEquals(results.size(),1);
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
     * @param filterSpec a condition on attributes to match
     * @return
     */
    private static ServicesQueue discover(FilterSpec filterSpec) {
        ServiceType serviceType = new ServiceType("ejb","jboss", null, null);
        return discovery.discover(serviceType, filterSpec);
    }

    /**
     * An attribute value pair
     */
    private static class AttributeValuePair {
        String attribute = null;
        String value = null;

        public AttributeValuePair(String attribute, String value) {
            this.attribute = attribute;
            this.value = value;
        }

        public String getAttribute() {
            return attribute;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Builds ServiceURLs with constant default type and varying attributes.
     *
     * @param pairs one or more attribute pairs to be set in the ServiceURL
     * @return a configured ServiceURL
     * @throws Exception
     */
    private static ServiceURL buildAttributeServiceURL(AttributeValuePair ...pairs) throws Exception {

        final ServiceURL.Builder builder = new ServiceURL.Builder();
        // set the locationURI
        builder.setUri(new URI("http://myhost.com"));
        builder.setAbstractType("ejb");
        builder.setAbstractTypeAuthority("jboss");
        // add an attribute
        for (AttributeValuePair pair : pairs) {
            builder.addAttribute(pair.getAttribute(), AttributeValue.fromString(pair.getValue()));
        }
        return builder.create();
    }
}
