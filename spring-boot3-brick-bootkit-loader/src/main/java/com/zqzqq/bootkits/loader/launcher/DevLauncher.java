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



package com.zqzqq.bootkits.loader.launcher;

import com.zqzqq.bootkits.loader.launcher.coexist.CoexistBaseLauncher;
import com.zqzqq.bootkits.loader.launcher.isolation.IsolationBaseLauncher;
import com.zqzqq.bootkits.loader.launcher.runner.MethodRunner;

/**
 * 开发环境launcher
 *
 * @author starBlues
 * @since 3.0.4
 * @version 4.0.0
 */
public class DevLauncher implements Launcher<ClassLoader>{

    private final SpringBootstrap springBootstrap;
    
    public DevLauncher(SpringBootstrap springBootstrap) {
        this.springBootstrap = springBootstrap;
    }

    @Override
    public ClassLoader run(String... args) throws Exception {
        MethodRunner methodRunner = new MethodRunner(springBootstrap.getClass().getName(),
                SPRING_BOOTSTRAP_RUN_METHOD, args);
        AbstractMainLauncher launcher;
        if(DevelopmentModeSetting.isolation()){
            launcher = new IsolationBaseLauncher(methodRunner);
        } else if(DevelopmentModeSetting.coexist()) {
            launcher = new CoexistBaseLauncher(methodRunner);
        } else {
            throw DevelopmentModeSetting.getUnknownModeException();
        }
        return launcher.run(args);
    }
}