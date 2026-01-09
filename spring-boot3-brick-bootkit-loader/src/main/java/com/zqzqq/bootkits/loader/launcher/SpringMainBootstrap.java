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

import com.zqzqq.bootkits.loader.jar.JarFile;
import com.zqzqq.bootkits.loader.launcher.runner.MainMethodRunner;
import com.zqzqq.bootkits.loader.launcher.runner.MethodRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * 主程序引导器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class SpringMainBootstrap {

    private static final Logger log = LoggerFactory.getLogger(SpringMainBootstrap.class);
    static final String MAIN_RUN_METHOD = "main";

    private static final CountDownLatch COUNT_DOWN_LATCH = new CountDownLatch(1);

    private static SpringBootstrap springBootstrap;

    public static void launch(Class<? extends SpringBootstrap> bootstrapClass, String[] args) {
        try {
            SpringBootstrap springBootstrap = bootstrapClass.getConstructor().newInstance();
            launch(springBootstrap, args);
        } catch (Exception e) {
            log.error("Failed to launch bootstrap class: {}", bootstrapClass.getName(), e);
        }
    }

    public static void launch(SpringBootstrap springBootstrap, String[] args) {
        SpringMainBootstrap.springBootstrap = Objects.requireNonNull(springBootstrap, "springBootBootstrap 不能为空");
        DevelopmentModeSetting.setDevelopmentMode(springBootstrap.developmentMode());
        MainMethodRunner mainMethodRunner = new MainMethodRunner(SpringMainBootstrap.class.getName(),
                MAIN_RUN_METHOD, args);
        JarFile.registerUrlProtocolHandler();
        Thread launchThread = new Thread(new Runner(mainMethodRunner));
        launchThread.start();
        try {
            COUNT_DOWN_LATCH.await();
        } catch (InterruptedException e) {
            log.error("Launch thread interrupted", e);
        }
    }

    private static class Runner implements Runnable{

        private final MethodRunner methodRunner;

        public Runner(MethodRunner methodRunner) {
            this.methodRunner = methodRunner;
        }

        @Override
        public void run() {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                methodRunner.run(contextClassLoader);
            } catch (Exception e) {
                log.error("Failed to run method in launch thread", e);
            } finally {
                COUNT_DOWN_LATCH.countDown();
            }
        }
    }

    private static void main(String[] args) throws Exception {
        Objects.requireNonNull(springBootstrap, "springBootBootstrap 不能为空");
        Launcher<ClassLoader> launcher = new DevLauncher(springBootstrap);
        launcher.run(args);
    }

}