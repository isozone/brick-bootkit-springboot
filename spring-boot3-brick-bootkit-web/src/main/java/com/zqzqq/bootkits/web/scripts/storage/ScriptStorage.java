package com.zqzqq.bootkits.web.scripts.storage;

import com.zqzqq.bootkits.web.scripts.dto.*;

import java.util.List;
import java.util.Optional;

/**
 * 脚本存储接口
 * 支持多种存储实现：文件存储、数据库存储、自定义存储
 * 
 * @author brick-bootkit
 */
public interface ScriptStorage {
    
    void initialize() throws StorageException;
    
    ScriptStorageType getType();
    
    // Script Repository
    void saveScriptInfo(ScriptInfoDTO scriptInfo) throws StorageException;
    Optional<ScriptInfoDTO> getScriptInfo(String scriptName) throws StorageException;
    List<ScriptInfoDTO> getAllScriptInfo() throws StorageException;
    void deleteScriptInfo(String scriptName) throws StorageException;
    void saveScriptContent(String scriptName, String content) throws StorageException;
    Optional<String> getScriptContent(String scriptName) throws StorageException;
    void saveScriptVersion(ScriptVersionDTO version) throws StorageException;
    List<ScriptVersionDTO> getScriptVersions(String scriptName) throws StorageException;
    void restoreScriptVersion(String scriptName, String version) throws StorageException;
    
    // Execution Records
    void saveExecutionRecord(ExecutionRecordDTO record) throws StorageException;
    Optional<ExecutionRecordDTO> getExecutionRecord(String executionId) throws StorageException;
    List<ExecutionRecordDTO> getExecutionRecords(int page, int size, String scriptName, String status) throws StorageException;
    List<ExecutionRecordDTO> getExecutionRecords(String scriptName) throws StorageException;
    List<ExecutionRecordDTO> getAllExecutionRecords() throws StorageException;
    boolean deleteExecutionRecord(String scriptName, String executionId) throws StorageException;
    void appendExecutionLog(String executionId, String logLine) throws StorageException;
    List<String> getExecutionLogs(String executionId) throws StorageException;
    
    // Scheduler Tasks
    void saveSchedulerTask(SchedulerTaskDTO task) throws StorageException;
    Optional<SchedulerTaskDTO> getSchedulerTask(String taskId) throws StorageException;
    List<SchedulerTaskDTO> getAllSchedulerTasks() throws StorageException;
    void deleteSchedulerTask(String taskId) throws StorageException;
    void updateTaskStatus(String taskId, String status) throws StorageException;
    
    // Queue
    void enqueue(QueueItemDTO item) throws StorageException;
    Optional<QueueItemDTO> dequeue() throws StorageException;
    QueueStatusDTO getQueueStatus() throws StorageException;
    void clearQueue() throws StorageException;
    
    // Batch Jobs
    void saveBatchJob(BatchJobDTO job) throws StorageException;
    Optional<BatchJobDTO> getBatchJob(String jobId) throws StorageException;
    List<BatchJobDTO> getAllBatchJobs() throws StorageException;
    void deleteBatchJob(String jobId) throws StorageException;
    
    // Templates
    void saveTemplate(ScriptTemplateDTO template) throws StorageException;
    Optional<ScriptTemplateDTO> getTemplate(String templateId) throws StorageException;
    List<ScriptTemplateDTO> getAllTemplates() throws StorageException;
    void deleteTemplate(String templateId) throws StorageException;
    
    void shutdown() throws StorageException;
}
