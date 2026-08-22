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


package com.zqzqq.bootkits.web.scripts.config;

import com.zqzqq.bootkits.scripts.core.ScriptManager;
import com.zqzqq.bootkits.scripts.core.impl.DefaultScriptManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 脚本管理器配置
 * 提供 ScriptManager Bean
 * 
 * @author brick-bootkit
 */
@Slf4j
@Configuration
public class ScriptManagerConfig {
    
    /**
     * 创建脚本管理器 Bean
     */
    @Bean
    public ScriptManager scriptManager() {
        log.info("Initializing ScriptManager...");
        try {
            DefaultScriptManager manager = new DefaultScriptManager();
            manager.initialize();
            log.info("ScriptManager initialized successfully");
            return manager;
        } catch (Exception e) {
            log.error("Failed to initialize ScriptManager", e);
            throw new RuntimeException("Failed to initialize ScriptManager", e);
        }
    }
}
