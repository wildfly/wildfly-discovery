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

import static javax.xml.stream.XMLStreamConstants.COMMENT;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.PROCESSING_INSTRUCTION;
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
import org.wildfly.discovery.impl.AggregateDiscoveryProvider;
import org.wildfly.discovery.impl.AggregateRegistryProvider;
import org.wildfly.discovery.impl.LocalRegistryAndDiscoveryProvider;
import org.wildfly.discovery.impl.StaticDiscoveryProvider;
import org.wildfly.discovery.spi.DiscoveryProvider;
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
        final ClientConfiguration clientConfiguration = ClientConfiguration.getInstance();
        if (clientConfiguration != null) try (final ConfigurationXMLStreamReader streamReader = clientConfiguration.readConfiguration(Collections.singleton(NS_DISCOVERY_1_0))) {
            return parseConfiguration(streamReader);
        } catch (ConfigXMLParseException e) {
            throw new InvalidDiscoveryConfigurationException(e);
        }
        return new ConfiguredProvider(DiscoveryProvider.EMPTY, RegistryProvider.EMPTY);
    }

    private static ConfiguredProvider parseConfiguration(final ConfigurationXMLStreamReader reader) throws ConfigXMLParseException {
        if (reader.hasNext()) {
            switch (reader.nextTag()) {
                case START_ELEMENT: {
                    checkNamespace(reader);
                    switch (reader.getLocalName()) {
                        case "discovery": {
                            ConfiguredProvider configuredProvider = parseDiscoveryElement(reader);
                            expectDocumentEnd(reader, configuredProvider);
                            return configuredProvider;
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
        return new ConfiguredProvider(DiscoveryProvider.EMPTY, RegistryProvider.EMPTY);
    }

    private static void expectDocumentEnd(final ConfigurationXMLStreamReader reader, final ConfiguredProvider configuredProvider) throws ConfigXMLParseException {
        while (reader.hasNext()) {
            switch (reader.next()) {
                case COMMENT:
                case PROCESSING_INSTRUCTION: {
                    break;
                }
                case END_DOCUMENT: {
                    return;
                }
                case START_ELEMENT:
                case END_ELEMENT: {
                    throw reader.unexpectedElement();
                }
                default: {
                    if (reader.isWhiteSpace()) break;
                    throw reader.unexpectedContent();
                }
            }
        }
        return;
    }

    private static ConfiguredProvider parseDiscoveryElement(final ConfigurationXMLStreamReader reader) throws ConfigXMLParseException {
        DiscoveryProvider discoveryProvider = DiscoveryProvider.EMPTY;
        RegistryProvider registryProvider = RegistryProvider.EMPTY;
        LocalRegistryAndDiscoveryProvider localRegistry = new LocalRegistryAndDiscoveryProvider();
        requireNoAttributes(reader);
        out: while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case START_ELEMENT: {
                    checkNamespace(reader);
                    switch (reader.getLocalName()) {
                        case "discovery-provider": {
                            if (discoveryProvider != DiscoveryProvider.EMPTY) {
                                throw reader.unexpectedElement();
                            }
                            discoveryProvider = parseDiscoveryProvider(reader, localRegistry);
                            break;
                        }
                        case "registry-provider": {
                            if (registryProvider != RegistryProvider.EMPTY) {
                                throw reader.unexpectedElement();
                            }
                            registryProvider = parseRegistryProvider(reader, localRegistry);
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
        return new ConfiguredProvider(discoveryProvider, registryProvider);
    }

    private static RegistryProvider parseRegistryProvider(final ConfigurationXMLStreamReader reader, final LocalRegistryAndDiscoveryProvider localRegistry) throws ConfigXMLParseException {
        requireNoAttributes(reader);
        RegistryProvider registryProvider;
        switch (reader.nextTag()) {
            case START_ELEMENT: {
                checkNamespace(reader);
                switch (reader.getLocalName()) {
                    case "local-registry": {
                        registryProvider = localRegistry;
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
        switch (reader.nextTag()) {
            case START_ELEMENT: {
                checkNamespace(reader);
                switch (reader.getLocalName()) {
                    case "local-registry": {
                        discoveryProvider = localRegistry;
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
            switch (reader.nextTag()) {
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
            switch (reader.nextTag()) {
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
            switch (reader.nextTag()) {
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

    private static void requireNoAttributes(final ConfigurationXMLStreamReader reader) {
        if (reader.getAttributeCount() > 0) {
            reader.unexpectedAttribute(0);
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
                    uri = reader.getURIAttributeValue(i);
                    break;
                }
                case "uri-scheme-authority": {
                    uriSchemeAuthority = reader.getAttributeValue(i);
                    break;
                }
                case "abstract-type": {
                    abstractType = reader.getAttributeValue(i);
                    break;
                }
                case "abstract-type-authority": {
                    abstractTypeAuthority = reader.getAttributeValue(i);
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
            switch (reader.nextTag()) {
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
                    name = reader.getAttributeValue(i);
                    break;
                }
                case "value": {
                    value = AttributeValue.fromString(reader.getAttributeValue(i));
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
                    className = reader.getAttributeValue(i);
                    break;
                }
                case "module": {
                    moduleName = reader.getAttributeValue(i);
                    break;
                }
                default: {
                    throw reader.unexpectedAttribute(i);
                }
            }
        }
        ClassLoader searchLoader = DiscoveryXmlParser.class.getClassLoader();
        T item;
        if (moduleName != null) {
            if (className != null) try {
                item = Module.loadClassFromCallerModuleLoader(moduleName, className).asSubclass(type).newInstance();
            } catch (ModuleLoadException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new InvalidDiscoveryConfigurationException(e);
            } else try {
                final ServiceLoader<T> loader = Module.loadServiceFromCallerModuleLoader(moduleName, type);
                final Iterator<T> iterator = loader.iterator();
                try {
                    if (! iterator.hasNext()) {
                        throw new InvalidDiscoveryConfigurationException("No provider found in module " + moduleName);
                    }
                    item = iterator.next();
                } catch (ServiceConfigurationError e) {
                    throw new InvalidDiscoveryConfigurationException(e);
                }
            } catch (ModuleLoadException e) {
                throw new InvalidDiscoveryConfigurationException(e);
            }
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

    private static void expectEnd(final ConfigurationXMLStreamReader reader) throws ConfigXMLParseException {
        if (reader.nextTag() != END_ELEMENT) {
            throw reader.unexpectedElement();
        }
    }

}