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

package com.zqzqq.bootkits.loader.launcher;

import com.zqzqq.bootkits.loader.jar.JarFile;
import com.zqzqq.bootkits.loader.launcher.coexist.CoexistFastJarLauncher;
import com.zqzqq.bootkits.loader.launcher.coexist.CoexistJarOuterLauncher;
import com.zqzqq.bootkits.loader.launcher.isolation.IsolationFastJarLauncher;
import com.zqzqq.bootkits.loader.launcher.isolation.IsolationJarOuterLauncher;
import com.zqzqq.bootkits.loader.launcher.runner.MethodRunner;
import com.zqzqq.bootkits.loader.utils.ObjectUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static com.zqzqq.bootkits.loader.LoaderConstant.*;

/**
 * 生产环境 Launcher
 *
 * @author starBlues
 * @since 3.0.4
 * @version 4.0.0
 */
public class ProdLauncher implements Launcher<ClassLoader>{

    static final String SPRING_BOOTSTRAP_RUN_METHOD = "run";

    @Override
    public ClassLoader run(String... args) throws Exception {
        File rootJarFile = getRootJarFile();
        String startClass = null;
        String mainPackageType;
        String developmentMode;
        try (JarFile jarFile = new JarFile(rootJarFile)){
            Manifest manifest = jarFile.getManifest();
            IllegalStateException exception = new IllegalStateException("当前启动包非法");
            if(manifest == null || manifest.getMainAttributes() == null){
                throw exception;
            }
            Attributes mainAttributes = manifest.getMainAttributes();
            startClass = mainAttributes.getValue(START_CLASS);
            if (ObjectUtils.isEmpty(startClass)) {
                throw exception;
            }
            mainPackageType = mainAttributes.getValue(MAIN_PACKAGE_TYPE);
            developmentMode = resolveDevelopmentMode(mainAttributes.getValue(MAIN_DEVELOPMENT_MODE));
        }

        DevelopmentModeSetting.setDevelopmentMode(developmentMode);

        MethodRunner methodRunner = new MethodRunner(startClass, SPRING_BOOTSTRAP_RUN_METHOD, args);
        AbstractMainLauncher launcher;

        if(Objects.equals(mainPackageType, MAIN_PACKAGE_TYPE_JAR_OUTER)){
            launcher = getJarOuterLauncher(methodRunner, rootJarFile);
        } else {
            launcher = getFastJarLauncher(methodRunner, rootJarFile);
        }

        return launcher.run(args);
    }

    static String resolveDevelopmentMode(String manifestDevelopmentMode) {
        if (!ObjectUtils.isEmpty(manifestDevelopmentMode)) {
            return manifestDevelopmentMode;
        }
        String modeFromProperties = DevelopmentModeSetting.resolveDevelopmentModeFromProperties();
        if (!ObjectUtils.isEmpty(modeFromProperties)) {
            return modeFromProperties;
        }
        throw new RuntimeException("Missing developmentMode configuration. Configure manifest " +
                "Main-Development-Mode, -Dplugin.developmentMode, " +
                "-Dspring-boot3-brick-bootkit.developmentMode, -DdevelopmentMode, " +
                "or PLUGIN_DEVELOPMENT_MODE.");
    }


    private File getRootJarFile() throws URISyntaxException {
        ProtectionDomain protectionDomain = SpringMainBootstrap.class.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
        String path = (location != null) ? location.getSchemeSpecificPart() : null;
        if (path == null) {
            throw new IllegalStateException("Unable to determine code source archive");
        }
        File root = new File(path);
        if (!root.exists()) {
            throw new IllegalStateException("Unable to determine code source archive from " + root);
        }
        return root;
    }

    private AbstractMainLauncher getFastJarLauncher(MethodRunner methodRunner, File rootJarFile){
        AbstractMainLauncher launcher;
        if(DevelopmentModeSetting.isolation()){
            launcher = new IsolationFastJarLauncher(methodRunner, rootJarFile);
        } else if(DevelopmentModeSetting.coexist()){
            launcher = new CoexistFastJarLauncher(methodRunner, rootJarFile);
        } else {
            throw DevelopmentModeSetting.getUnknownModeException();
        }
        return launcher;
    }

    private AbstractMainLauncher getJarOuterLauncher(MethodRunner methodRunner, File rootJarFile){
        AbstractMainLauncher launcher;
        if(DevelopmentModeSetting.isolation()){
            launcher = new IsolationJarOuterLauncher(methodRunner, rootJarFile);
        } else if(DevelopmentModeSetting.coexist()){
            launcher = new CoexistJarOuterLauncher(methodRunner, rootJarFile);
        } else {
            throw DevelopmentModeSetting.getUnknownModeException();
        }
        return launcher;
    }
}
