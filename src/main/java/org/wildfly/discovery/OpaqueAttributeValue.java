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

import static java.lang.Integer.signum;
import static java.lang.Math.min;

import java.util.Arrays;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class OpaqueAttributeValue extends AttributeValue implements Comparable<OpaqueAttributeValue> {

    private static final long serialVersionUID = 5294245642010483604L;

    private final byte[] bytes;
    private transient int hashCode;
    private transient String toString;

    OpaqueAttributeValue(final byte[] bytes, final int offs, final int len) {
        this.bytes = Arrays.copyOfRange(bytes, offs, offs + len);
    }

    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = Arrays.hashCode(bytes);
            if (hashCode == 0) {
                hashCode = 0xee7722ff;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    public boolean equals(final Object other) {
        return other instanceof OpaqueAttributeValue && equals((OpaqueAttributeValue) other);
    }

    public boolean equals(final AttributeValue other) {
        return other instanceof OpaqueAttributeValue && equals((OpaqueAttributeValue) other);
    }

    public boolean equals(final OpaqueAttributeValue other) {
        return other != null && hashCode() == other.hashCode() && Arrays.equals(bytes, other.bytes);
    }

    public String toString() {
        String toString = this.toString;
        if (toString == null) {
            final StringBuilder sb = new StringBuilder(bytes.length * 3 + 3);
            sb.append("\\FF");
            for (final byte b : bytes) {
                int l = b & 0x0f;
                int h = (b & 0xf0) >> 4;
                sb.append('\\');
                if (h < 10) {
                    sb.append('0' + h);
                } else {
                    sb.append('A' + h - 10);
                }
                if (l < 10) {
                    sb.append('0' + l);
                } else {
                    sb.append('A' + l - 10);
                }
            }
            toString = this.toString = sb.toString();
        }
        return toString;
    }

    public int compareTo(final AttributeValue other) {
        if (! (other instanceof OpaqueAttributeValue)) throw new IllegalArgumentException("Not comparable");
        return compareTo((OpaqueAttributeValue) other);
    }

    void escapeTo(final StringBuilder builder) {
        throw new IllegalStateException("Implement me");

    }

    public int compareTo(final OpaqueAttributeValue o) {
        final byte[] bytes = this.bytes;
        final byte[] oBytes = o.bytes;
        final int bytesLength = bytes.length;
        final int oBytesLength = oBytes.length;
        final int l = min(bytesLength, oBytesLength);
        int s;
        for (int i = 0; i < l; i ++) {
            s = signum((bytes[i] & 0xff) - (oBytes[i] & 0xff));
            if (s != 0) {
                return s;
            }
        }
        return signum(bytesLength - oBytesLength);
    }
}
