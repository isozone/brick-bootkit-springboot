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

/**
 * SpringPluginBootstrap 实例绑定
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.0
 */
public class SpringPluginBootstrapBinder {

    private final static ThreadLocal<SpringPluginBootstrap> BINDER = new ThreadLocal<>();


    public static SpringPluginBootstrap get(){
        return BINDER.get();
    }

    public static void set(SpringPluginBootstrap bootstrap){
        BINDER.set(bootstrap);
    }

    public static void remove(){
        BINDER.remove();
    }


}