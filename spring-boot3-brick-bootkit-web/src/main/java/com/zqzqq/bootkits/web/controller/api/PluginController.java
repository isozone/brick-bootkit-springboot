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


package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.*;
import com.zqzqq.bootkits.web.service.DemoPluginService;
import com.zqzqq.bootkits.web.service.PluginWebService;
import com.zqzqq.bootkits.web.service.SimplePluginService;
import com.zqzqq.bootkits.web.service.UploadHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 插件管理 API 控制器
 * 
 * @author brick-bootkit
 */
@Slf4j
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/plugins")
@Tag(name = "插件管理", description = "插件的增删改查和生命周期管理接口")
public class PluginController {

    private final ObjectProvider<PluginWebService> pluginWebServiceProvider;
    private final ObjectProvider<SimplePluginService> simplePluginServiceProvider;
    private final ObjectProvider<DemoPluginService> demoPluginServiceProvider;
    private final PluginWebAuthorizationService authorizationService;
    
    @Autowired
    private UploadHistoryService uploadHistoryService;

    public PluginController(
            ObjectProvider<PluginWebService> pluginWebServiceProvider, 
            ObjectProvider<SimplePluginService> simplePluginServiceProvider,
            ObjectProvider<DemoPluginService> demoPluginServiceProvider,
            PluginWebAuthorizationService authorizationService) {
        this.pluginWebServiceProvider = pluginWebServiceProvider;
        this.simplePluginServiceProvider = simplePluginServiceProvider;
        this.demoPluginServiceProvider = demoPluginServiceProvider;
        this.authorizationService = authorizationService;
    }

    private void authorize(PluginWebPermission permission) {
        authorizationService.check(permission, null);
    }

    private void authorize(PluginWebPermission permission, String pluginId) {
        authorizationService.check(permission, pluginId);
    }

    /**
     * 获取 SimplePluginService 实例
     */
    private SimplePluginService getSimplePluginService() {
        SimplePluginService service = simplePluginServiceProvider.getIfAvailable();
        if (service == null) {
            throw new PluginException("插件功能未启用，请在完整brick-bootkit环境中使用");
        }
        return service;
    }

    /**
     * 获取插件列表
     */
    @GetMapping
    @Operation(summary = "获取插件列表")
    public ApiResult<PageResult<PluginDTO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false, defaultValue = "all") String state,
            @RequestParam(required = false) String keyword) {
        authorize(PluginWebPermission.PLUGIN_VIEW);
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            return ApiResult.success(pluginWebService.listPlugins(page, size, state, keyword));
        }
        DemoPluginService demoService = demoPluginServiceProvider.getIfAvailable();
        if (demoService != null) {
            return ApiResult.success(demoService.listPlugins(page, size, state, keyword));
        }
        return ApiResult.success(getSimplePluginService().listPlugins(page, size, state, keyword));
    }

    /**
     * 获取所有插件列表（不分页）
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有插件")
    public ApiResult<List<PluginDTO>> getAllPlugins() {
        authorize(PluginWebPermission.PLUGIN_VIEW);
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            return ApiResult.success(pluginWebService.getAllPlugins());
        }
        DemoPluginService demoService = demoPluginServiceProvider.getIfAvailable();
        if (demoService != null) {
            return ApiResult.success(demoService.getAllPlugins());
        }
        return ApiResult.success(getSimplePluginService().getAllPlugins());
    }

    /**
     * 获取插件详情
     */
    @GetMapping("/{pluginId}")
    @Operation(summary = "获取插件详情")
    public ApiResult<PluginDetailDTO> detail(@PathVariable String pluginId) {
        authorize(PluginWebPermission.PLUGIN_VIEW, pluginId);
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            return ApiResult.success(pluginWebService.getPluginDetail(pluginId));
        }
        DemoPluginService demoService = demoPluginServiceProvider.getIfAvailable();
        if (demoService != null) {
            PluginDetailDTO detail = demoService.getDetail(pluginId);
            if (detail != null) {
                return ApiResult.success(detail);
            }
        }
        return ApiResult.success(getSimplePluginService().getPluginDetail(pluginId));
    }

    /**
     * 上传插件（第一阶段：只保存到临时目录）
     */
    @PostMapping("/upload/temp")
    @Operation(summary = "上传插件到临时目录")
    public ApiResult<String> uploadTemp(@RequestParam("file") MultipartFile file) {
        authorize(PluginWebPermission.PLUGIN_UPLOAD);
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            return pluginWebService.uploadPluginTemp(file);
        }
        DemoPluginService demoService = demoPluginServiceProvider.getIfAvailable();
        if (demoService != null) {
            return ApiResult.error(500, "Demo环境不支持临时上传");
        }
        return ApiResult.error(500, "插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 安装插件（第二阶段：从临时目录安装）
     */
    @PostMapping("/install/temp")
    @Operation(summary = "从临时目录安装插件")
    public ApiResult<PluginDTO> installFromTemp(@RequestBody PluginInstallRequest request) {
        authorize(PluginWebPermission.PLUGIN_INSTALL);
        if (request.getTempFilePath() == null) {
            return ApiResult.error(400, "请提供临时文件路径 (tempFilePath)");
        }
        
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            return pluginWebService.installPluginFromTemp(request.getTempFilePath(), request.getAutoStart());
        }
        DemoPluginService demoService = demoPluginServiceProvider.getIfAvailable();
        if (demoService != null) {
            return ApiResult.error(500, "Demo环境不支持临时安装");
        }
        return ApiResult.error(500, "插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 上传插件（整合版：上传+安装一次完成）
     */
    @PostMapping("/upload")
    @Operation(summary = "上传并安装插件")
    public ApiResult<PluginDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "autoStart", required = false, defaultValue = "false") Boolean autoStart,
            @RequestParam(name = "overwrite", required = false, defaultValue = "true") Boolean overwrite) {
        authorize(PluginWebPermission.PLUGIN_UPLOAD);
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            ApiResult<PluginDTO> result = pluginWebService.uploadPlugin(file, autoStart);
            // 如果 overwrite 为 false 且上传失败，返回错误
            if (!overwrite && result.getCode() != 200) {
                return result;
            }
            return result;
        }
        DemoPluginService demoService = demoPluginServiceProvider.getIfAvailable();
        if (demoService != null) {
            return demoService.uploadPlugin(file, autoStart);
        }
        return ApiResult.error(500, "插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 安装插件
     */
    @PostMapping("/install")
    @Operation(summary = "安装插件")
    public ApiResult<PluginDTO> install(@RequestBody PluginInstallRequest request) {
        authorize(PluginWebPermission.PLUGIN_INSTALL);
        if (request == null || request.getPluginPath() == null || request.getPluginPath().trim().isEmpty()) {
            return ApiResult.error(400, "请提供插件路径 (pluginPath)");
        }
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            final Path pluginPath;
            try {
                pluginPath = Paths.get(request.getPluginPath());
            } catch (InvalidPathException e) {
                return ApiResult.error(400, "插件路径不合法: " + e.getMessage());
            }
            PluginDTO pluginDTO = pluginWebService.installPlugin(pluginPath);
            
            // 如果 autoStart 为 true，安装后自动启动
            if (Boolean.TRUE.equals(request.getAutoStart())) {
                pluginWebService.startPlugin(pluginDTO.getPluginId());
                log.info("插件已自动启动: {}", pluginDTO.getPluginId());
            }
            return ApiResult.success(pluginDTO);
        }
        DemoPluginService demoService = demoPluginServiceProvider.getIfAvailable();
        if (demoService != null) {
            return ApiResult.success(demoService.installPlugin(Paths.get(request.getPluginPath())));
        }
        return ApiResult.error(500, "插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 启动插件
     */
    @PostMapping("/{pluginId}/start")
    @Operation(summary = "启动插件")
    public ApiResult<Void> start(@PathVariable String pluginId) {
        authorize(PluginWebPermission.PLUGIN_START, pluginId);
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            pluginWebService.startPlugin(pluginId);
            return ApiResult.success();
        }
        DemoPluginService demoService = demoPluginServiceProvider.getIfAvailable();
        if (demoService != null) {
            demoService.startPlugin(pluginId);
            return ApiResult.success();
        }
        return ApiResult.error(500, "插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 停止插件
     */
    @PostMapping("/{pluginId}/stop")
    @Operation(summary = "停止插件")
    public ApiResult<Void> stop(@PathVariable String pluginId) {
        authorize(PluginWebPermission.PLUGIN_STOP, pluginId);
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            pluginWebService.stopPlugin(pluginId);
            return ApiResult.success();
        }
        DemoPluginService demoService = demoPluginServiceProvider.getIfAvailable();
        if (demoService != null) {
            demoService.stopPlugin(pluginId);
            return ApiResult.success();
        }
        return ApiResult.error(500, "插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 重启插件
     */
    @PostMapping("/{pluginId}/restart")
    @Operation(summary = "重启插件")
    public ApiResult<Void> restart(@PathVariable String pluginId) {
        authorize(PluginWebPermission.PLUGIN_RESTART, pluginId);
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            pluginWebService.restartPlugin(pluginId);
            return ApiResult.success();
        }
        DemoPluginService demoService = demoPluginServiceProvider.getIfAvailable();
        if (demoService != null) {
            demoService.restartPlugin(pluginId);
            return ApiResult.success();
        }
        return ApiResult.error(500, "插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 卸载插件
     */
    @DeleteMapping("/{pluginId}")
    @Operation(summary = "卸载插件")
    public ApiResult<Void> uninstall(@PathVariable String pluginId) {
        authorize(PluginWebPermission.PLUGIN_UNINSTALL, pluginId);
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            pluginWebService.uninstallPlugin(pluginId);
            return ApiResult.success();
        }
        DemoPluginService demoService = demoPluginServiceProvider.getIfAvailable();
        if (demoService != null) {
            demoService.uninstallPlugin(pluginId);
            return ApiResult.success();
        }
        return ApiResult.error(500, "插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 下载插件
     */
    @GetMapping("/{pluginId}/download")
    @Operation(summary = "下载插件")
    public ResponseEntity<Resource> download(@PathVariable String pluginId) {
        authorize(PluginWebPermission.PLUGIN_DOWNLOAD, pluginId);
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = pluginWebService.getPluginResource(pluginId);
        PluginDetailDTO plugin = pluginWebService.getPluginDetail(pluginId);
        
        String filename = plugin.getName() + "-" + plugin.getVersion() + ".jar";
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * 验证插件
     */
    @PostMapping("/verify")
    @Operation(summary = "验证插件")
    public ApiResult<PluginVerifyResult> verify(@RequestBody PluginVerifyRequest request) {
        authorize(PluginWebPermission.PLUGIN_VERIFY);
        if (request == null || request.getPluginPath() == null || request.getPluginPath().trim().isEmpty()) {
            return ApiResult.error(400, "请提供插件路径 (pluginPath)");
        }
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            final Path pluginPath;
            try {
                pluginPath = Paths.get(request.getPluginPath());
            } catch (InvalidPathException e) {
                return ApiResult.error(400, "插件路径不合法: " + e.getMessage());
            }
            boolean valid = pluginWebService.verifyPlugin(pluginPath);
            return ApiResult.success(new PluginVerifyResult(valid, valid ? "插件验证通过" : "插件验证失败"));
        }
        DemoPluginService demoService = demoPluginServiceProvider.getIfAvailable();
        if (demoService != null) {
            boolean valid = demoService.verifyPlugin(Paths.get(request.getPluginPath()));
            return ApiResult.success(new PluginVerifyResult(valid, valid ? "插件验证通过" : "插件验证失败"));
        }
        return ApiResult.error(500, "插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 插件安装请求
     */
    @Data
    public static class PluginInstallRequest {
        private String pluginPath;
        /**
         * 临时文件路径（用于两阶段安装）
         */
        private String tempFilePath;
        /**
         * 安装后是否自动启动
         */
        private Boolean autoStart;
        /**
         * 是否覆盖已存在的插件（用于上传场景）
         */
        private Boolean overwrite;
    }

    /**
     * 插件验证请求
     */
    @Data
    public static class PluginVerifyRequest {
        private String pluginPath;
    }

    /**
     * 插件验证结果
     */
    @Data
    @AllArgsConstructor
    public static class PluginVerifyResult {
        private boolean valid;
        private String message;
    }

    /**
     * 获取上传历史列表
     */
    @GetMapping("/upload-history")
    @Operation(summary = "获取插件上传历史")
    public ApiResult<PageResult<PluginUploadHistory>> getUploadHistory(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "pluginId", required = false) String pluginId,
            @RequestParam(value = "status", required = false) PluginUploadHistory.UploadStatus status,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        authorize(PluginWebPermission.PLUGIN_HISTORY_READ, pluginId);
        PageResult<PluginUploadHistory> result = uploadHistoryService.queryHistory(
                page, size, pluginId, status, startDate, endDate);
        return ApiResult.success(result);
    }

    /**
     * 获取所有上传历史
     */
    @GetMapping("/upload-history/all")
    @Operation(summary = "获取所有上传历史")
    public ApiResult<List<PluginUploadHistory>> getAllUploadHistory() {
        authorize(PluginWebPermission.PLUGIN_HISTORY_READ);
        List<PluginUploadHistory> history = uploadHistoryService.getAllHistory();
        return ApiResult.success(history);
    }

    /**
     * 根据上传ID获取历史记录
     */
    @GetMapping("/upload-history/{uploadId}")
    @Operation(summary = "根据上传ID获取历史记录")
    public ApiResult<PluginUploadHistory> getUploadHistoryById(@PathVariable String uploadId) {
        authorize(PluginWebPermission.PLUGIN_HISTORY_READ);
        PluginUploadHistory history = uploadHistoryService.getHistoryById(uploadId);
        if (history == null) {
            return ApiResult.error(404, "上传历史记录不存在");
        }
        return ApiResult.success(history);
    }

    /**
     * 删除指定的上传历史记录
     */
    @DeleteMapping("/upload-history/{uploadId}")
    @Operation(summary = "删除上传历史记录")
    public ApiResult<Void> deleteUploadHistory(@PathVariable String uploadId) {
        authorize(PluginWebPermission.PLUGIN_HISTORY_WRITE);
        boolean deleted = uploadHistoryService.deleteHistory(uploadId);
        if (!deleted) {
            return ApiResult.error(404, "上传历史记录不存在");
        }
        return ApiResult.success();
    }

    /**
     * 批量删除指定日期之前的上传历史记录
     */
    @DeleteMapping("/upload-history")
    @Operation(summary = "批量删除上传历史记录")
    public ApiResult<Integer> deleteUploadHistoryBefore(
            @Parameter(description = "删除指定日期之前的记录") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beforeDate) {
        authorize(PluginWebPermission.PLUGIN_HISTORY_WRITE);
        int deletedCount = uploadHistoryService.deleteHistoryBefore(beforeDate);
        return ApiResult.success(deletedCount);
    }

    /**
     * 清空所有上传历史记录
     */
    @DeleteMapping("/upload-history/all")
    @Operation(summary = "清空所有上传历史")
    public ApiResult<Void> clearAllUploadHistory() {
        authorize(PluginWebPermission.PLUGIN_HISTORY_WRITE);
        uploadHistoryService.clearAllHistory();
        return ApiResult.success();
    }

    /**
     * 当前用户插件管理能力集合
     */
    @GetMapping("/auth/capabilities")
    @Operation(summary = "获取当前用户插件管理能力")
    public ApiResult<AuthCapabilities> capabilities(@RequestParam(value = "pluginId", required = false) String pluginId) {
        authorize(PluginWebPermission.PLUGIN_VIEW, pluginId);
        AuthCapabilities response = new AuthCapabilities(
                authorizationService.getMode().name().toLowerCase(),
                authorizationService.currentPrincipal(),
                authorizationService.getCapabilities(pluginId)
        );
        return ApiResult.success(response);
    }

    @Data
    @AllArgsConstructor
    public static class AuthCapabilities {
        private String mode;
        private String principal;
        private Map<String, Boolean> permissions;
    }
}
