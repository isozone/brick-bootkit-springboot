/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
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

package com.zqzqq.bootkits.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilsTest {

    @Test
    void isEmptyCharSequence() {
        assertTrue(ObjectUtils.isEmpty((CharSequence) null));
        assertTrue(ObjectUtils.isEmpty(""));
        assertFalse(ObjectUtils.isEmpty("a"));
    }

    @Test
    void isEmptyObject() {
        assertTrue(ObjectUtils.isEmpty(null));
        assertTrue(ObjectUtils.isEmpty(Optional.empty()));
        assertFalse(ObjectUtils.isEmpty(Optional.of("x")));
        assertTrue(ObjectUtils.isEmpty(new java.util.ArrayList<>()));
        assertFalse(ObjectUtils.isEmpty(Arrays.asList("a")));
        assertTrue(ObjectUtils.isEmpty(Collections.emptyMap()));
        assertFalse(ObjectUtils.isEmpty("x"));
        assertTrue(ObjectUtils.isEmpty(new int[]{}));
        assertFalse(ObjectUtils.isEmpty(new int[]{1}));
    }

    @Test
    void hasText() {
        assertFalse(ObjectUtils.hasText(null));
        assertFalse(ObjectUtils.hasText(""));
        assertFalse(ObjectUtils.hasText("   "));
        assertTrue(ObjectUtils.hasText("a"));
        assertTrue(ObjectUtils.hasText(" a "));
    }

    @Test
    void getFirst() {
        assertNull(ObjectUtils.getFirst(null));
        assertNull(ObjectUtils.getFirst(Collections.emptyList()));
        assertEquals("x", ObjectUtils.getFirst(Arrays.asList("x", "y")));
    }

    @Test
    void toList() {
        assertTrue(ObjectUtils.toList().isEmpty());
        assertEquals(3, ObjectUtils.toList(1, 2, 3).size());
    }

    @Test
    void cleanPath() {
        assertEquals("a/b/c", ObjectUtils.cleanPath("a/b/../b/c"));
    }

    @Test
    void getFilenameExtension() {
        assertEquals("jar", ObjectUtils.getFilenameExtension("a/b.jar"));
        assertEquals("gz", ObjectUtils.getFilenameExtension("a/b.tar.gz"));
        assertNull(ObjectUtils.getFilenameExtension("a/b"));
    }

    @Test
    void endsWithIgnoreCase() {
        assertTrue(ObjectUtils.endsWithIgnoreCase("abcDEF", "def"));
        assertFalse(ObjectUtils.endsWithIgnoreCase("abc", "xyz"));
    }

    @Test
    void commaDelimitedListToStringArray() {
        assertArrayEquals(new String[]{"a", "b"}, ObjectUtils.commaDelimitedListToStringArray("a,b"));
    }

    @Test
    void nullSafeEquals() {
        assertTrue(ObjectUtils.nullSafeEquals(null, null));
        assertFalse(ObjectUtils.nullSafeEquals("a", null));
        assertTrue(ObjectUtils.nullSafeEquals("a", "a"));
    }

    @Test
    void containsElement() {
        assertTrue(ObjectUtils.containsElement(new Object[]{"a", "b"}, "a"));
        assertFalse(ObjectUtils.containsElement(null, "a"));
    }
}
