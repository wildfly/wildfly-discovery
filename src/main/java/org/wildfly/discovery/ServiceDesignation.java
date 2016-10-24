/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
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

import java.io.Serializable;

/**
 * A service designation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class ServiceDesignation implements Serializable {

    private static final long serialVersionUID = -9128124037606385859L;

    /*

      service:<abstract-type>[.<name-auth>]
      service:<abstract-type>[.<name-auth>]:<url-scheme>[.<name-auth>]
      service:<url-scheme>[.<name-auth>]://[[<user> @] <host> [: <port>] [/ <path>]* [; attr]*
      service:<abstract-type>[.<name-auth>]:<url-scheme>[.<name-auth>]://[[<user> @] <host> [: <port>] [/ <path>]* [; attr]*


     */

    ServiceDesignation() {
    }

    /**
     * Get the abstract type, if any.
     *
     * @return the abstract type, or {@code null} if no abstract type is set (subclasses may restrict this)
     */
    public abstract String getAbstractType();

    /**
     * Get the abstract type authority of this service type.  Abstract types with no authority will return {@code null}
     * for this value.
     *
     * @return the abstract type authority of this service type (may be {@code null})
     */
    public abstract String getAbstractTypeAuthority();

    /**
     * Get the concrete type name of this service type, if any.
     *
     * @return the concrete service type, or {@code null} if no abstract type is set (subclasses may restrict this)
     */
    public abstract String getUriScheme();

    /**
     * Get the concrete type authority name of this service type, if any.
     *
     * @return the concrete type authority name (may be {@code null})
     */
    public abstract String getUriSchemeAuthority();

    /**
     * Determine whether the given service designation is implied by this service designation.
     *
     * @param other the other service designation
     * @return {@code true} of the other service designation is implied by this one, {@code false} otherwise
     */
    public abstract boolean implies(ServiceDesignation other);

    /**
     * Determine if this service designation is equal to another.
     *
     * @param other the other service designation
     * @return {@code true} if the service types are equal, {@code false} otherwise
     */
    public abstract boolean equals(Object other);

    /**
     * Determine if this service designation is equal to another.
     *
     * @param other the other service designation
     * @return {@code true} if the service types are equal, {@code false} otherwise
     */
    public abstract boolean equals(ServiceDesignation other);

    /**
     * Get the hash code of this designation.
     *
     * @return the hash code of this designation
     */
    public abstract int hashCode();

    /**
     * Get the simple string representation of this designation.
     *
     * @return the simple string representation of this designation (not {@code null})
     */
    public abstract String toString();
}
