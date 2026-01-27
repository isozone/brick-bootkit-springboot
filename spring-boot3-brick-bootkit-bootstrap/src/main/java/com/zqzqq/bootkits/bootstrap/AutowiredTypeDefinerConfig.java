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

import com.zqzqq.bootkits.bootstrap.annotation.AutowiredType;
import com.zqzqq.bootkits.bootstrap.realize.AutowiredTypeDefiner;

import java.util.HashSet;
import java.util.Set;

/**
 * 配置 ClassDefiner
 *
 * @author starBlues
 * @since 3.0.3
 * @version 3.0.3
 */
public class AutowiredTypeDefinerConfig {

    private final Set<AutowiredTypeDefiner.ClassDefiner> classDefiners;

    public AutowiredTypeDefinerConfig(){
        this.classDefiners = new HashSet<>();
    }

    Set<AutowiredTypeDefiner.ClassDefiner> getClassDefiners(){
        return classDefiners;
    }

    public AutowiredTypeDefinerConfig add(AutowiredType.Type type, String... classNamePatterns){
        if(type != null && classNamePatterns != null && classNamePatterns.length > 0){
            classDefiners.add(AutowiredTypeDefiner.ClassDefiner.config(type, classNamePatterns));
        }
        return this;
    }

    public AutowiredTypeDefinerConfig add(AutowiredType.Type type, Class<?>... classes){
        if(type != null && classes != null && classes.length > 0){
            classDefiners.add(AutowiredTypeDefiner.ClassDefiner.config(type, classes));
        }
        return this;
    }

}