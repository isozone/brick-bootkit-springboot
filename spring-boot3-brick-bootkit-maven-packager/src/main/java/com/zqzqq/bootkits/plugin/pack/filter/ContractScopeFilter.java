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

package com.zqzqq.bootkits.plugin.pack.filter;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 契约/宿主提供依赖过滤器。
 * <p>
 * 用于支撑「抽 {@code *-contract} 共享模块」的地基：把标为 {@code provided} 作用域、
 * 或 {@code optional} 的依赖从插件 jar 中剔除，使其不被重复打包——这些类由宿主
 * （主应用）classpath 提供，插件通过父类加载器委派即可复用，避免同 JVM（coexist）下
 * 出现「同一接口/DTO 多个类定义」导致的 {@link ClassCastException}。
 * <p>
 * 默认开启；如确需把 optional/provided 依赖也打进插件，可关闭对应开关。
 */
public class ContractScopeFilter implements ArtifactsFilter {

    private final boolean excludeProvided;
    private final boolean excludeOptional;

    public ContractScopeFilter(boolean excludeProvided, boolean excludeOptional) {
        this.excludeProvided = excludeProvided;
        this.excludeOptional = excludeOptional;
    }

    @Override
    public Set<Artifact> filter(Set<Artifact> artifacts) throws ArtifactFilterException {
        Set<Artifact> result = new LinkedHashSet<>();
        for (Artifact artifact : artifacts) {
            if (isArtifactIncluded(artifact)) {
                result.add(artifact);
            }
        }
        return result;
    }

    @Override
    public boolean isArtifactIncluded(Artifact artifact) throws ArtifactFilterException {
        if (excludeProvided && "provided".equals(artifact.getScope())) {
            return false;
        }
        if (excludeOptional && artifact.isOptional()) {
            return false;
        }
        return true;
    }
}
