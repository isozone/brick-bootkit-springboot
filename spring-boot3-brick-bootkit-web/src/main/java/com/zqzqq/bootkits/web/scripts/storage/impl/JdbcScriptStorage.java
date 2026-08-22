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


package com.zqzqq.bootkits.web.scripts.storage.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zqzqq.bootkits.scripts.core.ScriptType;
import com.zqzqq.bootkits.web.scripts.dto.*;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorageType;
import com.zqzqq.bootkits.web.scripts.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 数据库存储实现
 * 使用MyBatis-Plus进行数据库操作
 * 
 * @author brick-bootkit
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcScriptStorage implements ScriptStorage {
    
    private final com.zqzqq.bootkits.web.scripts.mapper.ScriptInfoMapper scriptInfoMapper;
    private final com.zqzqq.bootkits.web.scripts.mapper.ScriptExecutionMapper executionMapper;
    private final com.zqzqq.bootkits.web.scripts.mapper.SchedulerTaskMapper schedulerTaskMapper;
    private final com.zqzqq.bootkits.web.scripts.mapper.QueueItemMapper queueItemMapper;
    private final com.zqzqq.bootkits.web.scripts.mapper.BatchJobMapper batchJobMapper;
    private final com.zqzqq.bootkits.web.scripts.mapper.ScriptTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;
    
    @Override
    public void initialize() throws StorageException {
        log.info("JdbcScriptStorage initialized");
    }
    
    @Override
    public ScriptStorageType getType() {
        return ScriptStorageType.JDBC;
    }
    
    @Override
    public void saveScriptInfo(ScriptInfoDTO scriptInfo) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity entity = convertToScriptInfoEntity(scriptInfo);
        if (scriptInfoMapper.selectById(entity.getId()) != null) {
            scriptInfoMapper.updateById(entity);
        } else {
            scriptInfoMapper.insert(entity);
        }
    }
    
    @Override
    public Optional<ScriptInfoDTO> getScriptInfo(String scriptName) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity entity = scriptInfoMapper.selectOne(
                new LambdaQueryWrapper<com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity>()
                        .eq(com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity::getScriptName, scriptName));
        return Optional.ofNullable(entity).map(this::convertToScriptInfoDTO);
    }
    
    @Override
    public List<ScriptInfoDTO> getAllScriptInfo() throws StorageException {
        return scriptInfoMapper.selectList(null).stream()
                .map(this::convertToScriptInfoDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteScriptInfo(String scriptName) throws StorageException {
        scriptInfoMapper.delete(
                new LambdaQueryWrapper<com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity>()
                        .eq(com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity::getScriptName, scriptName));
    }
    
    @Override
    public void saveScriptContent(String scriptName, String content) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity entity = scriptInfoMapper.selectOne(
                new LambdaQueryWrapper<com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity>()
                        .eq(com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity::getScriptName, scriptName));
        if (entity != null) {
            entity.setScriptContent(content);
            scriptInfoMapper.updateById(entity);
        }
    }
    
    @Override
    public Optional<String> getScriptContent(String scriptName) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity entity = scriptInfoMapper.selectOne(
                new LambdaQueryWrapper<com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity>()
                        .eq(com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity::getScriptName, scriptName));
        return Optional.ofNullable(entity).map(com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity::getScriptContent);
    }
    
    @Override
    public void saveScriptVersion(ScriptVersionDTO version) throws StorageException {
    }
    
    @Override
    public List<ScriptVersionDTO> getScriptVersions(String scriptName) throws StorageException {
        return List.of();
    }
    
    @Override
    public void restoreScriptVersion(String scriptName, String version) throws StorageException {
    }
    
    @Override
    public void saveExecutionRecord(ExecutionRecordDTO record) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.ScriptExecutionEntity entity = convertToExecutionEntity(record);
        executionMapper.insert(entity);
    }
    
    @Override
    public Optional<ExecutionRecordDTO> getExecutionRecord(String executionId) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.ScriptExecutionEntity entity = executionMapper.selectById(executionId);
        return Optional.ofNullable(entity).map(this::convertToExecutionDTO);
    }
    
    @Override
    public List<ExecutionRecordDTO> getExecutionRecords(int page, int size, String scriptName, String status) throws StorageException {
        return executionMapper.selectList(null).stream()
                .map(this::convertToExecutionDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ExecutionRecordDTO> getExecutionRecords(String scriptName) throws StorageException {
        return executionMapper.selectList(null).stream()
                .filter(e -> e.getScriptName().equals(scriptName))
                .map(this::convertToExecutionDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ExecutionRecordDTO> getAllExecutionRecords() throws StorageException {
        return executionMapper.selectList(null).stream()
                .map(this::convertToExecutionDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean deleteExecutionRecord(String scriptName, String executionId) throws StorageException {
        return executionMapper.deleteById(executionId) > 0;
    }
    
    @Override
    public void appendExecutionLog(String executionId, String logLine) throws StorageException {
    }
    
    @Override
    public List<String> getExecutionLogs(String executionId) throws StorageException {
        return List.of();
    }
    
    @Override
    public void saveSchedulerTask(SchedulerTaskDTO task) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.SchedulerTaskEntity entity = convertToSchedulerTaskEntity(task);
        schedulerTaskMapper.insert(entity);
    }
    
    @Override
    public Optional<SchedulerTaskDTO> getSchedulerTask(String taskId) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.SchedulerTaskEntity entity = schedulerTaskMapper.selectById(taskId);
        return Optional.ofNullable(entity).map(this::convertToSchedulerTaskDTO);
    }
    
    @Override
    public List<SchedulerTaskDTO> getAllSchedulerTasks() throws StorageException {
        return schedulerTaskMapper.selectList(null).stream()
                .map(this::convertToSchedulerTaskDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteSchedulerTask(String taskId) throws StorageException {
        schedulerTaskMapper.deleteById(taskId);
    }
    
    @Override
    public void updateTaskStatus(String taskId, String status) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.SchedulerTaskEntity entity = schedulerTaskMapper.selectById(taskId);
        if (entity != null) {
            entity.setTaskStatus(status);
            schedulerTaskMapper.updateById(entity);
        }
    }
    
    @Override
    public void enqueue(QueueItemDTO item) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.QueueItemEntity entity = convertToQueueItemEntity(item);
        queueItemMapper.insert(entity);
    }
    
    @Override
    public Optional<QueueItemDTO> dequeue() throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.QueueItemEntity entity = queueItemMapper.selectOne(
                new LambdaQueryWrapper<com.zqzqq.bootkits.web.scripts.storage.entity.QueueItemEntity>()
                        .eq(com.zqzqq.bootkits.web.scripts.storage.entity.QueueItemEntity::getQueueStatus, "WAITING"));
        if (entity != null) {
            entity.setQueueStatus("PROCESSING");
            queueItemMapper.updateById(entity);
            return Optional.of(convertToQueueItemDTO(entity));
        }
        return Optional.empty();
    }
    
    @Override
    public QueueStatusDTO getQueueStatus() throws StorageException {
        return QueueStatusDTO.builder().build();
    }
    
    @Override
    public void clearQueue() throws StorageException {
        queueItemMapper.delete(null);
    }
    
    @Override
    public void saveBatchJob(BatchJobDTO job) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.BatchJobEntity entity = convertToBatchJobEntity(job);
        batchJobMapper.insert(entity);
    }
    
    @Override
    public Optional<BatchJobDTO> getBatchJob(String jobId) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.BatchJobEntity entity = batchJobMapper.selectById(jobId);
        return Optional.ofNullable(entity).map(this::convertToBatchJobDTO);
    }
    
    @Override
    public List<BatchJobDTO> getAllBatchJobs() throws StorageException {
        return batchJobMapper.selectList(null).stream()
                .map(this::convertToBatchJobDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteBatchJob(String jobId) throws StorageException {
        batchJobMapper.deleteById(jobId);
    }
    
    @Override
    public void saveTemplate(ScriptTemplateDTO template) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.ScriptTemplateEntity entity = convertToTemplateEntity(template);
        templateMapper.insert(entity);
    }
    
    @Override
    public Optional<ScriptTemplateDTO> getTemplate(String templateId) throws StorageException {
        com.zqzqq.bootkits.web.scripts.storage.entity.ScriptTemplateEntity entity = templateMapper.selectById(templateId);
        return Optional.ofNullable(entity).map(this::convertToTemplateDTO);
    }
    
    @Override
    public List<ScriptTemplateDTO> getAllTemplates() throws StorageException {
        return templateMapper.selectList(null).stream()
                .map(this::convertToTemplateDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteTemplate(String templateId) throws StorageException {
        templateMapper.deleteById(templateId);
    }
    
    @Override
    public void shutdown() throws StorageException {
    }
    
    private com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity convertToScriptInfoEntity(ScriptInfoDTO dto) {
        return com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity.builder()
                .scriptName(dto.getScriptName())
                .displayName(dto.getDisplayName())
                .description(dto.getDescription())
                .scriptContent(dto.getContent())
                .version(dto.getVersion())
                .group(dto.getGroup())
                .author(dto.getAuthor())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
    
    private ScriptInfoDTO convertToScriptInfoDTO(com.zqzqq.bootkits.web.scripts.storage.entity.ScriptInfoEntity entity) {
        return ScriptInfoDTO.builder()
                .scriptName(entity.getScriptName())
                .displayName(entity.getDisplayName())
                .description(entity.getDescription())
                .content(entity.getScriptContent())
                .version(entity.getVersion())
                .group(entity.getGroup())
                .author(entity.getAuthor())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    private com.zqzqq.bootkits.web.scripts.storage.entity.ScriptExecutionEntity convertToExecutionEntity(ExecutionRecordDTO dto) {
        return com.zqzqq.bootkits.web.scripts.storage.entity.ScriptExecutionEntity.builder()
                .id(dto.getExecutionId())
                .scriptName(dto.getScriptName())
                .status(dto.getStatus() != null ? dto.getStatus().name() : null)
                .exitCode(dto.getExitCode())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .executionTimeMs(dto.getExecutionTimeMs())
                .output(dto.getOutput())
                .errorMessage(dto.getErrorMessage())
                .arguments(dto.getArguments() != null ? String.join(",", dto.getArguments()) : null)
                .submittedBy(dto.getSubmittedBy())
                .createdAt(dto.getCreatedAt())
                .build();
    }
    
    private ExecutionRecordDTO convertToExecutionDTO(com.zqzqq.bootkits.web.scripts.storage.entity.ScriptExecutionEntity entity) {
        return ExecutionRecordDTO.builder()
                .executionId(entity.getId())
                .scriptName(entity.getScriptName())
                .status(entity.getStatus() != null ? ExecutionRecordDTO.ExecutionStatus.valueOf(entity.getStatus()) : null)
                .exitCode(entity.getExitCode())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .executionTimeMs(entity.getExecutionTimeMs())
                .output(entity.getOutput())
                .errorMessage(entity.getErrorMessage())
                .arguments(entity.getArguments() != null ? List.of(entity.getArguments().split(",")) : null)
                .submittedBy(entity.getSubmittedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    private com.zqzqq.bootkits.web.scripts.storage.entity.SchedulerTaskEntity convertToSchedulerTaskEntity(SchedulerTaskDTO dto) {
        return com.zqzqq.bootkits.web.scripts.storage.entity.SchedulerTaskEntity.builder()
                .id(dto.getTaskId())
                .taskId(dto.getTaskId())
                .taskName(dto.getTaskName())
                .description(dto.getDescription())
                .cronExpression(dto.getCronExpression())
                .scriptName(dto.getScriptName())
                .parameters(dto.getParameters())
                .taskStatus(dto.getTaskStatus())
                .maxConcurrentRuns(dto.getMaxConcurrentRuns())
                .timeoutMinutes(dto.getTimeoutMinutes())
                .retryCount(dto.getRetryCount())
                .retryIntervalMinutes(dto.getRetryIntervalMinutes())
                .notifyOnSuccess(dto.getNotifyOnSuccess())
                .notifyOnFailure(dto.getNotifyOnFailure())
                .notificationTarget(dto.getNotificationTarget())
                .lastStartTime(dto.getLastStartTime())
                .lastExecutionStartTime(dto.getLastExecutionStartTime())
                .lastExecutionEndTime(dto.getLastExecutionEndTime())
                .lastExecutionId(dto.getLastExecutionId())
                .lastExecutionResult(dto.getLastExecutionResult())
                .lastExecutionStatus(dto.getLastExecutionStatus())
                .lastSuccessTime(dto.getLastSuccessTime())
                .lastFailureTime(dto.getLastFailureTime())
                .manualTriggerCount(dto.getManualTriggerCount())
                .nextExecution(dto.getNextExecution())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
    
    private SchedulerTaskDTO convertToSchedulerTaskDTO(com.zqzqq.bootkits.web.scripts.storage.entity.SchedulerTaskEntity entity) {
        return SchedulerTaskDTO.builder()
                .taskId(entity.getTaskId())
                .taskName(entity.getTaskName())
                .description(entity.getDescription())
                .cronExpression(entity.getCronExpression())
                .scriptName(entity.getScriptName())
                .parameters(entity.getParameters())
                .taskStatus(entity.getTaskStatus())
                .maxConcurrentRuns(entity.getMaxConcurrentRuns())
                .timeoutMinutes(entity.getTimeoutMinutes())
                .retryCount(entity.getRetryCount())
                .retryIntervalMinutes(entity.getRetryIntervalMinutes())
                .notifyOnSuccess(entity.getNotifyOnSuccess())
                .notifyOnFailure(entity.getNotifyOnFailure())
                .notificationTarget(entity.getNotificationTarget())
                .lastStartTime(entity.getLastStartTime())
                .lastExecutionStartTime(entity.getLastExecutionStartTime())
                .lastExecutionEndTime(entity.getLastExecutionEndTime())
                .lastExecutionId(entity.getLastExecutionId())
                .lastExecutionResult(entity.getLastExecutionResult())
                .lastExecutionStatus(entity.getLastExecutionStatus())
                .lastSuccessTime(entity.getLastSuccessTime())
                .lastFailureTime(entity.getLastFailureTime())
                .manualTriggerCount(entity.getManualTriggerCount())
                .nextExecution(entity.getNextExecution())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    private com.zqzqq.bootkits.web.scripts.storage.entity.QueueItemEntity convertToQueueItemEntity(QueueItemDTO dto) {
        return com.zqzqq.bootkits.web.scripts.storage.entity.QueueItemEntity.builder()
                .id(dto.getItemId())
                .itemId(dto.getItemId())
                .queueName(dto.getQueueName())
                .scriptName(dto.getScriptName())
                .parameters(dto.getParameters())
                .priority(dto.getPriority())
                .queueStatus(dto.getQueueStatus() != null ? dto.getQueueStatus().name() : null)
                .scheduledAt(dto.getScheduledAt())
                .startedAt(dto.getStartedAt())
                .completedAt(dto.getCompletedAt())
                .result(dto.getResult())
                .errorMessage(dto.getErrorMessage())
                .submittedBy(dto.getSubmittedBy())
                .createdAt(dto.getCreatedAt())
                .build();
    }
    
    private QueueItemDTO convertToQueueItemDTO(com.zqzqq.bootkits.web.scripts.storage.entity.QueueItemEntity entity) {
        return QueueItemDTO.builder()
                .itemId(entity.getItemId())
                .queueName(entity.getQueueName())
                .scriptName(entity.getScriptName())
                .parameters(entity.getParameters())
                .priority(entity.getPriority())
                .queueStatus(entity.getQueueStatus() != null ? QueueItemDTO.QueueItemStatus.valueOf(entity.getQueueStatus()) : null)
                .scheduledAt(entity.getScheduledAt())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .result(entity.getResult())
                .errorMessage(entity.getErrorMessage())
                .submittedBy(entity.getSubmittedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    private com.zqzqq.bootkits.web.scripts.storage.entity.BatchJobEntity convertToBatchJobEntity(BatchJobDTO dto) {
        return com.zqzqq.bootkits.web.scripts.storage.entity.BatchJobEntity.builder()
                .id(dto.getJobId())
                .jobId(dto.getJobId())
                .jobName(dto.getJobName())
                .description(dto.getDescription())
                .scriptNames(dto.getScriptNames() != null ? String.join(",", dto.getScriptNames()) : null)
                .parameters(dto.getParameters())
                .executionMode(dto.getExecutionMode() != null ? dto.getExecutionMode().name() : null)
                .stopOnError(dto.getStopOnError())
                .maxConcurrency(dto.getMaxConcurrency())
                .jobStatus(dto.getJobStatus() != null ? dto.getJobStatus().name() : null)
                .totalItems(dto.getTotalItems())
                .processedItems(dto.getProcessedItems())
                .successItems(dto.getSuccessItems())
                .failedItems(dto.getFailedItems())
                .progress(dto.getProgress())
                .processedScripts(dto.getProcessedScripts() != null ? String.join(",", dto.getProcessedScripts()) : null)
                .executionLog(dto.getExecutionLog() != null ? String.join("\n", dto.getExecutionLog()) : null)
                .lastProcessedScript(dto.getLastProcessedScript())
                .lastProcessedTime(dto.getLastProcessedTime())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .scheduledTime(dto.getScheduledTime())
                .submittedBy(dto.getSubmittedBy())
                .build();
    }
    
    private BatchJobDTO convertToBatchJobDTO(com.zqzqq.bootkits.web.scripts.storage.entity.BatchJobEntity entity) {
        return BatchJobDTO.builder()
                .jobId(entity.getJobId())
                .jobName(entity.getJobName())
                .description(entity.getDescription())
                .scriptNames(entity.getScriptNames() != null ? List.of(entity.getScriptNames().split(",")) : null)
                .parameters(entity.getParameters())
                .executionMode(entity.getExecutionMode() != null ? BatchJobDTO.ExecutionMode.valueOf(entity.getExecutionMode()) : null)
                .stopOnError(entity.getStopOnError())
                .maxConcurrency(entity.getMaxConcurrency())
                .jobStatus(entity.getJobStatus() != null ? BatchJobDTO.BatchStatus.valueOf(entity.getJobStatus()) : null)
                .totalItems(entity.getTotalItems())
                .processedItems(entity.getProcessedItems())
                .successItems(entity.getSuccessItems())
                .failedItems(entity.getFailedItems())
                .progress(entity.getProgress())
                .processedScripts(entity.getProcessedScripts() != null ? List.of(entity.getProcessedScripts().split(",")) : null)
                .executionLog(entity.getExecutionLog() != null ? List.of(entity.getExecutionLog().split("\n")) : null)
                .lastProcessedScript(entity.getLastProcessedScript())
                .lastProcessedTime(entity.getLastProcessedTime())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .scheduledTime(entity.getScheduledTime())
                .submittedBy(entity.getSubmittedBy())
                .build();
    }
    
    private com.zqzqq.bootkits.web.scripts.storage.entity.ScriptTemplateEntity convertToTemplateEntity(ScriptTemplateDTO dto) {
        return com.zqzqq.bootkits.web.scripts.storage.entity.ScriptTemplateEntity.builder()
                .id(dto.getTemplateId())
                .templateId(dto.getTemplateId())
                .templateName(dto.getTemplateName())
                .displayName(dto.getDisplayName())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .tags(dto.getTags() != null ? String.join(",", dto.getTags()) : null)
                .scriptType(dto.getScriptType() != null ? dto.getScriptType().name() : null)
                .templateContent(dto.getTemplateContent())
                .parameters(dto.getParameters() != null ? toJson(dto.getParameters()) : null)
                .defaultValues(dto.getDefaultValues() != null ? toJson(dto.getDefaultValues()) : null)
                .usageCount(dto.getUsageCount())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
    
    private ScriptTemplateDTO convertToTemplateDTO(com.zqzqq.bootkits.web.scripts.storage.entity.ScriptTemplateEntity entity) {
        return ScriptTemplateDTO.builder()
                .templateId(entity.getTemplateId())
                .templateName(entity.getTemplateName())
                .displayName(entity.getDisplayName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .tags(entity.getTags() != null ? List.of(entity.getTags().split(",")) : null)
                .scriptType(entity.getScriptType() != null ? ScriptType.valueOf(entity.getScriptType()) : null)
                .templateContent(entity.getTemplateContent())
                .parameters(entity.getParameters() != null ? fromJson(entity.getParameters(), new TypeReference<List<ScriptTemplateDTO.TemplateParameter>>() {}) : new ArrayList<>())
                .defaultValues(entity.getDefaultValues() != null ? fromJson(entity.getDefaultValues(), new TypeReference<List<ScriptTemplateDTO.TemplateParameter>>() {}) : new ArrayList<>())
                .usageCount(entity.getUsageCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON", e);
            return null;
        }
    }
    
    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize JSON to object", e);
            return null;
        }
    }
}