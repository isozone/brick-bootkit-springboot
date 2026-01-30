package com.zqzqq.bootkits.web.scripts.controller;

import com.zqzqq.bootkits.web.scripts.dto.ExecutionRecordDTO;
import com.zqzqq.bootkits.web.scripts.service.ScriptExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 脚本执行Controller
 * 提供脚本执行记录的管理和查询功能
 * 
 * @author brick-bootkit
 */
@Slf4j
@RestController
@RequestMapping("/brick-web/api/v1/scripts/executions")
@RequiredArgsConstructor
public class ScriptExecutionController {
    
    private final ScriptExecutionService executionService;
    
    /**
     * 获取所有执行记录
     */
    @GetMapping
    public ResponseEntity<List<ExecutionRecordDTO>> getAllExecutions() {
        return ResponseEntity.ok(executionService.getAllExecutionRecords());
    }
    
    /**
     * 获取脚本的执行记录
     */
    @GetMapping("/script/{scriptName}")
    public ResponseEntity<List<ExecutionRecordDTO>> getExecutionsByScript(@PathVariable String scriptName) {
        return ResponseEntity.ok(executionService.getExecutionRecords(scriptName));
    }
    
    /**
     * 获取脚本最近的执行记录
     */
    @GetMapping("/script/{scriptName}/recent")
    public ResponseEntity<List<ExecutionRecordDTO>> getRecentExecutions(@PathVariable String scriptName,
                                                                          @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(executionService.getRecentExecutionRecords(scriptName, limit));
    }
    
    /**
     * 获取单条执行记录
     */
    @GetMapping("/script/{scriptName}/{executionId}")
    public ResponseEntity<ExecutionRecordDTO> getExecution(@PathVariable String scriptName,
                                                            @PathVariable String executionId) {
        return executionService.getExecutionRecord(scriptName, executionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 根据状态获取执行记录
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ExecutionRecordDTO>> getExecutionsByStatus(
            @PathVariable ExecutionRecordDTO.ExecutionStatus status) {
        return ResponseEntity.ok(executionService.getExecutionRecordsByStatus(status));
    }
    
    /**
     * 获取时间范围内的执行记录
     */
    @GetMapping("/script/{scriptName}/range")
    public ResponseEntity<List<ExecutionRecordDTO>> getExecutionsByTimeRange(
            @PathVariable String scriptName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(executionService.getExecutionRecordsByTimeRange(scriptName, startTime, endTime));
    }
    
    /**
     * 获取执行统计信息
     */
    @GetMapping("/script/{scriptName}/statistics")
    public ResponseEntity<ScriptExecutionService.ExecutionStatistics> getExecutionStatistics(
            @PathVariable String scriptName) {
        return ResponseEntity.ok(executionService.getExecutionStatistics(scriptName));
    }
    
    /**
     * 删除执行记录
     */
    @DeleteMapping("/script/{scriptName}/{executionId}")
    public ResponseEntity<Void> deleteExecution(@PathVariable String scriptName,
                                                  @PathVariable String executionId) {
        boolean deleted = executionService.deleteExecutionRecord(scriptName, executionId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
    
    /**
     * 清理过期执行记录
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldExecutions(@RequestParam(defaultValue = "30") int retentionDays) {
        int cleanedCount = executionService.cleanupOldExecutionRecords(retentionDays);
        return ResponseEntity.ok(Map.of(
                "retentionDays", retentionDays,
                "cleanedCount", cleanedCount
        ));
    }
    
    /**
     * 记录执行开始（内部使用）
     */
    @PostMapping("/record/start")
    public ResponseEntity<ExecutionRecordDTO> recordExecutionStart(@RequestBody Map<String, String> request) {
        String executionId = java.util.UUID.randomUUID().toString();
        ExecutionRecordDTO record = executionService.recordExecutionStart(
                request.get("scriptName"),
                executionId,
                request.get("executedBy"),
                request.get("parameters")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }
    
    /**
     * 记录执行成功（内部使用）
     */
    @PostMapping("/record/success")
    public ResponseEntity<ExecutionRecordDTO> recordExecutionSuccess(@RequestBody Map<String, Object> request) {
        String scriptName = (String) request.get("scriptName");
        String executionId = (String) request.get("executionId");
        String output = (String) request.get("output");
        Long durationMs = request.get("durationMs") != null ? 
                Long.parseLong(request.get("durationMs").toString()) : null;
        
        ExecutionRecordDTO record = executionService.recordExecutionSuccess(scriptName, executionId, output, durationMs);
        return ResponseEntity.ok(record);
    }
    
    /**
     * 记录执行失败（内部使用）
     */
    @PostMapping("/record/failure")
    public ResponseEntity<ExecutionRecordDTO> recordExecutionFailure(@RequestBody Map<String, Object> request) {
        String scriptName = (String) request.get("scriptName");
        String executionId = (String) request.get("executionId");
        String errorMessage = (String) request.get("errorMessage");
        Long durationMs = request.get("durationMs") != null ? 
                Long.parseLong(request.get("durationMs").toString()) : null;
        
        ExecutionRecordDTO record = executionService.recordExecutionFailure(scriptName, executionId, errorMessage, durationMs);
        return ResponseEntity.ok(record);
    }
    
    /**
     * 记录执行进度（内部使用）
     */
    @PostMapping("/record/progress")
    public ResponseEntity<Void> recordExecutionProgress(@RequestBody Map<String, Object> request) {
        String scriptName = (String) request.get("scriptName");
        String executionId = (String) request.get("executionId");
        Integer progress = request.get("progress") != null ? 
                Integer.parseInt(request.get("progress").toString()) : null;
        String message = (String) request.get("message");
        
        executionService.recordExecutionProgress(scriptName, executionId, progress, message);
        return ResponseEntity.ok().build();
    }
}