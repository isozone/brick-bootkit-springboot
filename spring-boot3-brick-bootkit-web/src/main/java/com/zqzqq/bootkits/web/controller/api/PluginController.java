package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.web.dto.*;
import com.zqzqq.bootkits.web.service.DemoPluginService;
import com.zqzqq.bootkits.web.service.PluginWebService;
import com.zqzqq.bootkits.web.service.SimplePluginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 插件管理 API 控制器
 * 
 * @author brick-bootkit
 */
@Slf4j
@RestController
@RequestMapping("${brick.web.api-prefix:/brick-web/api}/plugins")
@Tag(name = "插件管理", description = "插件的增删改查和生命周期管理接口")
public class PluginController {

    private final ObjectProvider<PluginWebService> pluginWebServiceProvider;
    private final ObjectProvider<SimplePluginService> simplePluginServiceProvider;
    private final ObjectProvider<DemoPluginService> demoPluginServiceProvider;
    private final ApplicationContext applicationContext;

    public PluginController(
            ObjectProvider<PluginWebService> pluginWebServiceProvider, 
            ObjectProvider<SimplePluginService> simplePluginServiceProvider,
            ObjectProvider<DemoPluginService> demoPluginServiceProvider,
            ApplicationContext applicationContext) {
        this.pluginWebServiceProvider = pluginWebServiceProvider;
        this.simplePluginServiceProvider = simplePluginServiceProvider;
        this.demoPluginServiceProvider = demoPluginServiceProvider;
        this.applicationContext = applicationContext;
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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "all") String state,
            @RequestParam(required = false) String keyword) {
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
     * 上传插件
     */
    @PostMapping("/upload")
    @Operation(summary = "上传插件")
    public ApiResult<PluginDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "enable", required = false, defaultValue = "false") Boolean enable) {
        // 调试日志：检查所有相关bean
        log.info("=== 插件上传调试 ===");
        log.info("1. pluginWebServiceProvider bean exists: {}", pluginWebServiceProvider != null);
        log.info("2. demoPluginServiceProvider bean exists: {}", demoPluginServiceProvider != null);
        
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        log.info("3. PluginWebService bean available: {}", pluginWebService != null);
        if (pluginWebService != null) {
            return pluginWebService.uploadPlugin(file, enable);
        }
        DemoPluginService demoService = demoPluginServiceProvider.getIfAvailable();
        log.info("4. DemoPluginService bean available: {}", demoService != null);
        if (demoService != null) {
            return demoService.uploadPlugin(file, enable);
        }
        
        // 检查 PluginManager 是否存在（通过 applicationContext）
        try {
            PluginManager pluginManager = applicationContext.getBean(PluginManager.class);
            log.info("5. PluginManager bean found via applicationContext: {}", pluginManager != null);
        } catch (Exception e) {
            log.warn("5. PluginManager bean NOT found via applicationContext: {}", e.getMessage());
        }
        
        return ApiResult.error(500, "插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 安装插件
     */
    @PostMapping("/install")
    @Operation(summary = "安装插件")
    public ApiResult<PluginDTO> install(@RequestBody PluginInstallRequest request) {
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            Path pluginPath = Paths.get(request.getPluginPath());
            return ApiResult.success(pluginWebService.installPlugin(pluginPath));
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
        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            Path pluginPath = Paths.get(request.getPluginPath());
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
}