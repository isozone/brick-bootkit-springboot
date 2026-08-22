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



package com.zqzqq.bootkits.bootstrap;

import com.zqzqq.bootkits.bootstrap.annotation.AutowiredType;
import com.zqzqq.bootkits.bootstrap.processor.ProcessorContext;
import com.zqzqq.bootkits.bootstrap.realize.AutowiredTypeDefiner;
import com.zqzqq.bootkits.utils.UrlUtils;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.Collections;
import java.util.Set;

/**
 * 默认的 AutowiredTypeResolver 实现
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.0
 */
public class DefaultAutowiredTypeResolver implements AutowiredTypeResolver{


    private final Set<AutowiredTypeDefiner.ClassDefiner> classDefiners;
    private final PathMatcher pathMatcher = new AntPathMatcher();


    public DefaultAutowiredTypeResolver(ProcessorContext processorContext) {
        AutowiredTypeDefiner autowiredTypeDefiner = processorContext.getSpringPluginBootstrap().autowiredTypeDefiner();
        if(autowiredTypeDefiner != null){
            AutowiredTypeDefinerConfig definerConfig = new AutowiredTypeDefinerConfig();
            autowiredTypeDefiner.config(definerConfig);
            classDefiners = definerConfig.getClassDefiners();
        } else {
            classDefiners = Collections.emptySet();
        }
    }

    @Override
    public AutowiredType.Type resolve(DependencyDescriptor descriptor){
        String name = descriptor.getDependencyType().getName();
        String classNamePath = UrlUtils.formatMatchUrl(name);
        for (AutowiredTypeDefiner.ClassDefiner classDefiner : classDefiners) {
            Set<String> classNamePatterns = classDefiner.getClassNamePatterns();
            for (String classNamePattern : classNamePatterns) {
                if(pathMatcher.match(classNamePattern, classNamePath)){
                    return classDefiner.getAutowiredType();
                }
            }
        }
        AutowiredType autowiredType = descriptor.getAnnotation(AutowiredType.class);
        if(autowiredType != null){
            return autowiredType.value();
        } else {
            return AutowiredType.Type.PLUGIN;
        }
    }


}