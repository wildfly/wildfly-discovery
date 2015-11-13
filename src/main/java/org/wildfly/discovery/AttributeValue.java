/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
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

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.wildfly.common.Assert;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AttributeValue implements Comparable<AttributeValue>, Serializable {
    // sorted in order of sorted appearance
    private static final int K_OPAQUE = 0;
    private static final int K_NUMERIC = 1;
    private static final int K_STRING = 2;
    private static final int K_BOOLEAN_TRUE = 3;
    private static final int K_BOOLEAN_FALSE = 4;

    private static final AttributeValue TRUE = new AttributeValue("true", K_BOOLEAN_TRUE);
    private static final AttributeValue FALSE = new AttributeValue("false", K_BOOLEAN_FALSE);

    private final byte[] content;
    private final int hashCode;
    private final int kind;
    private final int asInt;
    private String toString;

    private AttributeValue(final byte[] content, final String toString, final int kind) {
        this.content = content;
        hashCode = Arrays.hashCode(content);
        this.kind = kind;
        if (kind == K_NUMERIC) {
            asInt = Integer.parseInt(toString);
        } else {
            asInt = 0;
        }
        this.toString = toString;
    }
    
    private AttributeValue(final String string, final int kind) {
        this(string.getBytes(StandardCharsets.UTF_8), string, kind);
    }

    public static AttributeValue fromString(String string) {
        // short circuit checks
        switch (string) {
            case "true": return TRUE;
            case "false": return FALSE;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(string.length());
        int cp;
        boolean bs = false;
        boolean fd = false;
        int fb = 0;
        for (int i = 0; i < string.length(); i = string.offsetByCodePoints(i, 1)) {
            cp = string.codePointAt(i);
            if (bs) {
                if (cp == '\\') {
                    bs = false;
                    baos.write('\\');
                } else if (cp >= 0 && cp <= 9 || cp >= 'a' && cp <= 'f' || cp >= 'A' && cp <= 'F') {
                    int b = Character.digit(cp, 16);
                    if (fd) {
                        fd = false;
                        bs = false;
                        baos.write(fb << 4 | b);
                    } else {
                        fb = b;
                        fd = true;
                    }
                }
            } else {
                if (cp == '\\') {
                    bs = true;
                } else if (cp < 0x80) {
                    baos.write(cp);
                } else if (cp < 0x0800) {
                    baos.write(0xc0 | 0x1F & cp >> 6);
                    baos.write(0x80 | 0x3F & cp);
                } else if (cp < 0x10000) {
                    baos.write(0xE0 | 0x0F & cp >> 12);
                    baos.write(0x80 | 0x3F & cp >> 6);
                    baos.write(0x80 | 0x3F & cp);
                } else if (cp < 0x110000) {
                    baos.write(0xF0 | 0x07 & cp >> 18);
                    baos.write(0x80 | 0x3F & cp >> 12);
                    baos.write(0x80 | 0x3F & cp >> 6);
                    baos.write(0x80 | 0x3F & cp);
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
        // ignore hanging backslash
        return fromClonedBytes(baos.toByteArray());
    }

    public static AttributeValue fromBytes(byte[] bytes) {
        return fromClonedBytes(bytes.clone());
    }

    private static AttributeValue fromClonedBytes(final byte[] clone) {
        int kind = analyze(clone);
        if (kind == K_BOOLEAN_TRUE) return TRUE;
        if (kind == K_BOOLEAN_FALSE) return FALSE;
        if (kind == K_NUMERIC) return new AttributeValue(clone, new String(clone, StandardCharsets.US_ASCII), K_NUMERIC);
        return new AttributeValue(clone, null, kind);
    }

    private static final byte[] TRUE_BYTES = "true".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] FALSE_BYTES = "false".getBytes(StandardCharsets.US_ASCII);

    private static int analyze(final byte[] content) {
        if (Arrays.equals(content, TRUE_BYTES)) return K_BOOLEAN_TRUE;
        if (Arrays.equals(content, FALSE_BYTES)) return K_BOOLEAN_FALSE;
        if (content.length == 0) {
            return K_STRING;
        }
        int c = content[0] & 0xff;
        if (c == 0xff) {
            return K_OPAQUE;
        }

        if (c == '-' || c >= '0' && c <= '9') {
            for (int i = 1; i < content.length; i++) {
                if (!(c >= '0' && c <= '9')) {
                    return K_STRING;
                }
            }
            return K_NUMERIC;
        }

        return K_STRING;
    }

    public boolean isBoolean() {
        final int kind = this.kind;
        return kind == K_BOOLEAN_TRUE || kind == K_BOOLEAN_FALSE;
    }

    public boolean isNumeric() {
        return kind == K_NUMERIC;
    }

    public boolean isOpaque() {
        return kind == K_OPAQUE;
    }

    public boolean isString() {
        return kind == K_STRING;
    }

    public int asInt() {
        if (kind == K_NUMERIC) {
            return asInt;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int compareTo(final AttributeValue other) {
        Assert.checkNotNullParam("other", other);
        int res = signum(other.kind - kind);
        if (res != 0) return res;
        switch (kind) {
            case K_NUMERIC: return signum(other.asInt - asInt);
            case K_STRING: return toString().compareTo(other.toString());
            case K_OPAQUE: return compareArray(content, other.content);
            default: {
                // boolean; equal
                return 0;
            }
        }
    }

    private int compareArray(final byte[] c1, final byte[] c2) {
        int res;
        for (int i = 0; i < min(c1.length, c2.length); i ++) {
            res = signum((c2[i] & 0xff) - (c1[i] & 0xff));
            if (res != 0) return res;
        }
        return signum(c1.length - c2.length);
    }

    public String toString() {
        final String toString = this.toString;
        if (toString == null) {
            StringBuilder builder = new StringBuilder();
            final byte[] content = this.content;
            switch (kind) {
                case K_OPAQUE: {
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
                    break;
                }
                case K_STRING: {
                    final int length = content.length;
                    int i = 0;
                    while (i < length) {
                        final int a = content[i++] & 0xff;
                        if (a < 0x80) {
                            if (Character.isISOControl(a)) {
                                int l = a & 0x0f;
                                int h = (a & 0xf0) >> 4;
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
                            } else {
                                builder.appendCodePoint(a);
                            }
                        } else if (a < 0xC0) {
                            builder.append((char) 0xfffd);
                        } else if (a < 0xE0) {
                            if (i < length) {
                                final int b = content[i ++] & 0xff;
                                builder.appendCodePoint((a & 0x1f) << 6 | b & 0x3f);
                            } else {
                                builder.append((char) 0xfffd);
                            }
                        } else if (a < 0xF0) {
                            if (i < length) {
                                final int b = content[i++] & 0xff;
                                if ((b & 0xc0) == 0x80) {
                                    if (i < length) {
                                        final int c = content[i++] & 0xff;
                                        if ((c & 0xc0) == 0x80) {
                                            builder.appendCodePoint((a & 0x0f) << 12 | (b & 0x3f) << 6 | c & 0x3f);
                                        } else {
                                            builder.append((char) 0xfffd);
                                        }
                                    } else {
                                        builder.append((char) 0xfffd);
                                    }
                                } else {
                                    builder.append((char) 0xfffd);
                                }
                            } else {
                                builder.append((char) 0xfffd);
                            }
                        } else if (a < 0xF8) {
                            builder.append((char) 0xfffd);
                            i += 4;
                        } else if (a < 0xFC) {
                            builder.append((char) 0xfffd);
                            i += 5;
                        } else {
                            builder.append((char) 0xfffd);
                        }
                    }
                    break;
                }
                default: {
                    // all other cases already have had toString generated
                    throw Assert.impossibleSwitchCase(kind);
                }
            }
            return this.toString = builder.toString();
        }
        return toString;
    }

    Object writeReplace() {
        return new Serialized(content);
    }

    static class Serialized implements Serializable {
        private static final long serialVersionUID = 227138322941710864L;
        private final byte[] content;

        Serialized(final byte[] content) {
            this.content = content;
        }

        byte[] getContent() {
            return content;
        }

        Object readResolve() {
            return fromBytes(content);
        }
    }
}
