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



package com.zqzqq.bootkits.plugin.pack.dev;

import com.zqzqq.bootkits.plugin.pack.BasicRepackager;
import com.zqzqq.bootkits.plugin.pack.RepackageMojo;
import com.zqzqq.bootkits.utils.ObjectUtils;
import lombok.Getter;
import org.apache.maven.artifact.Artifact;

import java.util.*;

/**
 * 开发环境打包
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class DevRepackager extends BasicRepackager {

    @Getter
    private Map<String, Dependency> moduleDependencies = Collections.emptyMap();

    public DevRepackager(RepackageMojo repackageMojo) {
        super(repackageMojo);
    }

    @Override
    protected Set<String> getDependenciesIndexSet() throws Exception {
        DevConfig devConfig = repackageMojo.getDevConfig();
        if(devConfig == null){
            return super.getDependenciesIndexSet();
        }
        moduleDependencies = getModuleDependencies(devConfig);
        Set<String> dependenciesIndexSet = super.getDependenciesIndexSet();
        for (Dependency dependency : moduleDependencies.values()) {
            dependenciesIndexSet.add(dependency.getClassesPath());
        }
        List<String> localJars = devConfig.getLocalJars();
        if(!ObjectUtils.isEmpty(localJars)){
            dependenciesIndexSet.addAll(localJars);
        }
        return dependenciesIndexSet;
    }

    @Override
    protected boolean filterArtifact(Artifact artifact) {
        if(super.filterArtifact(artifact)){
            return true;
        }
        String moduleDependencyKey = getModuleDependencyKey(artifact.getGroupId(), artifact.getArtifactId());
        Dependency dependency = moduleDependencies.get(moduleDependencyKey);
        return dependency != null && !ObjectUtils.isEmpty(dependency.getClassesPath());
    }

    protected Map<String, Dependency> getModuleDependencies(DevConfig devConfig) {
        if(devConfig == null){
            return Collections.emptyMap();
        }
        List<Dependency> moduleDependencies = devConfig.getModuleDependencies();
        if(ObjectUtils.isEmpty(moduleDependencies)){
            return Collections.emptyMap();
        }
        Map<String, Dependency> moduleDependenciesMap = new HashMap<>();
        for (Dependency dependency : moduleDependencies) {
            String moduleDependencyKey = getModuleDependencyKey(dependency.getGroupId(),
                    dependency.getArtifactId());
            if(moduleDependencyKey == null){
                continue;
            }
            moduleDependenciesMap.put(moduleDependencyKey, dependency);
        }
        return moduleDependenciesMap;
    }

    protected String getModuleDependencyKey(String groupId, String artifactId){
        if(ObjectUtils.isEmpty(groupId) || ObjectUtils.isEmpty(artifactId)){
            return null;
        }
        return groupId + artifactId;
    }

}

