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



package com.zqzqq.bootkits.plugin.pack;

import com.zqzqq.bootkits.plugin.pack.filter.Exclude;
import com.zqzqq.bootkits.plugin.pack.filter.ExcludeFilter;
import com.zqzqq.bootkits.plugin.pack.filter.Include;
import com.zqzqq.bootkits.plugin.pack.filter.IncludeFilter;
import com.zqzqq.bootkits.plugin.pack.utils.CommonUtils;
import com.zqzqq.bootkits.utils.ObjectUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 抽象依赖过滤的 mojo
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class AbstractDependencyFilterMojo extends AbstractMojo {


    @Parameter(property = "spring-boot3-brick-bootkit-packager.includes")
    private List<Include> includes;

    @Parameter(property = "spring-boot3-brick-bootkit-packager.excludes")
    private List<Exclude> excludes;


    protected final Set<Artifact> filterDependencies(Set<Artifact> dependencies, FilterArtifacts filters)
            throws MojoExecutionException {
        try {
            Set<Artifact> filtered = new LinkedHashSet<>(dependencies);
            filtered.retainAll(filters.filter(dependencies));
            return filtered;
        }
        catch (ArtifactFilterException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    protected final FilterArtifacts getFilters(ArtifactsFilter... additionalFilters) {
        FilterArtifacts filters = new FilterArtifacts();
        for (ArtifactsFilter additionalFilter : additionalFilters) {
            filters.addFilter(additionalFilter);
        }
        if (!ObjectUtils.isEmpty(includes)) {
            filters.addFilter(new IncludeFilter(this.includes));
        }
        if(ObjectUtils.isEmpty(excludes)){
            excludes = new ArrayList<>();
        }
        // 添加框架核心排除
        addPluginFrameworkExclude();
        // 添加spring web 环境排除
        addSpringWebEnvExclude();
        filters.addFilter(new ExcludeFilter(this.excludes));
        return filters;
    }

    private void addPluginFrameworkExclude(){
        excludes.add(CommonUtils.getPluginFrameworkExclude());
    }

    private void addSpringWebEnvExclude(){
        excludes.add(Exclude.get("org.springframework.boot", "spring-boot-starter-web"));
        excludes.add(Exclude.get("org.springframework.boot", "spring-boot-starter-tomcat"));
        excludes.add(Exclude.get("org.springframework.boot", "spring-boot-starter-json"));
        excludes.add(Exclude.get("org.springframework", "spring-webmvc"));
        excludes.add(Exclude.get("org.springframework", "spring-web"));

        // jackson
        excludes.add(Exclude.get("com.fasterxml.jackson.core", "jackson-core"));
        excludes.add(Exclude.get("com.fasterxml.jackson.core", "jackson-databind"));
        excludes.add(Exclude.get("com.fasterxml.jackson.core", "jackson-annotations"));
        excludes.add(Exclude.get("com.fasterxml.jackson.module", "jackson-module-parameter-names"));
    }

}

