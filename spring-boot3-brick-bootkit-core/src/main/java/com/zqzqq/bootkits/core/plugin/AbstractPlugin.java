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


package com.zqzqq.bootkits.core.plugin;

import com.zqzqq.bootkits.core.logging.PluginLogger;

/**
 * 抽象插件基类
 * 提供插件的基本实现
 */
public abstract class AbstractPlugin implements Plugin {

    private static final PluginLogger logger = PluginLogger.getLogger(AbstractPlugin.class);

    private PluginLoader.PluginMetadata metadata;
    private boolean running = false;

    /**
     * 设置插件元数据
     */
    public void setMetadata(PluginLoader.PluginMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public String getId() {
        return metadata != null ? metadata.getId() : "unknown";
    }

    @Override
    public String getName() {
        return metadata != null ? metadata.getName() : "Unknown Plugin";
    }

    @Override
    public String getVersion() {
        return metadata != null ? metadata.getVersion() : "1.0.0";
    }

    @Override
    public String getDescription() {
        return metadata != null ? metadata.getDescription() : "";
    }

    @Override
    public void start() throws Exception {
        if (running) {
            logger.warn("插件已在运行: {}", getId());
            return;
        }

        logger.info("启动插件: {}", getId());
        doStart();
        running = true;
        logger.info("插件启动成功: {}", getId());
    }

    @Override
    public void stop() throws Exception {
        if (!running) {
            logger.warn("插件未在运行: {}", getId());
            return;
        }

        logger.info("停止插件: {}", getId());
        doStop();
        running = false;
        logger.info("插件停止成功: {}", getId());
    }

    @Override
    public void uninstall() throws Exception {
        if (running) {
            stop();
        }

        logger.info("卸载插件: {}", getId());
        doUninstall();
        logger.info("插件卸载成功: {}", getId());
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 子类实现具体的启动逻辑
     */
    protected abstract void doStart() throws Exception;

    /**
     * 子类实现具体的停止逻辑
     */
    protected abstract void doStop() throws Exception;

    /**
     * 子类实现具体的卸载逻辑
     */
    protected void doUninstall() throws Exception {
        // 默认实现为空，子类可以重写
    }
}