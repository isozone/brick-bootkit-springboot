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



package com.zqzqq.bootkits.core.descriptor;

/**
 * 插件依赖包信息
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class PluginLibInfo {

    /**
     * 路径
     */
    private final String path;

    /**
     * 是否加载到主程序中
     */
    private final boolean loadToMain;

    public PluginLibInfo() {
        this.path = null;
        this.loadToMain = false;
    }

    public PluginLibInfo(String path, boolean loadToMain) {
        this.path = path;
        this.loadToMain = loadToMain;
    }

    public String getPath() {
        return path;
    }

    public boolean isLoadToMain() {
        return loadToMain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginLibInfo that = (PluginLibInfo) o;
        return loadToMain == that.loadToMain && 
               (path != null ? path.equals(that.path) : that.path == null);
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (loadToMain ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PluginLibInfo{" +
                "path='" + path + '\'' +
                ", loadToMain=" + loadToMain +
                '}';
    }
}