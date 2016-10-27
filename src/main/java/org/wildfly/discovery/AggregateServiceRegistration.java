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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

import org.wildfly.common.Assert;

/**
 * An aggregate service registration.
 *
 * @see ServiceRegistration#aggregate(ServiceRegistration...)
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class AggregateServiceRegistration implements ServiceRegistration {
    private final ServiceRegistration[] registrations;

    /**
     * Construct a new instance.
     *
     * @param registrations the registrations (must not be {@code null})
     */
    protected AggregateServiceRegistration(final ServiceRegistration... registrations) {
        Assert.checkNotNullParam("registrations", registrations);
        this.registrations = registrations;
    }

    public void close() {
        for (ServiceRegistration registration : registrations) {
            if (registration != null) registration.close();
        }
    }

    public void deactivate() {
        for (ServiceRegistration registration : registrations) {
            if (registration != null) registration.deactivate();
        }
    }

    public void activate() {
        for (ServiceRegistration registration : registrations) {
            if (registration != null) registration.activate();
        }
    }

    private TemporalUnit convertUnit(TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:
                return ChronoUnit.NANOS;
            case MICROSECONDS:
                return ChronoUnit.MICROS;
            case MILLISECONDS:
                return ChronoUnit.MILLIS;
            case SECONDS:
                return ChronoUnit.SECONDS;
            case MINUTES:
                return ChronoUnit.MINUTES;
            case HOURS:
                return ChronoUnit.HOURS;
            case DAYS:
                return ChronoUnit.DAYS;
            // last ditch guess
            default:
                return ChronoUnit.valueOf(unit.toString());
        }
    }

    public void activateFor(final long time, final TimeUnit unit) {
        // resist clock skew
        activateUntil(Instant.now().plus(time, convertUnit(unit)));
    }

    public void activateFor(final Duration duration) {
        // resist clock skew
        activateUntil(Instant.now().plus(duration));
    }

    public void activateUntil(final Instant instant) {
        for (ServiceRegistration registration : registrations) {
            if (registration != null) registration.activateUntil(instant);
        }
    }
}
