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

import com.zqzqq.bootkits.bootstrap.listener.PluginApplicationWebEventListener;
import com.zqzqq.bootkits.bootstrap.processor.ProcessorContext;
import com.zqzqq.bootkits.spring.environment.EnvironmentProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;

/**
 * 主程序应为 web 类型插件创建时的插件 ApplicationContext
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.3
 */
public class PluginWebApplicationContext extends PluginApplicationContext implements WebServerApplicationContext {

    private final WebServer webServer;
    private final String serverNamespace;

    public PluginWebApplicationContext(DefaultListableBeanFactory beanFactory, ProcessorContext processorContext) {
        super(beanFactory, processorContext);
        this.webServer = new PluginSimulationWebServer(processorContext);
        this.serverNamespace = processorContext.getPluginDescriptor().getPluginId();
        addApplicationListener(new PluginApplicationWebEventListener(this));
    }

    @Override
    public WebServer getWebServer() {
        return webServer;
    }

    @Override
    public String getServerNamespace() {
        return serverNamespace;
    }


    public static class PluginSimulationWebServer implements WebServer {

        private final int port;

        public PluginSimulationWebServer(ProcessorContext processorContext) {
            EnvironmentProvider provider = processorContext.getMainApplicationContext().getEnvironmentProvider();
            Integer port = provider.getInteger("server.port");
            if(port == null){
                this.port = -1;
            } else {
                this.port = port;
            }
        }

        @Override
        public void start() throws WebServerException {
            throw new InvalidWebServerException();
        }

        @Override
        public void stop() throws WebServerException {
            throw new InvalidWebServerException();
        }

        @Override
        public int getPort() {
            return port;
        }

    }

    public static class InvalidWebServerException extends WebServerException{

        public InvalidWebServerException() {
            super("Invalid operation", null);
        }
    }

}

