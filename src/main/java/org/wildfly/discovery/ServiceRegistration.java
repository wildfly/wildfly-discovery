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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

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
     * Invalidate this registration immediately (possibly temporarily).
     */
    void deactivate();

    /**
     * Re-activate this registration immediately and indefinitely.
     */
    void activate();

    /**
     * Activate this registration for the given duration.
     *
     * @param time the time amount
     * @param unit the time unit (must not be {@code null})
     */
    void activateFor(long time, TimeUnit unit);

    /**
     * Activate this registration for the given duration.
     *
     * @param duration the duration (must not be {@code null})
     */
    default void activateFor(Duration duration) {
        Assert.checkNotNullParam("duration", duration);
        if (duration.isNegative() || duration.isZero()) {
            return;
        }
        if (duration.getSeconds() > 1_000_000) {
            activateFor(duration.getSeconds(), TimeUnit.SECONDS);
        } else {
            activateFor(duration.toNanos(), TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Activate this registration until the given deadline.
     *
     * @param instant the deadline (must not be {@code null})
     */
    default void activateUntil(Instant instant) {
        activateFor(Duration.between(Instant.now(), instant));
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

        public void activateFor(final long time, final TimeUnit unit) {
        }

        public String toString() {
            return "Empty service registration handle";
        }
    };
}
