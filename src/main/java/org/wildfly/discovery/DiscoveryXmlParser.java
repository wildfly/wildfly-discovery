/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
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

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.wildfly.client.config.ClientConfiguration;
import org.wildfly.client.config.ConfigXMLParseException;
import org.wildfly.client.config.ConfigurationXMLStreamReader;
import org.wildfly.common.Assert;
import org.wildfly.discovery.impl.AggregateDiscoveryProvider;
import org.wildfly.discovery.impl.AggregateRegistryProvider;
import org.wildfly.discovery.impl.LocalRegistryAndDiscoveryProvider;
import org.wildfly.discovery.impl.StaticDiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryProvider;
import org.wildfly.discovery.spi.ExternalDiscoveryConfigurator;
import org.wildfly.discovery.spi.RegistryProvider;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class DiscoveryXmlParser {

    private static final DiscoveryProvider[] NO_DISCOVERY_PROVIDERS = new DiscoveryProvider[0];
    private static final RegistryProvider[] NO_REGISTRY_PROVIDERS = new RegistryProvider[0];

    private DiscoveryXmlParser() {
    }

    static final String NS_DISCOVERY_1_0 = "urn:wildfly-discovery:1.0";

    static ConfiguredProvider getConfiguredProvider() {
        List<DiscoveryProvider> discoveryProviders = new ArrayList<>();
        List<RegistryProvider> registryProviders = new ArrayList<>();
        final ClientConfiguration clientConfiguration = ClientConfiguration.getInstance();
        if (clientConfiguration != null) try (final ConfigurationXMLStreamReader streamReader = clientConfiguration.readConfiguration(Collections.singleton(NS_DISCOVERY_1_0))) {
            parseConfiguration(streamReader, discoveryProviders, registryProviders);
        } catch (ConfigXMLParseException e) {
            throw new InvalidDiscoveryConfigurationException(e);
        }
        ServiceLoader<ExternalDiscoveryConfigurator> loader = ServiceLoader.load(ExternalDiscoveryConfigurator.class);
        final Iterator<ExternalDiscoveryConfigurator> iterator = loader.iterator();
        for (;;) try {
            if (! iterator.hasNext()) break;
            final ExternalDiscoveryConfigurator configurator = iterator.next();
            configurator.configure(
                provider -> discoveryProviders.add(Assert.checkNotNullParam("provider", provider)),
                provider -> registryProviders.add(Assert.checkNotNullParam("provider", provider))
            );
        } catch (ServiceConfigurationError | RuntimeException e) {
            // TODO log & continue
        }

        final DiscoveryProvider discoveryProvider;
        if (discoveryProviders.isEmpty()) {
            discoveryProvider = DiscoveryProvider.EMPTY;
        } else if (discoveryProviders.size() == 1) {
            discoveryProvider = discoveryProviders.get(0);
        } else {
            discoveryProvider = new AggregateDiscoveryProvider(discoveryProviders.toArray(NO_DISCOVERY_PROVIDERS));
        }
        final RegistryProvider registryProvider;
        if (registryProviders.isEmpty()) {
            registryProvider = RegistryProvider.EMPTY;
        } else if (registryProviders.size() == 1) {
            registryProvider = registryProviders.get(0);
        } else {
            registryProvider = new AggregateRegistryProvider(registryProviders.toArray(NO_REGISTRY_PROVIDERS));
        }
        return new ConfiguredProvider(discoveryProvider, registryProvider);
    }

    private static void parseConfiguration(final ConfigurationXMLStreamReader reader, final List<DiscoveryProvider> discoveryProviders, final List<RegistryProvider> registryProviders) throws ConfigXMLParseException {
        if (reader.hasNext()) {
            final int tag = reader.nextTag();
            switch (tag) {
                case START_ELEMENT: {
                    checkNamespace(reader);
                    switch (reader.getLocalName()) {
                        case "discovery": {
                            parseDiscoveryElement(reader, discoveryProviders, registryProviders);
                            return;
                        }
                        default: {
                            throw reader.unexpectedElement();
                        }
                    }
                }
                default: {
                    throw reader.unexpectedContent();
                }
            }
        }
    }

    private static void parseDiscoveryElement(final ConfigurationXMLStreamReader reader, final List<DiscoveryProvider> discoveryProviders, final List<RegistryProvider> registryProviders) throws ConfigXMLParseException {
        DiscoveryProvider discoveryProvider = DiscoveryProvider.EMPTY;
        RegistryProvider registryProvider = RegistryProvider.EMPTY;
        LocalRegistryAndDiscoveryProvider localRegistry = new LocalRegistryAndDiscoveryProvider();
        requireNoAttributes(reader);
        out: while (reader.hasNext()) {
            int tag = reader.nextTag();
            switch (tag) {
                case START_ELEMENT: {
                    checkNamespace(reader);
                    switch (reader.getLocalName()) {
                        case "discovery-provider": {
                            if (discoveryProvider != DiscoveryProvider.EMPTY) {
                                throw reader.unexpectedElement();
                            }
                            discoveryProviders.add(parseDiscoveryProvider(reader, localRegistry));
                            break;
                        }
                        case "registry-provider": {
                            if (registryProvider != RegistryProvider.EMPTY) {
                                throw reader.unexpectedElement();
                            }
                            registryProviders.add(parseRegistryProvider(reader, localRegistry));
                            break;
                        }
                        default: {
                            throw reader.unexpectedElement();
                        }
                    }
                    break;
                }
                case END_ELEMENT: {
                    break out;
                }
            }
        }
    }

    private static RegistryProvider parseRegistryProvider(final ConfigurationXMLStreamReader reader, final LocalRegistryAndDiscoveryProvider localRegistry) throws ConfigXMLParseException {
        requireNoAttributes(reader);
        RegistryProvider registryProvider;
        final int tag = reader.nextTag();
        switch (tag) {
            case START_ELEMENT: {
                checkNamespace(reader);
                switch (reader.getLocalName()) {
                    case "local-registry": {
                        registryProvider = localRegistry;
                        expectEnd(reader);
                        break;
                    }
                    case "aggregate": {
                        registryProvider = parseAggregateRegistry(reader, localRegistry);
                        break;
                    }
                    case "custom": {
                        registryProvider = parseCustom(reader, RegistryProvider.class);
                        break;
                    }
                    default: {
                        throw reader.unexpectedElement();
                    }
                }
                expectEnd(reader);
                return registryProvider;
            }
            default: {
                throw reader.unexpectedElement();
            }
        }
    }

    private static DiscoveryProvider parseDiscoveryProvider(final ConfigurationXMLStreamReader reader, final LocalRegistryAndDiscoveryProvider localRegistry) throws ConfigXMLParseException {
        requireNoAttributes(reader);
        DiscoveryProvider discoveryProvider;
        final int tag = reader.nextTag();
        switch (tag) {
            case START_ELEMENT: {
                checkNamespace(reader);
                switch (reader.getLocalName()) {
                    case "local-registry": {
                        discoveryProvider = localRegistry;
                        expectEnd(reader);
                        break;
                    }
                    case "static": {
                        discoveryProvider = parseStatic(reader);
                        break;
                    }
                    case "aggregate": {
                        discoveryProvider = parseAggregateDiscovery(reader, localRegistry);
                        break;
                    }
                    case "custom": {
                        discoveryProvider = parseCustom(reader, DiscoveryProvider.class);
                        break;
                    }
                    default: {
                        throw reader.unexpectedElement();
                    }
                }
                expectEnd(reader);
                return discoveryProvider;
            }
        }
        throw reader.missingRequiredElement(NS_DISCOVERY_1_0, "local-registry/static/aggregate/custom");
    }

    private static DiscoveryProvider parseAggregateDiscovery(final ConfigurationXMLStreamReader reader, final LocalRegistryAndDiscoveryProvider localRegistry) throws ConfigXMLParseException {
        requireNoAttributes(reader);
        final List<DiscoveryProvider> delegates = new ArrayList<>();
        out: while (reader.hasNext()) {
            final int tag = reader.nextTag();
            switch (tag) {
                case START_ELEMENT: {
                    checkNamespace(reader);
                    switch (reader.getLocalName()) {
                        case "discovery-provider": {
                            delegates.add(parseDiscoveryProvider(reader, localRegistry));
                            break;
                        }
                        default: {
                            throw reader.unexpectedElement();
                        }
                    }
                }
                case END_ELEMENT: {
                    break out;
                }
            }
        }
        return new AggregateDiscoveryProvider(delegates.toArray(NO_DISCOVERY_PROVIDERS));
    }

    private static RegistryProvider parseAggregateRegistry(final ConfigurationXMLStreamReader reader, final LocalRegistryAndDiscoveryProvider localRegistry) throws ConfigXMLParseException {
        requireNoAttributes(reader);
        final List<RegistryProvider> delegates = new ArrayList<>();
        out: while (reader.hasNext()) {
            final int tag = reader.nextTag();
            switch (tag) {
                case START_ELEMENT: {
                    checkNamespace(reader);
                    switch (reader.getLocalName()) {
                        case "registry-provider": {
                            delegates.add(parseRegistryProvider(reader, localRegistry));
                            break;
                        }
                        default: {
                            throw reader.unexpectedElement();
                        }
                    }
                }
                case END_ELEMENT: {
                    break out;
                }
            }
        }
        return new AggregateRegistryProvider(delegates.toArray(NO_REGISTRY_PROVIDERS));
    }

    private static DiscoveryProvider parseStatic(final ConfigurationXMLStreamReader reader) throws ConfigXMLParseException {
        List<ServiceURL> serviceURLs = new ArrayList<>();
        requireNoAttributes(reader);
        out: while (reader.hasNext()) {
            final int tag = reader.nextTag();
            switch (tag) {
                case START_ELEMENT: {
                    checkNamespace(reader);
                    switch (reader.getLocalName()) {
                        case "service": {
                            serviceURLs.add(parseService(reader));
                            break;
                        }
                        default: {
                            throw reader.unexpectedElement();
                        }
                    }
                }
                case END_ELEMENT: {
                    break out;
                }
            }
        }
        return new StaticDiscoveryProvider(serviceURLs);
    }

    private static void requireNoAttributes(final ConfigurationXMLStreamReader reader) throws ConfigXMLParseException {
        if (reader.getAttributeCount() > 0) {
            throw reader.unexpectedAttribute(0);
        }
    }

    private static ServiceURL parseService(final ConfigurationXMLStreamReader reader) throws ConfigXMLParseException {
        final ServiceURL.Builder builder = new ServiceURL.Builder();
        URI uri = null;
        String uriSchemeAuthority = null;
        String abstractType = null;
        String abstractTypeAuthority = null;
        int cnt = reader.getAttributeCount();
        for (int i = 0; i < cnt; i ++) {
            checkAttributeNamespace(reader, i);
            switch (reader.getAttributeLocalName(i)) {
                case "uri": {
                    uri = reader.getURIAttributeValueResolved(i);
                    break;
                }
                case "uri-scheme-authority": {
                    uriSchemeAuthority = reader.getAttributeValueResolved(i);
                    break;
                }
                case "abstract-type": {
                    abstractType = reader.getAttributeValueResolved(i);
                    break;
                }
                case "abstract-type-authority": {
                    abstractTypeAuthority = reader.getAttributeValueResolved(i);
                    break;
                }
                default: {
                    throw reader.unexpectedAttribute(i);
                }
            }
        }
        if (uri == null) {
            throw reader.missingRequiredAttribute(null, "uri");
        }
        builder.setUri(uri);
        if (uriSchemeAuthority != null) builder.setUriSchemeAuthority(uriSchemeAuthority);
        if (abstractType != null) builder.setAbstractType(abstractType);
        if (abstractTypeAuthority != null) builder.setAbstractTypeAuthority(abstractTypeAuthority);
        out: while (reader.hasNext()) {
            final int tag = reader.nextTag();
            switch (tag) {
                case START_ELEMENT: {
                    checkNamespace(reader);
                    switch (reader.getLocalName()) {
                        case "attribute": {
                            parseAttribute(reader, builder);
                            break;
                        }
                        default: {
                            throw reader.unexpectedElement();
                        }
                    }
                    break;
                }
                case END_ELEMENT: {
                    break out;
                }
            }
        }
        return builder.create();
    }

    private static void checkAttributeNamespace(final ConfigurationXMLStreamReader reader, final int i) throws ConfigXMLParseException {
        if (reader.getAttributeNamespace(i) != null) {
            throw reader.unexpectedAttribute(i);
        }
    }

    private static void parseAttribute(final ConfigurationXMLStreamReader reader, final ServiceURL.Builder builder) throws ConfigXMLParseException {
        String name = null;
        AttributeValue value = null;
        int cnt = reader.getAttributeCount();
        for (int i = 0; i < cnt; i ++) {
            checkAttributeNamespace(reader, i);
            switch (reader.getAttributeLocalName(i)) {
                case "name": {
                    name = reader.getAttributeValueResolved(i);
                    break;
                }
                case "value": {
                    value = AttributeValue.fromString(reader.getAttributeValueResolved(i));
                    break;
                }
                default: {
                    throw reader.unexpectedAttribute(i);
                }
            }
        }
        if (name == null) {
            throw reader.missingRequiredAttribute(null, "name");
        }
        expectEnd(reader);
        if (value == null) {
            builder.addAttribute(name);
        } else {
            builder.addAttribute(name, value);
        }
    }

    private static void checkNamespace(final ConfigurationXMLStreamReader reader) throws ConfigXMLParseException {
        switch (reader.getNamespaceURI()) {
            case NS_DISCOVERY_1_0:
                break;
            default:
                throw reader.unexpectedElement();
        }
    }

    private static <T> T parseCustom(final ConfigurationXMLStreamReader reader, final Class<T> type) throws ConfigXMLParseException {
        String className = null;
        String moduleName = null;
        int cnt = reader.getAttributeCount();
        for (int i = 0; i < cnt; i ++) {
            checkAttributeNamespace(reader, i);
            switch (reader.getAttributeLocalName(i)) {
                case "class": {
                    className = reader.getAttributeValueResolved(i);
                    break;
                }
                case "module": {
                    moduleName = reader.getAttributeValueResolved(i);
                    break;
                }
                default: {
                    throw reader.unexpectedAttribute(i);
                }
            }
        }
        T item;
        if (moduleName != null) {
           item = ModuleLoadDelegate.loadService(moduleName, className, type);
        } else {
            if (className != null) try {
                item = Class.forName(className, true, DiscoveryXmlParser.class.getClassLoader()).asSubclass(type).newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new InvalidDiscoveryConfigurationException(e);
            }
            else {
                final ServiceLoader<T> loader = ServiceLoader.load(type);
                final Iterator<T> iterator = loader.iterator();
                try {
                    if (! iterator.hasNext()) {
                        throw new InvalidDiscoveryConfigurationException("No provider found");
                    }
                    item = iterator.next();
                } catch (ServiceConfigurationError e) {
                    throw new InvalidDiscoveryConfigurationException(e);
                }
            }
        }
        expectEnd(reader);
        return item;
    }

    static final class ModuleLoadDelegate {
        static <T> T loadService(String moduleName, String className, final Class<T> type) throws ConfigXMLParseException {
            if (className != null) {
                try {
                    Module.loadClassFromCallerModuleLoader(moduleName, className).asSubclass(type).newInstance();
                } catch (ModuleLoadException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    throw new InvalidDiscoveryConfigurationException(e);
                }
            } else {
                try {
                    final ServiceLoader<T> loader = Module.loadServiceFromCallerModuleLoader(moduleName, type);
                    final Iterator<T> iterator = loader.iterator();
                    try {
                        if (!iterator.hasNext()) {
                            throw new InvalidDiscoveryConfigurationException("No provider found in module " + moduleName);
                        }
                        return iterator.next();
                    } catch (ServiceConfigurationError e) {
                        throw new InvalidDiscoveryConfigurationException(e);
                    }
                } catch (ModuleLoadException e) {
                    throw new InvalidDiscoveryConfigurationException(e);
                }
            }
            return null;
        }
    }

    private static void expectEnd(final ConfigurationXMLStreamReader reader) throws ConfigXMLParseException {
        if (reader.nextTag() != END_ELEMENT) {
            throw reader.unexpectedElement();
        }
    }

}
