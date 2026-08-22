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



package com.zqzqq.bootkits.loader.launcher.runner;

/**
 * 娑撹崵鈻兼惔蹇旀煙濞夋洖鎯庨崝銊ユ珤
 *
 * @author starBlues
 * @since 3.0.0
 * @version 4.0.0
 */
public class MainMethodRunner extends MethodRunner{

    public MainMethodRunner(String mainClass, String mainRunMethod, String[] args) {
        super(mainClass, mainRunMethod, args);
    }

    @Override
    protected Object getInstance(Class<?> mainClass) throws Exception {
        return null;
    }

    @Override
    public String toString() {
        return "MainMethodRunner{" +
                "mainClass='" + getMainClass() + '\'' +
                ", mainRunMethod='" + getMainRunMethod() + '\'' +
                ", args=" + java.util.Arrays.toString(getArgs()) +
                '}';
    }
}