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

package com.zqzqq.bootkits.loader.launcher;

/**
 * 启动上下文
 *
 * @author starBlues
 * @since 3.0.0
 * @version 4.0.0
 */
public class LauncherContext {

    private static volatile ClassLoader mainClassLoader = null;

    /**
     * 获取主程序包的ClassLoader
     * @return 主程序包ClassLoader
     */
    public static ClassLoader getMainClassLoader(){
        return mainClassLoader;
    }

    /**
     * 设置主程序包的ClassLoader
     * @param classLoader 主程序包ClassLoader
     */
    static void setMainClassLoader(ClassLoader classLoader){
        if(mainClassLoader == null){
            synchronized (AbstractLauncher.class){
                if(mainClassLoader == null){
                    mainClassLoader = classLoader;
                }
            }
        }
    }


}