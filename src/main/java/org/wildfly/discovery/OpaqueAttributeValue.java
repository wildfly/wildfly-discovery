/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
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
final class OpaqueAttributeValue extends AttributeValue {

    private static final long serialVersionUID = - 4002474273921497373L;

    private final byte[] content;
    private transient int hashCode;

    OpaqueAttributeValue(final byte[] content, final boolean clone) {
        this.content = clone ? content.clone() : content;
    }

    int getKind() {
        return K_OPAQUE;
    }

    public boolean isOpaque() {
        return true;
    }

    public int compareTo(final AttributeValue other) {
        if (other instanceof OpaqueAttributeValue) {
            return compareArrays(content, ((OpaqueAttributeValue) other).content);
        } else {
            return super.compareTo(other);
        }
    }

    public boolean equals(final Object obj) {
        return obj instanceof OpaqueAttributeValue && equals((OpaqueAttributeValue) obj);
    }

    public boolean equals(final AttributeValue obj) {
        return obj instanceof OpaqueAttributeValue && equals((OpaqueAttributeValue) obj);
    }

    public boolean equals(final OpaqueAttributeValue obj) {
        return obj == this || obj != null && hashCode() == obj.hashCode() && Arrays.equals(content, obj.content);
    }

    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = Arrays.hashCode(this.content);
            if (hashCode == 0) {
                hashCode = 1;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    private static int compareArrays(final byte[] a1, final byte[] a2) {
        final int l1 = a1.length;
        final int l2 = a2.length;
        final int minLen = min(l1, l2);
        int res;
        for (int i = 0; i < minLen; i ++) {
            res = signum((a1[i] & 0xff) - (a2[i] & 0xff));
            if (res != 0) return res;
        }
        return signum(l1 - l2);
    }

    String generateToString() {
        final StringBuilder builder = new StringBuilder(content.length * 2);
        for (final byte b : content) {
            int l = b & 0x0f;
            int h = (b & 0xf0) >> 4;
            builder.append('\\');
            if (h < 10) {
                builder.append('0' + h);
            } else {
                builder.append('A' + h - 10);
            }
            if (l < 10) {
                builder.append('0' + l);
            } else {
                builder.append('A' + l - 10);
            }
        }
        return builder.toString();
    }
}
