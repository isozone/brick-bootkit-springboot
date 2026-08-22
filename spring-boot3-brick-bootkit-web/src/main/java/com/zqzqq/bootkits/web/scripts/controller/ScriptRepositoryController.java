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


package com.zqzqq.bootkits.web.scripts.controller;

import com.zqzqq.bootkits.web.scripts.dto.ExecuteRequest;
import com.zqzqq.bootkits.web.scripts.dto.ScriptInfoDTO;
import com.zqzqq.bootkits.web.scripts.dto.ScriptVersionDTO;
import com.zqzqq.bootkits.web.scripts.service.ScriptRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 脚本仓库Controller
 * 提供脚本的CRUD、版本管理等功能
 * 
 * @author brick-bootkit
 */
@Slf4j
@RestController
@RequestMapping("/plugins-web/api/v1/scripts")
@RequiredArgsConstructor
public class ScriptRepositoryController {
    
    private final ScriptRepositoryService scriptRepositoryService;
    
    /**
     * 获取所有脚本列表
     */
    @GetMapping
    public ResponseEntity<List<ScriptInfoDTO>> getAllScripts() {
        return ResponseEntity.ok(scriptRepositoryService.getAllScriptInfo());
    }
    
    /**
     * 根据脚本名称获取脚本信息
     */
    @GetMapping("/{scriptName}")
    public ResponseEntity<ScriptInfoDTO> getScript(@PathVariable String scriptName) {
        return scriptRepositoryService.getScriptInfo(scriptName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 执行脚本（测试用）
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeScript(@RequestBody ExecuteRequest request) {
        log.info("Executing script: {}, type: {}", request.getScriptName(), request.getScriptType());
        
        Map<String, Object> result = scriptRepositoryService.executeScript(request);
        
        boolean success = Boolean.TRUE.equals(result.get("success"));
        if (success) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * 创建或更新脚本
     */
    @PostMapping
    public ResponseEntity<ScriptInfoDTO> saveScript(@RequestBody ScriptInfoDTO scriptInfo) {
        ScriptInfoDTO saved = scriptRepositoryService.saveScriptInfo(scriptInfo);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    /**
     * 更新脚本
     */
    @PutMapping("/{scriptName}")
    public ResponseEntity<ScriptInfoDTO> updateScript(@PathVariable String scriptName,
                                                       @RequestBody ScriptInfoDTO scriptInfo) {
        scriptInfo.setScriptName(scriptName);
        ScriptInfoDTO saved = scriptRepositoryService.saveScriptInfo(scriptInfo);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * 删除脚本
     */
    @PostMapping("/{scriptName}/delete")
    public ResponseEntity<Map<String, Object>> deleteScript(@PathVariable String scriptName) {
        scriptRepositoryService.deleteScript(scriptName);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取脚本内容
     */
    @GetMapping("/{scriptName}/content")
    public ResponseEntity<String> getScriptContent(@PathVariable String scriptName) {
        return scriptRepositoryService.getScriptContent(scriptName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 更新脚本内容
     */
    @PutMapping("/{scriptName}/content")
    public ResponseEntity<Void> updateScriptContent(@PathVariable String scriptName,
                                                     @RequestBody Map<String, String> request) {
        String content = request.get("content");
        if (content != null) {
            scriptRepositoryService.saveScriptContent(scriptName, content);
        }
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取脚本版本列表
     */
    @GetMapping("/{scriptName}/versions")
    public ResponseEntity<List<ScriptVersionDTO>> getScriptVersions(@PathVariable String scriptName) {
        return ResponseEntity.ok(scriptRepositoryService.getScriptVersions(scriptName));
    }
    
    /**
     * 保存脚本版本
     */
    @PostMapping("/{scriptName}/versions")
    public ResponseEntity<Void> saveScriptVersion(@PathVariable String scriptName,
                                                   @RequestBody ScriptVersionDTO version) {
        scriptRepositoryService.saveScriptVersion(version);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    /**
     * 恢复到指定版本
     */
    @PostMapping("/{scriptName}/versions/{version}/restore")
    public ResponseEntity<Void> restoreScriptVersion(@PathVariable String scriptName,
                                                      @PathVariable String version) {
        scriptRepositoryService.restoreScriptVersion(scriptName, version);
        return ResponseEntity.ok().build();
    }
}