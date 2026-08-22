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

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class FilesUtilsTest {

    @Test
    void joiningFilePath() {
        String sep = File.separator;
        assertEquals("a" + sep + "b", FilesUtils.joiningFilePath("a", "b"));
        assertEquals("a" + sep + "b", FilesUtils.joiningFilePath("a/", "b"));
    }

    @Test
    void joiningZipPath() {
        assertEquals("a/b", FilesUtils.joiningZipPath("a", "b"));
    }

    @Test
    void isRelativePath() {
        assertTrue(FilesUtils.isRelativePath("~x"));
        assertFalse(FilesUtils.isRelativePath("/x"));
        assertFalse(FilesUtils.isRelativePath(null));
    }

    @Test
    void resolveRelativePath() {
        assertEquals("/root" + File.separator + "sub", FilesUtils.resolveRelativePath("/root", "~sub"));
        assertEquals("/abs", FilesUtils.resolveRelativePath("/root", "/abs"));
    }

    @Test
    void existFile() {
        assertFalse(FilesUtils.existFile(null));
        assertFalse(FilesUtils.existFile(""));
        assertTrue(FilesUtils.existFile(System.getProperty("java.io.tmpdir")));
    }
}
