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



package com.zqzqq.bootkits.loader.utils;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bootstrap 阶段自包含工具类，仅依赖 JDK 标准库。
 * <p>
 * loader 模块作为引导层运行在系统类加载器上，bootstrap 完成前
 * {@code lib/} 目录下的第三方类不可见，因此本模块不得引用
 * {@code com.zqzqq.bootkits.utils.*} 等外部工具类。
 *
 * @author starBlues
 * @since 4.0.12
 */
public final class BootstrapUtils {

    private static final String RELATIVE_SIGN = "~";
    private static final String SLASH = "/";
    private static final String BACKSLASH = "\\";
    private static final String DOUBLE_SLASH = "//";

    private BootstrapUtils() {}

    // ── ObjectUtils 兼容方法 ──────────────────────────────────────────

    public static boolean isEmpty(CharSequence charSequence) {
        return charSequence == null || charSequence.length() == 0;
    }

    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof Optional) {
            return !((Optional<?>) obj).isPresent();
        }
        if (obj instanceof CharSequence) {
            return ((CharSequence) obj).length() == 0;
        }
        if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        }
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).isEmpty();
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).isEmpty();
        }
        return false;
    }

    public static <T> List<T> toList(T... array) {
        if (array.length == 0) {
            return new ArrayList<>(0);
        }
        List<T> list = new ArrayList<>(array.length);
        for (T t : array) {
            list.add(t);
        }
        return list;
    }

    public static <T> T getFirst(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        for (T t : collection) {
            return t;
        }
        return null;
    }

    // ── FilesUtils 兼容方法 ──────────────────────────────────────────

    public static boolean isRelativePath(String path) {
        if (isEmpty(path)) {
            return false;
        }
        return path.startsWith(RELATIVE_SIGN);
    }

    public static String resolveRelativePath(String rootPath, String relativePath) {
        if (isEmpty(relativePath)) {
            return relativePath;
        }
        if (isRelativePath(relativePath)) {
            String resolved = relativePath.replaceFirst(RELATIVE_SIGN, "");
            return joiningFilePath(rootPath, resolved);
        } else {
            return relativePath;
        }
    }

    public static String joiningFilePath(String... paths) {
        if (paths == null || paths.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int length = paths.length;
        for (int i = 0; i < length; i++) {
            String path = paths[i];
            if (isEmpty(path)) {
                continue;
            }
            if (i < length - 1) {
                if (path.endsWith(SLASH)) {
                    path = path.substring(0, path.lastIndexOf(SLASH));
                } else if (path.endsWith(BACKSLASH)) {
                    path = path.substring(0, path.lastIndexOf(BACKSLASH));
                } else if (path.endsWith(DOUBLE_SLASH)) {
                    path = path.substring(0, path.lastIndexOf(DOUBLE_SLASH));
                }
            }
            if (i > 0) {
                if (path.startsWith(File.separator) || path.startsWith(SLASH) ||
                        path.startsWith(DOUBLE_SLASH) || path.startsWith(BACKSLASH)) {
                    sb.append(path);
                } else {
                    sb.append(File.separator).append(path);
                }
            } else {
                sb.append(path);
            }
        }
        return sb.toString();
    }
}
