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

import org.apache.maven.artifact.Artifact;

/**
 * Constants for maven packager.
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class Constant {

    public static final String PACKAGING_POM = "pom";
    public static final String SCOPE_PROVIDED = "provided";
    public static final String SCOPE_COMPILE = "compile";
    public static final String SCOPE_SYSTEM = "system";
    public static final String SCOPE_TEST = "test";

    public static final String MAVEN_POM_TYPE = "pom";

    public static final String MAVEN_MAIN_TYPE = "main";

    public static final String MODE_MAIN = "main";
    public static final String MODE_DEV = "dev";
    public static final String MODE_PROD = "prod";

    public static final String PLUGIN_METE_COMMENTS = "plugin meta configuration";

    /**
     * Development mode method name.
     */
    public static final String DEVELOPMENT_MODE_METHOD_NAME = "developmentMode";


    public static boolean isPom(String packageType) {
        return PACKAGING_POM.equalsIgnoreCase(packageType);
    }

    public static boolean filterArtifact(Artifact artifact, Boolean includeSystemScope) {
        boolean scopeFilter = Constant.scopeFilter(artifact.getScope());
        if (scopeFilter) {
            return true;
        }
        if (Constant.isSystemScope(artifact.getScope())) {
            return includeSystemScope == null || !includeSystemScope;
        }
        return Constant.filterPomTypeArtifact(artifact);
    }

    public static boolean filterMainTypeArtifact(Artifact artifact) {
        // Dependencies with type=main are excluded from plugin package.
        return MAVEN_MAIN_TYPE.equalsIgnoreCase(artifact.getType());
    }

    public static boolean filterPomTypeArtifact(Artifact artifact) {
        return MAVEN_POM_TYPE.equalsIgnoreCase(artifact.getType());
    }

    public static boolean scopeFilter(String scope) {
        return SCOPE_PROVIDED.equalsIgnoreCase(scope)
                || SCOPE_TEST.equalsIgnoreCase(scope);
    }

    public static boolean isSystemScope(String scope) {
        return SCOPE_SYSTEM.equalsIgnoreCase(scope);
    }
}
