package org.wildfly.discovery;

import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryRequest;
import org.wildfly.discovery.spi.DiscoveryResult;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;

/**
 * Tests timeout functionality of calls to discovery
 *
 * @author <a href="mailto:rachmato@redhat.com">Richard Achmatowicz</a>
 */
public final class DiscoveryTimeoutTestCase {

    private static final Logger logger = Logger.getLogger(DiscoveryTimeoutTestCase.class);

    private static Discovery discoveryInstance1 = null;
    private static Discovery discoveryInstance2 = null;

    private static int PROVIDER_URL_GENERATION_DELAY = 5 * 1000;

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

        // create a Discovery instance using a DiscoveryProvider that delays generation of ServiceURLs
        // we need one statically defined provider per test :-(
        discoveryInstance1 = Discovery.create(new DelayedDiscoveryProvider(list, PROVIDER_URL_GENERATION_DELAY));
        discoveryInstance2 = Discovery.create(new DelayedDiscoveryProvider(list, PROVIDER_URL_GENERATION_DELAY));
    }

    /**
     * Do any test specific setup here
     */
    @Before
    public void setupTest() {
    }

    /**
     * Test accessing the queue when discovery instance is initialised with a timeout.
     * Tests the case where:
     * - takeService() is called before any ServiceURLs are ready, times out and returns null
     * - takeService() is called when a ServiceURL is ready, does not time out and returns a value
     */
    @Test
    public void testDiscoveryWithConstructorTimeout() {

        FilterSpec cluster = FilterSpec.equal("cluster","c");
        FilterSpec module = FilterSpec.equal("module","m");
        FilterSpec all = FilterSpec.all(cluster,module);

        // call discovery for single attribute
        logger.info("Calling discover for filterspec " + all);
        try {
            // get the queue containing the discovery results
            final ServicesQueue servicesQueue = discover(discoveryInstance1, all, 1000, MILLISECONDS);

            // now start the consumer of the ServiceURLs
            Thread consumer = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // take a serviceURL immediately - should not be available for 5 seconds
                        logger.info("Calling takeService()");
                        ServiceURL serviceURL = servicesQueue.takeService();
                        Assert.assertNull("ServiceURL should be null due to timeout on method!", serviceURL);

                        // take a second serviceURL after 5 seconds - should be available
                        try {
                            logger.info("Sleeping to allow ServiceURLs to be generated...");
                            Thread.sleep(PROVIDER_URL_GENERATION_DELAY);
                        } catch (InterruptedException ie) {
                            // noop
                        }
                        logger.info("Calling takeService()");
                        serviceURL = servicesQueue.takeService();
                        Assert.assertNotNull("ServiceURL should be non-null due to delay!", serviceURL);

                        // now drain the queue, we hae done our test
                        while ((serviceURL = servicesQueue.takeService(Long.MAX_VALUE, MILLISECONDS)) != null) {
                            logger.info("while draining, found match: " + serviceURL.toString());
                        }
                    } catch(InterruptedException ie) {
                        Assert.fail("Discovery was interrupted ...");
                    }
                }
            });
            consumer.start();
            consumer.join();

        } catch(InterruptedException ie)  {
            logger.info("consumer thread was interrupted!");
        }
    }

    /**
     * Test accessing the queue when discovery instance is initialised with a timeout.
     * Tests the case where:
     * - takeService(t, tu) is called before any ServiceURLs are ready, waits long enough and returns a serviceURL
     * - takeService(t, tu) is called when a ServiceURL is ready, does not time out and returns a value
     */
    @Test
    public void testDiscoveryWithMethodTimeout() {

        FilterSpec cluster = FilterSpec.equal("cluster","c");
        FilterSpec module = FilterSpec.equal("module","m");
        FilterSpec all = FilterSpec.any(cluster,module);

        // call discovery for single attribute
        logger.info("Calling discover for filterspec " + all);
        try {
            // get the queue containing the discovery results
            final ServicesQueue servicesQueue = discover(discoveryInstance2, all);

            // now start the consumer of the ServiceURLs
            Thread consumer = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // take a serviceURL immediately - should not be available for 5 seconds
                        logger.info("Calling takeService(t, tu)");
                        ServiceURL serviceURL = servicesQueue.takeService(PROVIDER_URL_GENERATION_DELAY, MILLISECONDS);
                        Assert.assertNotNull("ServiceURL should be non-null due to timeout on method!", serviceURL);

                        // take a second serviceURL after 5 seconds - should be available
                        try {
                            logger.info("Sleeping to allow ServiceURLs to be generated...");
                            Thread.sleep(PROVIDER_URL_GENERATION_DELAY);
                        } catch (InterruptedException ie) {
                            // noop
                        }
                        logger.info("Calling takeService(t, tu)");
                        serviceURL = servicesQueue.takeService(PROVIDER_URL_GENERATION_DELAY, MILLISECONDS);
                        Assert.assertNotNull("ServiceURL should be non-null due to timeout on method!", serviceURL);

                        // now drain the queue, we hae done our test
                        while ((serviceURL = servicesQueue.takeService(Long.MAX_VALUE, MILLISECONDS)) != null) {
                            logger.info("while draining, found match: " + serviceURL.toString());
                        }
                    } catch(InterruptedException ie) {
                        Assert.fail("Discovery was interrupted ...");
                    }
                }
            });
            consumer.start();
            consumer.join();

        } catch(InterruptedException ie)  {
            logger.info("consumer thread was interrupted!");
        }
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
        discoveryInstance1 = null;
        discoveryInstance2 = null;
    }

    /**
     * Returns a queue of registered ServiceURLs which match the filter spec
     * @param filterSpec a condition on attributes to match
     * @return
     */
    private static ServicesQueue discover(Discovery discovery, FilterSpec filterSpec) {
        ServiceType serviceType = new ServiceType("ejb","jboss", null, null);
        return discovery.discover(serviceType, filterSpec);
    }

    /**
     * Returns a queue of registered ServiceURLs which match the filter spec
     * @param filterSpec a condition on attributes to match
     * @param time a timeout specifying how long to wait for a result to be avaiulable
     * @param timeUnit the units for parameter time
     * @return
     */
    private static ServicesQueue discover(Discovery discovery, FilterSpec filterSpec, long time, TimeUnit timeUnit) {
        ServiceType serviceType = new ServiceType("ejb","jboss", null, null);
        return discovery.discover(serviceType, filterSpec, time, timeUnit);
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

    /*
     * A StaticDiscoveryProvider which introduces a delay when returning ServiceURLs
     */
    private static final class DelayedDiscoveryProvider implements DiscoveryProvider {

        private final List<ServiceURL> services;
        // delay in ms
        private int delay;

        public DelayedDiscoveryProvider(List<ServiceURL> services) {
            this(services, 0);
        }

        public DelayedDiscoveryProvider(List<ServiceURL> services, int delay) {
            this.services = services;
            this.delay = delay;
        }

        @Override
        public DiscoveryRequest discover(ServiceType serviceType, FilterSpec filterSpec, DiscoveryResult result) {
            try {
                // set up a new thread to execute ServiceURL generation before returning the cancellation handle to the consumer
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (ServiceURL service : services) {
                                if (serviceType.implies(service) && (filterSpec == null || service.satisfies(filterSpec))) {
                                    // introduce delay in populating the queue with ServiceURLs
                                    logger.info("populating queue with matches with delay " + delay + " ms");
                                    try {
                                        Thread.sleep(DelayedDiscoveryProvider.this.delay);
                                    } catch (InterruptedException ie) {
                                        // noop
                                    }
                                    logger.info("adding match to queue: " + service.toString());
                                    result.addMatch(service);
                                }
                            }
                        } finally {
                            logger.info("queue matches generated, calling complete()");
                            result.complete();
                        }
                    }
                }).start();
                return DiscoveryRequest.NULL;
            } finally {
                // noop
            }
        }
    }
}
