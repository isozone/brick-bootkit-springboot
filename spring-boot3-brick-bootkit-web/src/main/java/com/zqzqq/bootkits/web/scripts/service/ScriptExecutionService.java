package com.zqzqq.bootkits.web.scripts.service;

import com.zqzqq.bootkits.web.scripts.dto.ExecutionRecordDTO;
import com.zqzqq.bootkits.web.scripts.dto.ScriptInfoDTO;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 脚本执行服务
 * 提供脚本执行记录的管理和查询功能
 * 
 * @author brick-bootkit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptExecutionService {
    
    private final ScriptStorage scriptStorage;
    
    /**
     * 记录执行开始
     */
    public ExecutionRecordDTO recordExecutionStart(String scriptName, String scriptType,
                                                   String executionId, String executedBy, String parameters) {
        log.info("Recording execution start for script: {}, type: {}, executionId: {}", scriptName, scriptType, executionId);
        
        ExecutionRecordDTO record = new ExecutionRecordDTO();
        record.setExecutionId(executionId);
        record.setScriptName(scriptName);
        record.setScriptType(scriptType);
        record.setStartTime(LocalDateTime.now());
        record.setSubmittedBy(executedBy);
        record.setParameters(parameters);
        record.setStatus(ExecutionRecordDTO.ExecutionStatus.RUNNING);
        
        try {
            scriptStorage.saveExecutionRecord(record);
        } catch (StorageException e) {
            log.error("Failed to save execution record", e);
        }
        return record;
    }
    
    /**
     * 记录执行成功
     */
    public ExecutionRecordDTO recordExecutionSuccess(String scriptName, String executionId,
                                                      String output, Long durationMs) {
        log.info("Recording execution success for script: {}, executionId: {}", scriptName, executionId);
        
        try {
            Optional<ExecutionRecordDTO> recordOpt = scriptStorage.getExecutionRecord(executionId);
            if (recordOpt.isEmpty()) {
                log.warn("Execution record not found: {}", executionId);
                return null;
            }
            
            ExecutionRecordDTO record = recordOpt.get();
            record.setStatus(ExecutionRecordDTO.ExecutionStatus.SUCCESS);
            record.setEndTime(LocalDateTime.now());
            record.setOutput(output);
            record.setDurationMs(durationMs);
            
            scriptStorage.saveExecutionRecord(record);
            return record;
        } catch (StorageException e) {
            log.error("Failed to record execution success", e);
            return null;
        }
    }
    
    /**
     * 记录执行失败
     */
    public ExecutionRecordDTO recordExecutionFailure(String scriptName, String executionId,
                                                       String errorMessage, Long durationMs) {
        log.info("Recording execution failure for script: {}, executionId: {}", scriptName, executionId);
        
        try {
            Optional<ExecutionRecordDTO> recordOpt = scriptStorage.getExecutionRecord(executionId);
            if (recordOpt.isEmpty()) {
                log.warn("Execution record not found: {}", executionId);
                return null;
            }
            
            ExecutionRecordDTO record = recordOpt.get();
            record.setStatus(ExecutionRecordDTO.ExecutionStatus.FAILED);
            record.setEndTime(LocalDateTime.now());
            record.setErrorMessage(errorMessage);
            record.setDurationMs(durationMs);
            
            scriptStorage.saveExecutionRecord(record);
            return record;
        } catch (StorageException e) {
            log.error("Failed to record execution failure", e);
            return null;
        }
    }
    
    /**
     * 记录执行进度
     */
    public void recordExecutionProgress(String scriptName, String executionId, 
                                         Integer progress, String message) {
        log.debug("Recording execution progress for script: {}, executionId: {}, progress: {}", 
                  scriptName, executionId, progress);
        
        try {
            Optional<ExecutionRecordDTO> recordOpt = scriptStorage.getExecutionRecord(executionId);
            if (recordOpt.isPresent()) {
                ExecutionRecordDTO record = recordOpt.get();
                record.setProgress(progress);
                record.setStatusMessage(message);
                scriptStorage.saveExecutionRecord(record);
            }
        } catch (StorageException e) {
            log.error("Failed to record execution progress", e);
        }
    }
    
    /**
     * 获取执行记录
     */
    public Optional<ExecutionRecordDTO> getExecutionRecord(String scriptName, String executionId) {
        try {
            return scriptStorage.getExecutionRecord(executionId);
        } catch (StorageException e) {
            log.error("Failed to get execution record", e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取脚本所有执行记录
     */
    public List<ExecutionRecordDTO> getExecutionRecords(String scriptName) {
        try {
            return scriptStorage.getExecutionRecords(scriptName);
        } catch (StorageException e) {
            log.error("Failed to get execution records", e);
            return List.of();
        }
    }
    
    /**
     * 获取脚本最近N条执行记录
     */
    public List<ExecutionRecordDTO> getRecentExecutionRecords(String scriptName, int limit) {
        return getExecutionRecords(scriptName).stream()
                .limit(limit)
                .toList();
    }
    
    /**
     * 获取所有执行记录
     */
    public List<ExecutionRecordDTO> getAllExecutionRecords() {
        try {
            return scriptStorage.getAllExecutionRecords();
        } catch (StorageException e) {
            log.error("Failed to get all execution records", e);
            return List.of();
        }
    }
    
    /**
     * 按状态获取执行记录
     */
    public List<ExecutionRecordDTO> getExecutionRecordsByStatus(ExecutionRecordDTO.ExecutionStatus status) {
        return getAllExecutionRecords().stream()
                .filter(r -> status.equals(r.getStatus()))
                .toList();
    }
    
    /**
     * 获取时间范围内的执行记录
     */
    public List<ExecutionRecordDTO> getExecutionRecordsByTimeRange(String scriptName,
                                                                     LocalDateTime startTime,
                                                                     LocalDateTime endTime) {
        return getExecutionRecords(scriptName).stream()
                .filter(r -> r.getStartTime() != null && 
                            !r.getStartTime().isBefore(startTime) && 
                            !r.getStartTime().isAfter(endTime))
                .toList();
    }
    
    /**
     * 获取执行统计
     */
    public ExecutionStatistics getExecutionStatistics(String scriptName) {
        List<ExecutionRecordDTO> records = getExecutionRecords(scriptName);
        
        long totalCount = records.size();
        long successCount = records.stream()
                .filter(r -> ExecutionRecordDTO.ExecutionStatus.SUCCESS.equals(r.getStatus()))
                .count();
        long failedCount = records.stream()
                .filter(r -> ExecutionRecordDTO.ExecutionStatus.FAILED.equals(r.getStatus()))
                .count();
        long runningCount = records.stream()
                .filter(r -> ExecutionRecordDTO.ExecutionStatus.RUNNING.equals(r.getStatus()))
                .count();
        
        Double avgDuration = records.stream()
                .filter(r -> r.getExecutionTimeMs() != null)
                .mapToLong(ExecutionRecordDTO::getExecutionTimeMs)
                .average()
                .orElse(0.0);
        
        ExecutionStatistics stats = new ExecutionStatistics();
        stats.setTotalCount(totalCount);
        stats.setSuccessCount(successCount);
        stats.setFailedCount(failedCount);
        stats.setRunningCount(runningCount);
        stats.setSuccessRate(totalCount > 0 ? (double) successCount / totalCount * 100 : 0);
        stats.setAvgDurationMs(avgDuration);
        
        return stats;
    }
    
    /**
     * 删除执行记录
     */
    public boolean deleteExecutionRecord(String scriptName, String executionId) {
        try {
            return scriptStorage.deleteExecutionRecord(scriptName, executionId);
        } catch (StorageException e) {
            log.error("Failed to delete execution record", e);
            return false;
        }
    }
    
    /**
     * 清理过期执行记录
     */
    public int cleanupOldExecutionRecords(int retentionDays) {
        log.info("Cleaning up execution records older than {} days", retentionDays);
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        int cleanedCount = 0;
        
        try {
            List<ScriptInfoDTO> allScripts = scriptStorage.getAllScriptInfo();
            for (ScriptInfoDTO script : allScripts) {
                List<ExecutionRecordDTO> records = getExecutionRecords(script.getScriptName());
                for (ExecutionRecordDTO record : records) {
                    if (record.getEndTime() != null && record.getEndTime().isBefore(cutoffTime)) {
                        deleteExecutionRecord(script.getScriptName(), record.getExecutionId());
                        cleanedCount++;
                    }
                }
            }
        } catch (StorageException e) {
            log.error("Failed to cleanup old execution records", e);
        }
        
        return cleanedCount;
    }
    
    /**
     * 执行统计信息
     */
    public static class ExecutionStatistics {
        private long totalCount;
        private long successCount;
        private long failedCount;
        private long runningCount;
        private double successRate;
        private double avgDurationMs;
        
        // Getters and Setters
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public long getSuccessCount() { return successCount; }
        public void setSuccessCount(long successCount) { this.successCount = successCount; }
        public long getFailedCount() { return failedCount; }
        public void setFailedCount(long failedCount) { this.failedCount = failedCount; }
        public long getRunningCount() { return runningCount; }
        public void setRunningCount(long runningCount) { this.runningCount = runningCount; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        public double getAvgDurationMs() { return avgDurationMs; }
        public void setAvgDurationMs(double avgDurationMs) { this.avgDurationMs = avgDurationMs; }
    }
}