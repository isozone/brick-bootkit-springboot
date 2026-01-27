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
 * 插件的启动引导器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.2
 */
public abstract class AbstractLauncher<R> implements Launcher<R> {

    private volatile boolean classLoaderRestored = false;

    @Override
    public R run(String... args) throws Exception {
        ClassLoader classLoader = createClassLoader(args);
        Thread thread = Thread.currentThread();
        ClassLoader oldClassLoader  = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(classLoader);
            return runWithContextClassLoader(classLoader, args);
        } finally {
            restoreContextClassLoader(oldClassLoader);
        }
    }

    /**
     * 在classloader上下文中运行
     * @param classLoader classLoader
     * @param args 参数
     * @return 运行结果
     * @throws Exception 异常
     */
    protected R runWithContextClassLoader(ClassLoader classLoader, String... args) throws Exception {
        return launch(classLoader, args);
    }

    /**
     * 恢复classloader上下文
     * @param oldClassLoader 原来的classLoader
     */
    protected void restoreContextClassLoader(ClassLoader oldClassLoader) {
        Thread thread = Thread.currentThread();
        thread.setContextClassLoader(oldClassLoader);
        classLoaderRestored = true;
    }

    /**
     * 检查类加载器是否已恢复
     * @return true 如果已恢复，false 如果未恢复
     */
    protected boolean isClassLoaderRestored() {
        return classLoaderRestored;
    }

    /**
     * 重置类加载器恢复状态
     */
    protected void resetClassLoaderRestored() {
        this.classLoaderRestored = false;
    }

    /**
     * 创建classloader
     * @param args 参数
     * @return ClassLoader
     * @throws Exception 创建异常
     */
    protected abstract ClassLoader createClassLoader(String... args) throws Exception;

    /**
     * 子类实现具体的启动方法
     * @param classLoader 当前的ClassLoader
     * @param args 启动参数
     * @return 启动返回值
     * @throws Exception 启动异常
     */
    protected abstract R launch(ClassLoader classLoader, String... args) throws Exception;

}