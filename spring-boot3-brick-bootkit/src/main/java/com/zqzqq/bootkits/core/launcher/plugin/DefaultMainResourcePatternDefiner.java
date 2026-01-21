/**
 * Copyright [2019-Present] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.core.launcher.plugin;

import com.zqzqq.bootkits.core.classloader.MainResourcePatternDefiner;
import com.zqzqq.bootkits.core.launcher.JavaMainResourcePatternDefiner;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.utils.SpringBeanUtils;
import org.springframework.context.ApplicationContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 主程序资源匹配定义器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.3
 */
public class DefaultMainResourcePatternDefiner extends JavaMainResourcePatternDefiner {

    private static final String FRAMEWORK = "com/zqzqq/bootkits/**";

    public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";

    private final String mainPackage;
    private final IntegrationConfiguration configuration;
    private final ApplicationContext applicationContext;

    public DefaultMainResourcePatternDefiner(IntegrationConfiguration configuration, ApplicationContext applicationContext) {
        this.mainPackage = configuration.mainPackage();
        this.configuration = configuration;
        this.applicationContext = applicationContext;
    }

    @Override
    public Set<String> getIncludePatterns() {
        Set<String> includeResourcePatterns = super.getIncludePatterns();
        // add mainPackage
        includeResourcePatterns.add(ObjectUtils.changePackageToMatch(mainPackage));
        // add framework
        includeResourcePatterns.add(FRAMEWORK);
        addWebIncludeResourcePatterns(includeResourcePatterns);
        addApiDoc(includeResourcePatterns);
        addDbDriver(includeResourcePatterns);
        addMainDependencyFramework(includeResourcePatterns);

        addIdea(includeResourcePatterns);
        addLog(includeResourcePatterns);

        // add extension
        List<MainResourcePatternDefiner> extensionPatternDefiners = getExtensionPatternDefiners();
        for (MainResourcePatternDefiner extensionPatternDefiner : extensionPatternDefiners) {
            Set<String> includePatterns = extensionPatternDefiner.getIncludePatterns();
            if(!ObjectUtils.isEmpty(includePatterns)){
                includeResourcePatterns.addAll(includePatterns);
            }
        }

        return includeResourcePatterns;
    }

    @Override
    public Set<String> getExcludePatterns() {
        Set<String> excludeResourcePatterns = new HashSet<>();
        excludeResourcePatterns.add(FACTORIES_RESOURCE_LOCATION);

        // add extension
        List<MainResourcePatternDefiner> extensionPatternDefiners = getExtensionPatternDefiners();
        for (MainResourcePatternDefiner extensionPatternDefiner : extensionPatternDefiners) {
            Set<String> excludePatterns = extensionPatternDefiner.getExcludePatterns();
            if(!ObjectUtils.isEmpty(excludePatterns)){
                excludeResourcePatterns.addAll(excludePatterns);
            }
        }
        return excludeResourcePatterns;
    }

    protected void addWebIncludeResourcePatterns(Set<String> patterns) {
        patterns.add("org/springframework/web/**");
        patterns.add("org/springframework/http/**");
        patterns.add("org/springframework/remoting/**");
        patterns.add("org/springframework/ui/**");

        patterns.add("org/springframework/boot/autoconfigure/http/**");
        patterns.add("org/springframework/boot/autoconfigure/web/**");
        patterns.add("org/springframework/boot/autoconfigure/websocket/**");
        patterns.add("org/springframework/boot/autoconfigure/webservices/**");
        patterns.add("org/springframework/boot/autoconfigure/jackson/**");

        patterns.add("com/fasterxml/jackson/**");
    }

    protected void addApiDoc(Set<String> patterns) {
        patterns.add("springfox/documentation/**");
        patterns.add("io/swagger/**");
        patterns.add("org/springdoc/**");
    }

    protected void addDbDriver(Set<String> patterns) {
        // mysql
        patterns.add("com/mysql/**");
        // oracle
        patterns.add("oracle/jdbc/**");
        // sqlserver
        patterns.add("com/microsoft/jdbc/sqlserver/**");
        // DB2
        patterns.add("com/ibm/db2/jdbc/**");
        // DB2/AS400
        patterns.add("com/ibm/as400/**");
        // Informix
        patterns.add("com/informix/jdbc/**");
        // Hypersonic
        patterns.add("org/hsql/**");
        // MS SQL
        patterns.add("com/microsoft/jdbc/**");
        // Postgres
        patterns.add("org/postgresql/**");
        // Sybase
        patterns.add("com/sybase/jdbc2/**");
        // Weblogic
        patterns.add("weblogic/jdbc/**");
        // h2
        patterns.add("jdbc/h2/**");
    }

    protected void addMainDependencyFramework(Set<String> patterns) {
        patterns.add("com/github/benmanes/caffeine/cache/**");
    }

    protected void addIdea(Set<String> patterns) {
        // idea debug agent
        patterns.add("com/intellij/rt/debugger/agent/**");
    }

    protected void addLog(Set<String> patterns) {
        if(Boolean.FALSE.equals(configuration.pluginFollowLog())){
            return;
        }
        patterns.add("org/slf4j/**");
    }


    /**
     * 获取扩展鐨?MainResourcePatternDefiner
     *
     * @return List
     */
    private List<MainResourcePatternDefiner> getExtensionPatternDefiners() {
        return SpringBeanUtils.getBeans(applicationContext, MainResourcePatternDefiner.class);
    }

}

