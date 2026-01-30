package com.zqzqq.bootkits.web.scripts.service;

import com.zqzqq.bootkits.scripts.core.ScriptConfiguration;
import com.zqzqq.bootkits.scripts.core.ScriptExecutionResult;
import com.zqzqq.bootkits.scripts.core.ScriptManager;
import com.zqzqq.bootkits.scripts.core.ScriptType;
import com.zqzqq.bootkits.web.scripts.dto.ExecuteRequest;
import com.zqzqq.bootkits.web.scripts.dto.ScriptInfoDTO;
import com.zqzqq.bootkits.web.scripts.dto.ScriptVersionDTO;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 脚本仓库服务
 * 提供脚本的CRUD、版本管理等功能
 * 
 * @author brick-bootkit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptRepositoryService {
    
    private final ScriptStorage scriptStorage;
    private final ScriptManager scriptManager;
    @Lazy
    private final ScriptExecutionService scriptExecutionService;
    
    /**
     * 生成执行ID
     */
    private String generateExecutionId() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 执行脚本
     */
    public Map<String, Object> executeScript(ExecuteRequest request) {
        log.info("Executing script: {}, type: {}", request.getScriptName(), request.getScriptType());
        
        Map<String, Object> result = new java.util.HashMap<>();
        long startTime = System.currentTimeMillis();
        
        // 生成执行ID并记录开始
        String executionId = generateExecutionId();
        String scriptType = request.getScriptType() != null ? request.getScriptType() : "SHELL";
        String paramsStr = request.getParams() != null ? request.getParams().toString() : "";
        scriptExecutionService.recordExecutionStart(request.getScriptName(), scriptType, executionId, "system", paramsStr);
        
        try {
            // 构建参数
            String[] arguments = null;
            if (request.getParams() != null && !request.getParams().isEmpty()) {
                arguments = request.getParams().stream()
                        .map(p -> p.getOrDefault("value", ""))
                        .toArray(String[]::new);
            }
            
            // 构建配置
            ScriptConfiguration config = new ScriptConfiguration();
            if (request.getTimeoutSeconds() != null) {
                config.setTimeoutMs(request.getTimeoutSeconds() * 1000L);
            }
            
            // 解析脚本类型（使用前面定义的 scriptType 变量）
            ScriptType execScriptType = ScriptType.valueOf(request.getScriptType() != null ? 
                    request.getScriptType() : "SHELL");
            
            // 执行脚本
            ScriptExecutionResult execResult = scriptManager.executeScript(
                    execScriptType, request.getScriptContent(), arguments, config);
            
            long duration = System.currentTimeMillis() - startTime;
            
            result.put("success", execResult.isSuccess());
            result.put("output", execResult.getMergedOutputString());
            result.put("errorMessage", execResult.getErrorMessage());
            result.put("exitCode", execResult.getExitCode());
            result.put("durationMs", duration);
            
            // 记录执行结果
            if (execResult.isSuccess()) {
                scriptExecutionService.recordExecutionSuccess(request.getScriptName(), executionId, 
                        execResult.getMergedOutputString(), duration);
            } else {
                scriptExecutionService.recordExecutionFailure(request.getScriptName(), executionId, 
                        execResult.getErrorMessage(), duration);
            }
            
            log.info("Script execution completed: success={}", execResult.isSuccess());
            return result;
            
        } catch (Exception e) {
            log.error("Failed to execute script", e);
            long duration = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
            result.put("exitCode", -1);
            result.put("durationMs", duration);
            
            // 记录执行失败
            scriptExecutionService.recordExecutionFailure(request.getScriptName(), executionId, 
                    e.getMessage(), duration);
            return result;
        }
    }
    
    /**
     * 创建或更新脚本信息
     */
    public ScriptInfoDTO saveScriptInfo(ScriptInfoDTO scriptInfo) {
        log.info("Saving script info: {}", scriptInfo.getScriptName());
        try {
            scriptStorage.saveScriptInfo(scriptInfo);
        } catch (StorageException e) {
            log.error("Failed to save script info", e);
        }
        return scriptInfo;
    }
    
    /**
     * 根据脚本名称获取脚本信息
     */
    public Optional<ScriptInfoDTO> getScriptInfo(String scriptName) {
        try {
            return scriptStorage.getScriptInfo(scriptName);
        } catch (StorageException e) {
            log.error("Failed to get script info", e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取所有脚本信息
     */
    public List<ScriptInfoDTO> getAllScriptInfo() {
        try {
            return scriptStorage.getAllScriptInfo();
        } catch (StorageException e) {
            log.error("Failed to get all script info", e);
            return List.of();
        }
    }
    
    /**
     * 删除脚本
     */
    public void deleteScript(String scriptName) {
        log.info("Deleting script: {}", scriptName);
        try {
            scriptStorage.deleteScriptInfo(scriptName);
        } catch (StorageException e) {
            log.error("Failed to delete script", e);
        }
    }
    
    /**
     * 保存脚本内容
     */
    public void saveScriptContent(String scriptName, String content) {
        log.info("Saving script content for: {}", scriptName);
        try {
            scriptStorage.saveScriptContent(scriptName, content);
        } catch (StorageException e) {
            log.error("Failed to save script content", e);
        }
    }
    
    /**
     * 获取脚本内容
     */
    public Optional<String> getScriptContent(String scriptName) {
        try {
            return scriptStorage.getScriptContent(scriptName);
        } catch (StorageException e) {
            log.error("Failed to get script content", e);
            return Optional.empty();
        }
    }
    
    /**
     * 保存脚本版本
     */
    public void saveScriptVersion(ScriptVersionDTO version) {
        log.info("Saving script version: v{}", version.getVersion());
        try {
            scriptStorage.saveScriptVersion(version);
        } catch (StorageException e) {
            log.error("Failed to save script version", e);
        }
    }
    
    /**
     * 获取脚本所有版本
     */
    public List<ScriptVersionDTO> getScriptVersions(String scriptName) {
        try {
            return scriptStorage.getScriptVersions(scriptName);
        } catch (StorageException e) {
            log.error("Failed to get script versions", e);
            return List.of();
        }
    }
    
    /**
     * 恢复到指定版本
     */
    public void restoreScriptVersion(String scriptName, String version) {
        log.info("Restoring script {} to version: {}", scriptName, version);
        try {
            scriptStorage.restoreScriptVersion(scriptName, version);
        } catch (StorageException e) {
            log.error("Failed to restore script version", e);
        }
    }
}