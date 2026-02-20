/**
 * Copyright [2019-Present] [starBlues]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.plugin.pack;

import com.zqzqq.bootkits.common.Constants;
import com.zqzqq.bootkits.plugin.pack.dev.DevRepackager;
import com.zqzqq.bootkits.utils.ObjectUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Prepare plugin meta files for standalone dev/testing before package phase.
 */
@Mojo(name = "prepare-meta", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresProject = true,
        threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class PrepareMetaMojo extends RepackageMojo {

    private final Set<String> prepareLoadToMainSet = new HashSet<>();

    @Override
    protected void pack() throws MojoExecutionException, MojoFailureException {
        initPrepareLoadToMainSet();
        // Only prepare dev metadata files (PLUGIN.META/RESOURCES.CONF/MANIFEST) for local startup.
        new DevRepackager(this).repackage();
    }

    @Override
    public String resolveLoadToMain(Artifact artifact) {
        if (artifact == null) {
            return "";
        }
        if (prepareLoadToMainSet.contains(artifact.getGroupId() + artifact.getArtifactId())) {
            return Constants.LOAD_TO_MAIN_SIGN;
        }
        return "";
    }

    private void initPrepareLoadToMainSet() {
        LoadToMain loadToMain = getLoadToMain();
        if (loadToMain == null) {
            return;
        }
        List<Dependency> dependencies = loadToMain.getDependencies();
        if (ObjectUtils.isEmpty(dependencies)) {
            return;
        }
        for (Dependency dependency : dependencies) {
            prepareLoadToMainSet.add(dependency.getGroupId() + dependency.getArtifactId());
        }
    }
}
