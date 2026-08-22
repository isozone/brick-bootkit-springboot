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


package com.zqzqq.bootkits.web.demo;

import com.zqzqq.bootkits.loader.launcher.SpringBootstrap;
import com.zqzqq.bootkits.loader.launcher.SpringMainBootstrap;
import com.zqzqq.bootkits.web.annotation.EnableBrickWeb;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * Brick Web 管理控制台测试应用
 *
 * @author brick-bootkit
 */
@SpringBootApplication(
    scanBasePackages = {"com.zqzqq.bootkits.**","com.zqzqq.bootkits.web.demo.**"},
    exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
    }
)
@EnableBrickWeb
@EnableScheduling
public class DemoApplication  implements SpringBootstrap {

    public static void main(String[] args) {
        SpringMainBootstrap.launch(DemoApplication.class, args);
    }

    @Override
    public void run(String[] args){
        SpringApplication application = new SpringApplicationBuilder(DemoApplication.class).build(args);
        application.run(args);
    }
}
