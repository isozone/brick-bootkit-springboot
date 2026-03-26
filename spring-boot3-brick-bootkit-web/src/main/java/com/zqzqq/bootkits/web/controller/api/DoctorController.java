package com.zqzqq.bootkits.web.controller.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.integration.doctor.PluginDoctorExportFormatter;
import com.zqzqq.bootkits.integration.doctor.PluginDoctorReport;
import com.zqzqq.bootkits.integration.doctor.PluginDoctorService;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.dto.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 环境自检接口。
 */
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/doctor")
@Tag(name = "环境自检", description = "宿主环境与插件配置自检接口")
public class DoctorController {

    private final ObjectProvider<PluginDoctorService> doctorServiceProvider;
    private final PluginWebAuthorizationService authorizationService;
    private final ObjectMapper objectMapper;

    public DoctorController(ObjectProvider<PluginDoctorService> doctorServiceProvider,
                            PluginWebAuthorizationService authorizationService,
                            ObjectMapper objectMapper) {
        this.doctorServiceProvider = doctorServiceProvider;
        this.authorizationService = authorizationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Operation(summary = "获取环境自检报告")
    public ApiResult<PluginDoctorReport> report() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        PluginDoctorService doctorService = doctorServiceProvider.getIfAvailable();
        if (doctorService == null) {
            return ApiResult.error(ErrorCode.PLUGIN_DOCTOR_UNAVAILABLE,
                    "Doctor 服务未启用",
                    "PLUGIN_DOCTOR_UNAVAILABLE",
                    "/troubleshooting",
                    "doctor-first");
        }
        return ApiResult.success(doctorService.diagnose());
    }

    @GetMapping("/export/text")
    @Operation(summary = "导出环境自检报告（文本）")
    public ResponseEntity<String> exportText() {
        PluginDoctorReport report = getRequiredDoctorReport();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"doctor-report.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(PluginDoctorExportFormatter.toText(report));
    }

    @GetMapping("/export/json")
    @Operation(summary = "导出环境自检报告（JSON）")
    public ResponseEntity<String> exportJson() throws JsonProcessingException {
        PluginDoctorReport report = getRequiredDoctorReport();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"doctor-report.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }

    private PluginDoctorReport getRequiredDoctorReport() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        PluginDoctorService doctorService = doctorServiceProvider.getIfAvailable();
        if (doctorService == null) {
            throw new IllegalStateException("Doctor 服务未启用");
        }
        return doctorService.diagnose();
    }
}
