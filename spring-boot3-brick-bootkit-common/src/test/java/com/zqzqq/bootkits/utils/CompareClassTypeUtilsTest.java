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

import static org.junit.jupiter.api.Assertions.*;

class CompareClassTypeUtilsTest {

    @Test
    void compareAssignable() {
        assertTrue(CompareClassTypeUtils.compare(Number.class, Integer.class));
        assertTrue(CompareClassTypeUtils.compare(int.class, Integer.class));
    }

    @Test
    void comparePrimitive() {
        assertTrue(CompareClassTypeUtils.compare(int.class, int.class));
        assertFalse(CompareClassTypeUtils.compare(int.class, long.class));
        assertFalse(CompareClassTypeUtils.compare(String.class, Integer.class));
    }

    @Test
    void isBoolean() {
        assertTrue(CompareClassTypeUtils.isBoolean(boolean.class));
        assertTrue(CompareClassTypeUtils.isBoolean(Boolean.class));
        assertFalse(CompareClassTypeUtils.isBoolean(Integer.class));
    }

    @Test
    void isInt() {
        assertTrue(CompareClassTypeUtils.isInt(int.class));
        assertTrue(CompareClassTypeUtils.isInt(Integer.class));
        assertFalse(CompareClassTypeUtils.isInt(long.class));
    }
}
