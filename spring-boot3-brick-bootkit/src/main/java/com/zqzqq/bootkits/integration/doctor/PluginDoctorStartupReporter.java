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


package com.zqzqq.bootkits.integration.doctor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Logs doctor report after application startup.
 */
public class PluginDoctorStartupReporter implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(PluginDoctorStartupReporter.class);

    private final PluginDoctorService doctorService;

    public PluginDoctorStartupReporter(PluginDoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        PluginDoctorReport report = doctorService.diagnose();
        log.info("Plugin doctor summary: {}", report.getSummary());
        for (PluginDoctorReport.Item item : report.getItems()) {
            if (!"OK".equals(item.getSeverity())) {
                log.info("Plugin doctor [{}] {} - {}", item.getSeverity(), item.getMessage(), item.getSuggestion());
            }
        }
    }
}
