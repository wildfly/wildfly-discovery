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

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.wildfly.common.Assert;

/**
 * A filter specification for matching attributes.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class FilterSpec implements Serializable {

    private static final long serialVersionUID = 2473228835262926917L;

    FilterSpec() {}

    /**
     * Determine whether the given simple attribute map matches this filter.
     *
     * @param attributes the attribute map
     * @return {@code true} if the map matches, {@code false} otherwise
     */
    public abstract boolean matchesSimple(Map<String, AttributeValue> attributes);

    /**
     * Determine whether the given attribute multi-map matches this filter.
     *
     * @param attributes the attribute map
     * @return {@code true} if the map matches, {@code false} otherwise
     */
    public abstract boolean matchesMulti(Map<String, ? extends Collection<AttributeValue>> attributes);

    private static final FilterSpec[] NONE = new FilterSpec[0];

    private static FilterSpec[] safeToArray(Collection<FilterSpec> collection) {
        return safeToArray(collection.iterator(), 0);
    }

    private static FilterSpec[] safeToArray(Iterator<FilterSpec> it, int idx) {
        final FilterSpec[] array;
        while (it.hasNext()) {
            FilterSpec next = it.next();
            if (next != null) {
                array = safeToArray(it, idx + 1);
                array[idx] = next;
                return array;
            }
        }
        array = idx == 0 ? NONE : new FilterSpec[idx];
        return array;
    }

    /**
     * Create a new filter from a string.
     *
     * @param string the filter string
     * @return the filter specification
     */
    public static FilterSpec fromString(String string) {
        Assert.checkNotNullParam("string", string);
        final StringIterator i = new StringIterator(string);
        final FilterSpec filterSpec = parseFilter(i);
        if (i.hasNext()) {
            i.next();
            throw i.unexpectedCharacter();
        }
        return filterSpec;
    }

    private static FilterSpec parseFilter(StringIterator i) {
        if (i.next() != '(') {
            throw i.unexpectedCharacter();
        }
        switch (i.peek()) {
            case '&': {
                return parseAllFilter(i);
            }
            case '|': {
                return parseAnyFilter(i);
            }
            case '!': {
                return parseNotFilter(i);
            }
            case '=':
            case '~':
            case '(':
            case ')':
            case '\\':
            case '<':
            case '>':
            case '*':
            case ':': {
                i.next();
                throw i.unexpectedCharacter();
            }
            default: {
                return parseStartsWithAttribute(i);
            }
        }
    }

    private static final int OP_EQUAL   = 1;
    private static final int OP_LE      = 2;
    private static final int OP_GE      = 3;
    private static final int OP_APPROX  = 4;

    private static FilterSpec parseStartsWithAttribute(final StringIterator i) {
        StringBuilder attr = new StringBuilder();
        int cp;
        for (;;) {
            cp = i.next();
            switch (cp) {
                case '=': {
                    return parsePlainOp(i, OP_EQUAL, attr.toString());
                }
                case '~': {
                    if (i.next() == '=') {
                        return parsePlainOp(i, OP_APPROX, attr.toString());
                    } else {
                        throw i.unexpectedCharacter();
                    }
                }
                case '>': {
                    if (i.next() == '=') {
                        return parsePlainOp(i, OP_GE, attr.toString());
                    } else {
                        throw i.unexpectedCharacter();
                    }
                }
                case '<': {
                    if (i.next() == '=') {
                        return parsePlainOp(i, OP_LE, attr.toString());
                    } else {
                        throw i.unexpectedCharacter();
                    }
                }
                case '\\': {
                    parseEscapedCodePoint(i, getHexByte(i), attr);
                    break;
                }
                default: {
                    attr.appendCodePoint(cp);
                    break;
                }
            }
        }
    }

    private static FilterSpec parsePlainOp(final StringIterator i, final int op, final String attr) {
        StringBuilder val = new StringBuilder();
        int cp;
        for (;;) {
            cp = i.next();
            switch (cp) {
                case ')': {
                    switch (op) {
                        case OP_APPROX: return approx(attr, val.toString());
                        case OP_EQUAL: return equal(attr, val.toString());
                        case OP_GE: return greaterOrEqual(attr, val.toString());
                        case OP_LE: return lessOrEqual(attr, val.toString());
                        default: throw new IllegalStateException();
                    }
                }
                case '*': {
                    if (op == OP_EQUAL) {
                        if (val.length() == 0 && i.peek() == ')') {
                            i.next();
                            return hasAttribute(attr);
                        }
                        // substring
                        return parseSubstring(i, attr, val.toString());
                    }
                    throw i.unexpectedCharacter();
                }
                case '(': {
                    throw i.unexpectedCharacter();
                }
                case '\\': {
                    final int firstByte = getHexByte(i);
                    if (firstByte == 0xff && val.length() == 0) {
                        if (op == OP_EQUAL || op == OP_GE || op == OP_LE) {
                            return parsePlainBytesOp(i, op, attr);
                        }
                        throw i.unexpectedCharacter();
                    } else {
                        parseEscapedCodePoint(i, firstByte, val);
                    }
                    break;
                }
                default: {
                    val.appendCodePoint(cp);
                    break;
                }
            }
        }
    }

    private static FilterSpec parsePlainBytesOp(final StringIterator i, final int op, final String attr) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int cp;
        for (;;) {
            cp = i.next();
            switch (cp) {
                case ')': {
                    switch (op) {
                        case OP_EQUAL: return equal(attr, os.toByteArray());
                        case OP_GE: return greaterOrEqual(attr, os.toByteArray());
                        case OP_LE: return lessOrEqual(attr, os.toByteArray());
                        default: throw new IllegalStateException();
                    }
                }
                case '*':
                case '(': {
                    throw i.unexpectedCharacter();
                }
                case '\\': {
                    os.write(getHexByte(i));
                    break;
                }
                default: {
                    throw i.unexpectedCharacter();
                }
            }
        }
    }

    private static void parseEscapedCodePoint(final StringIterator i, final int firstByte, final StringBuilder b) {
        if (firstByte <= 0x7F) {
            b.appendCodePoint(firstByte);
            return;
        }
        if (firstByte >= 0xF8) {
            b.appendCodePoint('�');
            return;
        } else if (firstByte >= 0xF0) {
            // 4 byte sequence
            int b2 = getEscapedHexByte(i);
            if ((b2 & 0xC0) != 0x80) {
                b.appendCodePoint('�');
                parseEscapedCodePoint(i, b2, b);
                return;
            }
            int b3 = getEscapedHexByte(i);
            if ((b3 & 0xC0) != 0x80) {
                b.appendCodePoint('�');
                parseEscapedCodePoint(i, b3, b);
                return;
            }
            int b4 = getEscapedHexByte(i);
            if ((b4 & 0xC0) != 0x80) {
                b.appendCodePoint('�');
                parseEscapedCodePoint(i, b4, b);
                return;
            }
            b.appendCodePoint((firstByte & 0x1F) << 18 | (b2 & 0x3F) << 12 | (b3 & 0x3F) << 6 | b4 & 0x3F);
            return;
        } else if (firstByte >= 0xE0) {
            // 3 byte sequence
            int b2 = getEscapedHexByte(i);
            if ((b2 & 0xC0) != 0x80) {
                b.appendCodePoint('�');
                parseEscapedCodePoint(i, b2, b);
                return;
            }
            int b3 = getEscapedHexByte(i);
            if ((b3 & 0xC0) != 0x80) {
                b.appendCodePoint('�');
                parseEscapedCodePoint(i, b3, b);
                return;
            }
            b.appendCodePoint((firstByte & 0x1F) << 12 | (b2 & 0x3F) << 6 | b3 & 0x3F);
            return;
        } else if (firstByte >= 0xC0) {
            // 2 byte sequence
            int b2 = getEscapedHexByte(i);
            if ((b2 & 0xC0) != 0x80) {
                b.appendCodePoint('�');
                parseEscapedCodePoint(i, b2, b);
                return;
            }
            b.appendCodePoint((firstByte & 0x1F) << 6 | b2 & 0x3F);
            return;
        } else {
            // invalid sequence
            b.appendCodePoint('�');
            return;
        }
    }

    private static int getEscapedHexByte(final StringIterator i) {
        if (i.next() != '\\') {
            throw i.unexpectedCharacter();
        }
        return getHexByte(i);
    }

    private static int getHexByte(final StringIterator i) {
        int cp;
        int iv = i.next();
        if (iv >= '0' && iv <= '9') {
            cp = iv - '0';
        } else if (iv >= 'A' && iv <= 'F') {
            cp = iv - 'A' + 10;
        } else if (iv >= 'a' && iv <= 'f') {
            cp = iv - 'a' + 10;
        } else {
            throw i.unexpectedCharacter();
        }
        iv = i.next();
        if (iv >= '0' && iv <= '9') {
            cp = cp * 16 + iv - '0';
        } else if (iv >= 'A' && iv <= 'F') {
            cp = cp * 16 + iv - 'A' + 10;
        } else if (iv >= 'a' && iv <= 'f') {
            cp = cp * 16 + iv - 'a' + 10;
        } else {
            throw i.unexpectedCharacter();
        }
        return cp;
    }

    private static FilterSpec parseSubstring(final StringIterator i, final String attr, final String initialPart) {
        StringBuilder val = new StringBuilder();
        int cp;
        for (;;) {
            cp = i.next();
            switch (cp) {
                case ')': {
                    return substringMatch(attr, initialPart, val.toString());
                }
                case '*': {
                    throw i.unexpectedCharacter();
                }
                case '\\': {
                    int iv;
                    iv = i.next();
                    if (iv >= '0' && iv <= '9') {
                        cp = iv - '0';
                    } else if (iv >= 'A' && iv <= 'F') {
                        cp = iv - 'A' + 10;
                    } else if (iv >= 'a' && iv <= 'f') {
                        cp = iv - 'a' + 10;
                    } else {
                        throw i.unexpectedCharacter();
                    }
                    iv = i.next();
                    if (iv >= '0' && iv <= '9') {
                        cp = cp * 10 + iv - '0';
                    } else if (iv >= 'A' && iv <= 'F') {
                        cp = cp * 10 + iv - 'A' + 10;
                    } else if (iv >= 'a' && iv <= 'f') {
                        cp = cp * 10 + iv - 'a' + 10;
                    } else {
                        throw i.unexpectedCharacter();
                    }
                    // fall thru
                }
                default: {
                    val.appendCodePoint(cp);
                    break;
                }
            }
        }
    }

    private static FilterSpec parseAllFilter(StringIterator i) {
        i.next(); // == '&'
        final ArrayList<FilterSpec> filters = new ArrayList<>();
        for (;;) {
            if (i.peek() == ')') {
                i.next();
                if (filters.isEmpty()) {
                    return all(NONE);
                } else {
                    return all(filters.toArray(new FilterSpec[filters.size()]));
                }
            } else {
                filters.add(parseFilter(i));
            }
        }
    }

    private static FilterSpec parseAnyFilter(StringIterator i) {
        i.next(); // == '|'
        final ArrayList<FilterSpec> filters = new ArrayList<>();
        for (;;) {
            if (i.peek() == ')') {
                i.next();
                if (filters.isEmpty()) {
                    return any(NONE);
                } else {
                    return any(filters.toArray(new FilterSpec[filters.size()]));
                }
            } else {
                filters.add(parseFilter(i));
            }
        }
    }

    private static FilterSpec parseNotFilter(StringIterator i) {
        i.next(); // == '!'
        final FilterSpec result = not(parseFilter(i));
        if (i.next() != ')') {
            throw i.unexpectedCharacter();
        }
        return result;
    }

    /**
     * Create a filter which matches all of the given sub-filters.
     *
     * @param specs the sub-filters
     * @return the filter specification
     */
    public static FilterSpec all(FilterSpec... specs) {
        Assert.checkNotNullParam("specs", specs);
        return new ListFilterSpec(true, specs.clone());
    }

    /**
     * Create a filter which matches all of the given sub-filters.
     *
     * @param specs the sub-filters
     * @return the filter specification
     */
    public static FilterSpec all(Collection<FilterSpec> specs) {
        Assert.checkNotNullParam("specs", specs);
        return new ListFilterSpec(true, safeToArray(specs));
    }

    /**
     * Create a filter which matches any of the given sub-filters.
     *
     * @param specs the sub-filters
     * @return the filter specification
     */
    public static FilterSpec any(FilterSpec... specs) {
        Assert.checkNotNullParam("specs", specs);
        return new ListFilterSpec(false, specs.clone());
    }

    /**
     * Create a filter which matches any of the given sub-filters.
     *
     * @param specs the sub-filters
     * @return the filter specification
     */
    public static FilterSpec any(Collection<FilterSpec> specs) {
        Assert.checkNotNullParam("specs", specs);
        return new ListFilterSpec(false, safeToArray(specs));
    }

    /**
     * Create a filter which matches the inverse of the given filter.
     *
     * @param spec the sub-filter
     * @return the filter specification
     */
    public static FilterSpec not(FilterSpec spec) {
        Assert.checkNotNullParam("spec", spec);
        return new NotFilterSpec(spec);
    }

    /**
     * Create a filter specification which matches one attribute value.
     *
     * @param attribute the attribute name
     * @param value the attribute value
     * @return the filter specification
     */
    public static FilterSpec equal(String attribute, String value) {
        Assert.checkNotNullParam("attribute", attribute);
        Assert.checkNotNullParam("value", value);
        return new EqualsFilterSpec(attribute, AttributeValue.fromString(value));
    }

    /**
     * Create a filter specification which matches one attribute value.
     *
     * @param attribute the attribute name
     * @param value the attribute byte value
     * @return the filter specification
     */
    public static FilterSpec equal(String attribute, byte[] value) {
        Assert.checkNotNullParam("attribute", attribute);
        Assert.checkNotNullParam("value", value);
        return new EqualsFilterSpec(attribute, AttributeValue.fromBytes(value));
    }

    /**
     * Create a filter specification which matches a leading or trailing (or both) substring of one attribute value.
     *
     * @param attribute the attribute name
     * @param initialPart the initial part, or {@code ""} to match any initial part
     * @param finalPart the final part, or {@code ""} to match any final part
     * @return the filter specification
     */
    public static FilterSpec substringMatch(final String attribute, final String initialPart, final String finalPart) {
        Assert.checkNotNullParam("attribute", attribute);
        Assert.checkNotNullParam("initialPart", initialPart);
        Assert.checkNotNullParam("finalPart", finalPart);
        if (initialPart.isEmpty() && finalPart.isEmpty()) {
            return hasAttribute(attribute);
        }
        return new SubstringFilterSpec(attribute, initialPart, finalPart);
    }

    /**
     * Create a filter specification which approximately matches one attribute value.
     *
     * @param attribute the attribute name
     * @param value the attribute value
     * @return the filter specification
     */
    public static FilterSpec approx(String attribute, String value) {
        Assert.checkNotNullParam("attribute", attribute);
        Assert.checkNotNullParam("value", value);
        return new EqualsFilterSpec(attribute, AttributeValue.fromString(value));
    }

    /**
     * Create a filter specification which matches when the given attribute's value is lexicographically greater than or
     * equal to the given value.
     *
     * @param attribute the attribute name
     * @param value the attribute value
     * @return the filter specification
     */
    public static FilterSpec greaterOrEqual(String attribute, String value) {
        Assert.checkNotNullParam("attribute", attribute);
        Assert.checkNotNullParam("value", value);
        return new GreaterEqualFilterSpec(attribute, AttributeValue.fromString(value));
    }

    /**
     * Create a filter specification which matches when the given attribute's value is lexicographically greater than or
     * equal to the given value.
     *
     * @param attribute the attribute name
     * @param value the attribute value
     * @return the filter specification
     */
    public static FilterSpec greaterOrEqual(String attribute, byte[] value) {
        Assert.checkNotNullParam("attribute", attribute);
        Assert.checkNotNullParam("value", value);
        return new GreaterEqualFilterSpec(attribute, AttributeValue.fromBytes(value));
    }

    /**
     * Create a filter specification which matches when the given attribute's value is lexicographically less than or
     * equal to the given value.
     *
     * @param attribute the attribute name
     * @param value the attribute value
     * @return the filter specification
     */
    public static FilterSpec lessOrEqual(String attribute, String value) {
        Assert.checkNotNullParam("attribute", attribute);
        Assert.checkNotNullParam("value", value);
        return new LessEqualFilterSpec(attribute, AttributeValue.fromString(value));
    }
    /**
     * Create a filter specification which matches when the given attribute's value is lexicographically less than or
     * equal to the given value.
     *
     * @param attribute the attribute name
     * @param value the attribute value
     * @return the filter specification
     */
    public static FilterSpec lessOrEqual(String attribute, byte[] value) {
        Assert.checkNotNullParam("attribute", attribute);
        Assert.checkNotNullParam("value", value);
        return new LessEqualFilterSpec(attribute, AttributeValue.fromBytes(value));
    }

    /**
     * Create a filter specification which matches when the given attribute is present.
     *
     * @param attribute the attribute name
     * @return the filter specification
     */
    public static FilterSpec hasAttribute(String attribute) {
        Assert.checkNotNullParam("attribute", attribute);
        return new HasFilterSpec(attribute);
    }

    /**
     * Escape an attribute string, suitable for putting into a filter.
     *
     * @param str the string to escape
     * @return the escaped string
     */
    public static String escape(String str) {
        Assert.checkNotNullParam("str", str);
        final int len = str.length();
        char ch;
        for (int i = 0; i < len; i ++) {
            ch = str.charAt(i);
            if (ch == '*' || ch == '(' || ch == ')' || ch == '\\' || ch == 0 || ch == '|' || ch == '=' || ch == '<' || ch == '>' || ch == '~' || ch == '/' || ch == '&') {
                final StringBuilder b = new StringBuilder(str.length() + str.length() >> 2);
                escapeTo(str, b);
                return b.toString();
            }
        }
        return str;
    }

    /**
     * Escape an attribute string, suitable for putting into a filter.
     *
     * @param str the string to escape
     * @param b the builder to append to
     */
    public static void escapeTo(String str, StringBuilder b) {
        Assert.checkNotNullParam("str", str);
        Assert.checkNotNullParam("b", b);
        final int len = str.length();
        char ch;
        for (int i = 0; i < len; i ++) {
            ch = str.charAt(i);
            if (ch == '*' || ch == '(' || ch == ')' || ch == '\\' || ch == 0 || ch == '|' || ch == '=' || ch == '<' || ch == '>' || ch == '~' || ch == '/' || ch == '&') {
                b.append('\\');
                int iv = ch >> 4;
                b.append(iv < 10 ? '0' + iv : 'a' + iv - 10);
                iv = ch & 0xf;
                b.append(iv < 10 ? '0' + iv : 'a' + iv - 10);
            } else {
                b.append(ch);
            }
        }
    }

    /**
     * Get the string representation of this filter.
     *
     * @return the string representation of this filter
     */
    public final String toString() {
        final StringBuilder b = new StringBuilder(40);
        toString(b);
        return b.toString();
    }

    /**
     * Get the string representation of this filter into a string builder.
     *
     * @param builder the builder to append to
     */
    abstract void toString(StringBuilder builder);

    final Object writeReplace() {
        return new Serialized(toString());
    }

    class Serialized implements Serializable {

        private static final long serialVersionUID = 555689132344539984L;

        private final String string;

        Serialized(final String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        final Object readResolve() {
            return FilterSpec.fromString(string);
        }
    }
}
