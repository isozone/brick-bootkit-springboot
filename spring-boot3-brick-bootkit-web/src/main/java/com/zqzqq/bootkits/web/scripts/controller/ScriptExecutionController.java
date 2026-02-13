package com.zqzqq.bootkits.web.scripts.controller;

import com.zqzqq.bootkits.web.scripts.dto.ExecutionRecordDTO;
import com.zqzqq.bootkits.web.scripts.service.ScriptExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping("/plugins-web/api/v1/scripts/executions")
@RequiredArgsConstructor
public class ScriptExecutionController {
    
    private final ScriptExecutionService executionService;
    
    /**
     * 分页获取所有执行记录
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getExecutionsWithPagination(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String scriptName,
            @RequestParam(required = false) String status) {
        List<ExecutionRecordDTO> allRecords;
        
        // 根据条件筛选
        if (scriptName != null && !scriptName.isEmpty()) {
            allRecords = executionService.getExecutionRecords(scriptName);
            if (status != null && !status.isEmpty()) {
                try {
                    ExecutionRecordDTO.ExecutionStatus execStatus = ExecutionRecordDTO.ExecutionStatus.valueOf(status);
                    allRecords = allRecords.stream()
                            .filter(r -> execStatus.equals(r.getStatus()))
                            .toList();
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status value: {}", status);
                }
            }
        } else {
            allRecords = executionService.getAllExecutionRecords();
            if (status != null && !status.isEmpty()) {
                try {
                    ExecutionRecordDTO.ExecutionStatus execStatus = ExecutionRecordDTO.ExecutionStatus.valueOf(status);
                    allRecords = allRecords.stream()
                            .filter(r -> execStatus.equals(r.getStatus()))
                            .toList();
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status value: {}", status);
                }
            }
        }
        
        // 按时间倒序排序
        allRecords = allRecords.stream()
                .sorted((a, b) -> {
                    if (b.getStartTime() == null) return 1;
                    if (a.getStartTime() == null) return -1;
                    return b.getStartTime().compareTo(a.getStartTime());
                })
                .toList();
        
        // 分页
        int total = allRecords.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<ExecutionRecordDTO> pagedRecords = fromIndex < total ? 
                allRecords.subList(fromIndex, toIndex) : List.of();
        
        return ResponseEntity.ok(Map.of(
                "records", pagedRecords,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size)
        ));
    }
    
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
     * 导出执行记录为CSV
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExecutions(
            @RequestParam(required = false) String scriptName,
            @RequestParam(required = false) String status) {
        
        List<ExecutionRecordDTO> allRecords;
        
        // 根据条件筛选
        if (scriptName != null && !scriptName.isEmpty()) {
            allRecords = executionService.getExecutionRecords(scriptName);
            if (status != null && !status.isEmpty()) {
                try {
                    ExecutionRecordDTO.ExecutionStatus execStatus = ExecutionRecordDTO.ExecutionStatus.valueOf(status);
                    allRecords = allRecords.stream()
                            .filter(r -> execStatus.equals(r.getStatus()))
                            .toList();
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status value: {}", status);
                }
            }
        } else {
            allRecords = executionService.getAllExecutionRecords();
            if (status != null && !status.isEmpty()) {
                try {
                    ExecutionRecordDTO.ExecutionStatus execStatus = ExecutionRecordDTO.ExecutionStatus.valueOf(status);
                    allRecords = allRecords.stream()
                            .filter(r -> execStatus.equals(r.getStatus()))
                            .toList();
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status value: {}", status);
                }
            }
        }
        
        // 按时间倒序排序
        allRecords = allRecords.stream()
                .sorted((a, b) -> {
                    if (b.getStartTime() == null) return 1;
                    if (a.getStartTime() == null) return -1;
                    return b.getStartTime().compareTo(a.getStartTime());
                })
                .toList();
        
        // 生成CSV
        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // BOM for Excel UTF-8 support
        csv.append("执行ID,脚本名称,脚本类型,状态,提交人,开始时间,结束时间,耗时(毫秒),参数,状态消息,输出,错误信息\n");
        
        for (ExecutionRecordDTO record : allRecords) {
            csv.append(escapeCsv(record.getExecutionId())).append(",");
            csv.append(escapeCsv(record.getScriptName())).append(",");
            csv.append(escapeCsv(record.getScriptType())).append(",");
            csv.append(escapeCsv(record.getStatus() != null ? record.getStatus().name() : "")).append(",");
            csv.append(escapeCsv(record.getSubmittedBy() != null ? record.getSubmittedBy() : record.getExecutedBy())).append(",");
            csv.append(escapeCsv(formatTime(record.getStartTime()))).append(",");
            csv.append(escapeCsv(formatTime(record.getEndTime()))).append(",");
            csv.append(record.getExecutionTimeMs() != null ? record.getExecutionTimeMs() : "").append(",");
            csv.append(escapeCsv(record.getParameters())).append(",");
            csv.append(escapeCsv(record.getStatusMessage())).append(",");
            csv.append(escapeCsv(truncateString(record.getOutput(), 32000))).append(",");
            csv.append(escapeCsv(truncateString(record.getErrorMessage(), 32000))).append("\n");
        }
        
        String filename = "execution_records_" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/plain;charset=utf-8"));
        headers.setContentDispositionFormData("attachment", filename);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        // Escape quotes and wrap in quotes if contains comma, quote or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    private String truncateString(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength) + "...";
    }
    
    private String formatTime(LocalDateTime time) {
        if (time == null) return "";
        return time.toString().replace("T", " ").substring(0, 19);
    }
    
    /**
     * 记录执行开始（内部使用）
     */
    @PostMapping("/record/start")
    public ResponseEntity<ExecutionRecordDTO> recordExecutionStart(@RequestBody Map<String, String> request) {
        String executionId = java.util.UUID.randomUUID().toString();
        ExecutionRecordDTO record = executionService.recordExecutionStart(
                request.get("scriptName"),
                request.get("scriptType"),
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