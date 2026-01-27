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

package com.zqzqq.bootkits.bootstrap.coexist;

import com.zqzqq.bootkits.utils.ObjectUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Coexist模式下存放当前插件允许的 auto 配置
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.0
 */
public class CoexistAllowAutoConfiguration {

    private final Set<String> allowPrefix = new HashSet<>();

    public CoexistAllowAutoConfiguration(){
        addDefault();
    }

    private void addDefault(){
        allowPrefix.add("org.springframework.boot.autoconfigure.aop.AopAutoConfiguration");
    }

    public CoexistAllowAutoConfiguration add(String autoConfigurationClass){
        if(ObjectUtils.isEmpty(autoConfigurationClass)){
            return this;
        }
        allowPrefix.add(autoConfigurationClass);
        return this;
    }

    public boolean match(String autoConfigurationClass){
        for (String prefix : allowPrefix) {
            if(autoConfigurationClass.startsWith(prefix)){
                return true;
            }
        }
        return false;
    }

}