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

package com.zqzqq.bootkits.bootstrap;

import com.zqzqq.bootkits.bootstrap.coexist.CoexistAllowAutoConfiguration;
import com.zqzqq.bootkits.common.PluginDisableAutoConfig;
import com.zqzqq.bootkits.loader.launcher.DevelopmentModeSetting;
import com.zqzqq.bootkits.utils.ObjectUtils;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 插件禁用AutoConfiguration
 *
 * @author starBlues
 * @version 3.1.0
 * @since 3.0.3
 */
public class PluginDisableAutoConfiguration implements AutoConfigurationImportFilter {

    private static final ThreadLocal<Boolean> LAUNCH_PLUGIN = new ThreadLocal<Boolean>();

    public PluginDisableAutoConfiguration(){

    }

    public static void setLaunchPlugin() {
        LAUNCH_PLUGIN.set(true);
    }

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        if(DevelopmentModeSetting.isolation()){
            return new IsolationDisableAutoConfiguration().match(autoConfigurationClasses, autoConfigurationMetadata);
        } else if(DevelopmentModeSetting.coexist()){
            return new CoexistDisableAutoConfiguration().match(autoConfigurationClasses, autoConfigurationMetadata);
        } else {
            boolean[] permitAll = new boolean[autoConfigurationClasses.length];
            for (int i = 0; i < autoConfigurationClasses.length; i++) {
                permitAll[i] = permit(
                        PluginDisableAutoConfig.getCommonPluginDisableAutoConfig(),
                        autoConfigurationClasses[i]);
            }
            return permitAll;
        }
    }

    private static boolean permit(Collection<String> disableCollection, String className){
        if(ObjectUtils.isEmpty(className)){
            return true;
        }
        for (String disableFuzzyClass : disableCollection) {
            if (className.contains(disableFuzzyClass)) {
                return false;
            }
        }
        return true;
    }

    private static class IsolationDisableAutoConfiguration implements AutoConfigurationImportFilter{

        private final List<String> disableFuzzyClass = new ArrayList<>();

        IsolationDisableAutoConfiguration(){
            addDisableFuzzyClasses();
        }

        private void addDisableFuzzyClasses() {
            disableFuzzyClass.add("org.springframework.boot.autoconfigure.http");
            disableFuzzyClass.add("org.springframework.boot.autoconfigure.web");
            disableFuzzyClass.add("org.springframework.boot.autoconfigure.websocket");
            disableFuzzyClass.add("org.springframework.boot.autoconfigure.jackson");
            disableFuzzyClass.add("org.springframework.boot.autoconfigure.webservices");
            disableFuzzyClass.addAll(PluginDisableAutoConfig.getCommonPluginDisableAutoConfig());
        }

        @Override
        public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
            boolean[] match = new boolean[autoConfigurationClasses.length];
            for (int i = 0; i < autoConfigurationClasses.length; i++) {
                String autoConfigurationClass = autoConfigurationClasses[i];
                if(autoConfigurationClass == null || "".equals(autoConfigurationClass)){
                    continue;
                }
                match[i] = permit(disableFuzzyClass, autoConfigurationClass);
            }
            return match;
        }
    }


    private static class CoexistDisableAutoConfiguration implements AutoConfigurationImportFilter{

        public CoexistDisableAutoConfiguration(){

        }

        @Override
        public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
            Boolean launchPlugin = LAUNCH_PLUGIN.get();
            boolean[] match = new boolean[autoConfigurationClasses.length];
            try {
                if(launchPlugin != null && launchPlugin){
                    CoexistAllowAutoConfiguration configuration = SpringPluginBootstrapBinder.get()
                            .getCoexistAllowAutoConfiguration();
                    for (int i = 0; i < autoConfigurationClasses.length; i++) {
                        String autoConfigurationClass = autoConfigurationClasses[i];
                        if(ObjectUtils.isEmpty(autoConfigurationClass)){
                            continue;
                        }
                        if(permit(PluginDisableAutoConfig.getCommonPluginDisableAutoConfig(), autoConfigurationClass)){
                            match[i] = configuration.match(autoConfigurationClass);
                        } else {
                            match[i] = false;
                        }
                    }
                    return match;
                } else {
                    for (int i = 0; i < autoConfigurationClasses.length; i++) {
                        match[i] = true;
                    }
                }
                return match;
            } finally {
                LAUNCH_PLUGIN.remove();
            }
        }
    }

}