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
