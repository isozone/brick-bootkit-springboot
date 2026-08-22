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



package com.zqzqq.bootkits.bootstrap.realize;

import com.zqzqq.bootkits.bootstrap.AutowiredTypeDefinerConfig;
import com.zqzqq.bootkits.bootstrap.annotation.AutowiredType;
import com.zqzqq.bootkits.utils.UrlUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * autowiredType 批量定义接口
 *
 * @author starBlues
 * @since 3.0.3
 * @version 3.0.3
 */
public interface AutowiredTypeDefiner {

    /**
     * 定义 ClassDefiner
     * @param config 往config中进行配置ClassDefiner
     */
    void config(AutowiredTypeDefinerConfig config);


    @Getter
    @EqualsAndHashCode
    class ClassDefiner{
        /**
         * 注入类型
         */
        private final AutowiredType.Type autowiredType;

        /**
         * 类名模式匹配
         */
        private final Set<String> classNamePatterns;

        private ClassDefiner(AutowiredType.Type autowiredType, Set<String> classNamePatterns){
            this.autowiredType = autowiredType;
            this.classNamePatterns = classNamePatterns;
        }

        public static ClassDefiner config(AutowiredType.Type autowiredType, Class<?>... classes){
            if(autowiredType == null){
                throw new IllegalArgumentException("autowiredType 参数不能为空");
            }
            int length = classes.length;
            if(length == 0){
                throw new IllegalArgumentException("classes 参数不能为空");
            }
            Set<String> classNamePatterns = new HashSet<>(length);
            for (Class<?> aClass : classes) {
                classNamePatterns.add(UrlUtils.formatMatchUrl(aClass.getName()));
            }
            return new ClassDefiner(autowiredType, classNamePatterns);
        }

        public static ClassDefiner config(AutowiredType.Type autowiredType, String... classNamePatterns){
            if(autowiredType == null){
                throw new IllegalArgumentException("autowiredType 参数不能为空");
            }
            int length = classNamePatterns.length;
            if(length == 0){
                throw new IllegalArgumentException("classNamePatterns 参数不能为空");
            }
            Set<String> classNamePatternsSet = new HashSet<>(length);
            for (String classNamePattern : classNamePatterns) {
                classNamePatternsSet.add(UrlUtils.formatMatchUrl(classNamePattern));
            }
            return new ClassDefiner(autowiredType, classNamePatternsSet);
        }

    }


}

