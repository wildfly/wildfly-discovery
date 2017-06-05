package org.wildfly.discovery;

import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests parsing of discovery configuration files.
 *
 * This test does the following:
 * - uses a wildfly-config.xml file on the classpath to populate ConfiguredProvider with some ServiceURLs
 * - verifies that the ServiceURLs defined there can be found via discovery
 *
 * @author <a href="mailto:rachmato@redhat.com">Richard Achmatowicz</a>
 */
public class ParsingTestCase {

    /**
     * Check that serviceURLs registered in ConfiguredProvider are available
     */
    @Test
    public void testConfiguredProviderContents() {

        // get the default discovery supplier, which should be ConfigiuredProvider
        Discovery discovery = Discovery.getContextManager().getPrivilegedSupplier().get();

        // ServiceTypes that we defined in the configuration file
        ServiceType nodeServiceType = new ServiceType("ejb", "jboss", "node:myNode", null);
        ServiceType URIServiceType = new ServiceType("ejb", "jboss", "http-remoting://15.16.17.18:8080", null);

        // some filter specs to retrieve the SeviceURLs
        FilterSpec cluster = FilterSpec.equal("cluster","myCluster");
        FilterSpec node = FilterSpec.equal("node","myNode");

        ServiceType basicServiceType = new ServiceType("ejb", "jboss", null, null);

        // call discovery to retrieve the abstract node we defined
        List<ServiceURL> nodeResults = new ArrayList<ServiceURL>();
        try (final ServicesQueue servicesQueue = discovery.discover(basicServiceType, cluster)) {
            ServiceURL serviceURL = servicesQueue.takeService();
            while (serviceURL != null) {
                nodeResults.add(serviceURL);
                serviceURL = servicesQueue.takeService();
            }
        } catch (InterruptedException ie) {
            Assert.fail("Discovery was interrupted!");
        }
        // we should get two result back
        assertEquals("abstract node was not parsed correctly! ", 1, nodeResults.size());
        nodeResults.get(0).implies(nodeServiceType);

        // call discovery to retrieve the concrete URI we defined
        List<ServiceURL> URIResults = new ArrayList<ServiceURL>();
        try (final ServicesQueue servicesQueue = discovery.discover(basicServiceType, node)) {
            ServiceURL serviceURL = servicesQueue.takeService();
            while (serviceURL != null)  {
                URIResults.add(serviceURL);
                serviceURL = servicesQueue.takeService();
            }
        } catch (InterruptedException ie) {
            Assert.fail("Discovery was interrupted!");
        }
        // we should get two result back
        assertEquals("concrete node was not parsed correctly! ", 1, URIResults.size());
        URIResults.get(0).implies(URIServiceType);
    }
}
