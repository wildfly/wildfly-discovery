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

import java.time.Instant;

import org.wildfly.common.Assert;

/**
 * A service registration handle, which can be used to remove a previously registered service.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServiceRegistration extends AutoCloseable {

    /**
     * Close and remove this registration immediately.
     */
    void close();

    /**
     * Invalidate this registration immediately (possibly temporarily) without closing it.
     */
    void deactivate();

    /**
     * Re-activate this registration immediately and indefinitely.
     */
    void activate();

    /**
     * Hint to this registration that deactivation of the service is likely at the given time.
     *
     * @param instant the time at which deactivation is likely (must not be {@code null})
     */
    default void hintDeactivateAt(Instant instant) {
        Assert.checkNotNullParam("instant", instant);
    }

    /**
     * Create an aggregate of registrations which are all controlled as one.
     *
     * @param registrations the registrations to control
     * @return the aggregated handle
     */
    static ServiceRegistration aggregate(ServiceRegistration... registrations) {
        return new AggregateServiceRegistration(registrations);
    }

    /**
     * An empty service registration handle, which has no effect.
     */
    ServiceRegistration EMPTY = new ServiceRegistration() {
        public void close() {
        }

        public void deactivate() {
        }

        public void activate() {
        }

        public String toString() {
            return "Empty service registration handle";
        }
    };
}
