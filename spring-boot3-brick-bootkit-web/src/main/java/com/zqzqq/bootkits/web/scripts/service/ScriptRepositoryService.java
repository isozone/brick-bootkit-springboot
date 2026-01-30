package com.zqzqq.bootkits.web.scripts.service;

import com.zqzqq.bootkits.web.scripts.dto.ScriptInfoDTO;
import com.zqzqq.bootkits.web.scripts.dto.ScriptVersionDTO;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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