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

import com.zqzqq.bootkits.common.Constants;
import com.zqzqq.bootkits.plugin.pack.dev.DevConfig;
import com.zqzqq.bootkits.plugin.pack.dev.DevRepackager;
import com.zqzqq.bootkits.plugin.pack.encrypt.EncryptConfig;
import com.zqzqq.bootkits.plugin.pack.encrypt.EncryptPlugin;
import com.zqzqq.bootkits.plugin.pack.encrypt.EncryptPluginFactory;
import com.zqzqq.bootkits.plugin.pack.main.MainConfig;
import com.zqzqq.bootkits.plugin.pack.main.MainRepackager;
import com.zqzqq.bootkits.plugin.pack.prod.ProdConfig;
import com.zqzqq.bootkits.plugin.pack.prod.ProdRepackager;
import com.zqzqq.bootkits.utils.ObjectUtils;
import lombok.Getter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 閲嶆柊打包 mojo
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
@Mojo(name = "repackage", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Getter
public class RepackageMojo extends AbstractPackagerMojo {


    @Parameter(property = "spring-boot3-brick-bootkit-packager.devConfig")
    private DevConfig devConfig;

    @Parameter(property = "spring-boot3-brick-bootkit-packager.prodConfig")
    private ProdConfig prodConfig;

    @Parameter(property = "spring-boot3-brick-bootkit-packager.mainConfig")
    private MainConfig mainConfig;

    @Parameter(property = "spring-boot3-brick-bootkit-packager.mainLoad")
    private LoadToMain loadToMain;

    @Parameter(property = "spring-boot3-brick-bootkit-packager.encryptConfig")
    private EncryptConfig encryptConfig;

    private final Set<String> loadToMainSet = new HashSet<>();

    @Override
    protected void pack() throws MojoExecutionException, MojoFailureException {
        initLoadToMainSet();
        String mode = getMode();
        try {
            encrypt();
        } catch (Exception e) {
            throw new MojoExecutionException("encrypt failed: " + e.getMessage());
        }
        if(Constant.MODE_PROD.equalsIgnoreCase(mode)){
            new ProdRepackager(this).repackage();
        } else if(Constant.MODE_DEV.equalsIgnoreCase(mode)){
            new DevRepackager(this).repackage();
        } else if(Constant.MODE_MAIN.equalsIgnoreCase(mode)){
            new MainRepackager(this).repackage();
        } else {
            throw new MojoExecutionException(mode  +" model not supported, mode support : "
                    + Constant.MODE_DEV + "/" + Constant.MODE_PROD);
        }
    }

    public String resolveLoadToMain(Artifact artifact){
        if(artifact == null){
            return "";
        }
        if(loadToMainSet.contains(artifact.getGroupId() + artifact.getArtifactId())){
            return Constants.LOAD_TO_MAIN_SIGN;
        }
        return "";
    }

    private void initLoadToMainSet(){
        if(loadToMain == null){
            return;
        }
        List<Dependency> dependencies = loadToMain.getDependencies();
        if(ObjectUtils.isEmpty(dependencies)){
            return;
        }
        for (Dependency dependency : dependencies) {
            loadToMainSet.add(dependency.getGroupId() + dependency.getArtifactId());
        }
    }

    /**
     * 加密
     * @throws Exception 加密异常
     */
    private void encrypt() throws Exception {
        if(encryptConfig == null){
            return;
        }
        EncryptPlugin encryptPlugin = new EncryptPluginFactory();
        PluginInfo pluginInfo = encryptPlugin.encrypt(encryptConfig, getPluginInfo());
        if(pluginInfo != null){
            setPluginInfo(pluginInfo);
        }
    }

}

