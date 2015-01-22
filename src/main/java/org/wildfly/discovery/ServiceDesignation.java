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
     * Determine whether the given service designation is implied by this service designation.
     *
     * @param other the other service designation
     * @return {@code true} of the other service designation is implied by this one, {@code false} otherwise
     */
    public abstract boolean implies(ServiceDesignation other);

    public abstract boolean equals(Object other);

    public abstract boolean equals(ServiceDesignation other);

    public abstract int hashCode();

    public abstract String toString();
}
