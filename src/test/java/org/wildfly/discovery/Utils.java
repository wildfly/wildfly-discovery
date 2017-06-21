package org.wildfly.discovery;

import org.wildfly.common.Assert;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by nrla on 16/06/17.
 */
public class Utils {

    public Utils() {
    }

    public static List<ServiceURL> drainServicesQueue(ServicesQueue discoveryCallServicesQueue) throws InterruptedException {
        List<ServiceURL> results = new ArrayList<ServiceURL>();
        drainServicesQueue(discoveryCallServicesQueue, results);
        return results;
    }

    public static void drainServicesQueue(ServicesQueue servicesQueue,List<ServiceURL> serviceURLs) throws InterruptedException {
        Assert.checkNotNullParam("servicesQueue", servicesQueue);
        Assert.checkNotNullParam("servicesURLs", serviceURLs);

        try {
            ServiceURL serviceURL = servicesQueue.takeService();
            do {
                serviceURLs.add(serviceURL);
                serviceURL = servicesQueue.takeService();
            } while (serviceURL != null);
        } catch(InterruptedException ie) {
            throw ie;
        }
    }

    /**
     * Builds ServiceURLs with constant default type and varying attributes.
     *
     * @param abstractType the abstract service type (must not be (@code null))
     * @param abstractTypeAuth the abstract service type authority (must not be (@code null))
     * @param pairs one or more attribute pairs to be set in the ServiceURL (must not be (@code null))
     *
     * @return a configured ServiceURL
     * @throws Exception
     */
    public static ServiceURL buildServiceURL(String abstractType, String abstractTypeAuth, URI uri, AttributeValuePair...pairs) throws Exception {
        Assert.checkNotNullParam("abstractType", abstractType);
        Assert.checkNotNullParam("abstractTypeAuth", abstractTypeAuth);
        Assert.checkNotNullParam("uri", uri);

        final ServiceURL.Builder builder = new ServiceURL.Builder();
        // set the locationURI
        builder.setUri(uri);
        builder.setAbstractType(abstractType);
        builder.setAbstractTypeAuthority(abstractTypeAuth);
        // add an attribute
        for (AttributeValuePair pair : pairs) {
            builder.addAttribute(pair.getAttribute(), AttributeValue.fromString(pair.getValue()));
        }
        return builder.create();
    }

    /**
     * An attribute value pair
     */
    public static class AttributeValuePair {
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

}
