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

import java.util.NoSuchElementException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class StringIterator {
    private final String string;
    private int idx;

    StringIterator(final String string) {
        this.string = string;
    }

    boolean hasNext() {
        return idx < string.length();
    }

    int next() {
        if (! hasNext()) throw unexpectedEnd();
        try {
            return string.codePointAt(idx);
        } finally {
            idx = string.offsetByCodePoints(idx, 1);
        }
    }

    int peek() {
        if (! hasNext()) throw new NoSuchElementException();
        return string.codePointAt(idx);
    }

    int getOffset() {
        return idx;
    }

    IllegalArgumentException unexpectedCharacter() {
        return new IllegalArgumentException("Unexpected character at " + string.offsetByCodePoints(idx, -1));
    }

    IllegalArgumentException unexpectedEnd() {
        return new IllegalArgumentException("Unexpected end of string");
    }
}
