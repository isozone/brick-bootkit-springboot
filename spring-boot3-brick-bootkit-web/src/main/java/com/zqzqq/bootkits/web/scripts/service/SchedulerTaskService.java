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


package com.zqzqq.bootkits.web.scripts.service;

import com.zqzqq.bootkits.web.scripts.dto.SchedulerTaskDTO;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 定时任务服务
 * 
 * @author brick-bootkit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerTaskService {
    
    private final ScriptStorage scriptStorage;
    
    public SchedulerTaskDTO createSchedulerTask(SchedulerTaskDTO task) {
        log.info("Creating scheduler task: {}", task.getTaskName());
        
        if (task.getTaskId() == null) {
            task.setTaskId(UUID.randomUUID().toString());
        }
        task.setCreatedAt(LocalDateTime.now());
        if (task.getTaskStatus() == null) {
            task.setTaskStatus("CREATED");
        }
        
        try {
            scriptStorage.saveSchedulerTask(task);
        } catch (StorageException e) {
            log.error("Failed to save scheduler task", e);
        }
        return task;
    }
    
    public Optional<SchedulerTaskDTO> getSchedulerTask(String taskId) {
        try {
            return scriptStorage.getSchedulerTask(taskId);
        } catch (StorageException e) {
            log.error("Failed to get scheduler task", e);
            return Optional.empty();
        }
    }
    
    public List<SchedulerTaskDTO> getAllSchedulerTasks() {
        try {
            return scriptStorage.getAllSchedulerTasks();
        } catch (StorageException e) {
            log.error("Failed to get all scheduler tasks", e);
            return List.of();
        }
    }
    
    public SchedulerTaskDTO updateSchedulerTask(String taskId, SchedulerTaskDTO updatedTask) {
        log.info("Updating scheduler task: {}", taskId);
        
        Optional<SchedulerTaskDTO> existingOpt = getSchedulerTask(taskId);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Scheduler task not found: " + taskId);
        }
        
        SchedulerTaskDTO existing = existingOpt.get();
        existing.setDescription(updatedTask.getDescription());
        existing.setCronExpression(updatedTask.getCronExpression());
        existing.setScriptName(updatedTask.getScriptName());
        existing.setParameters(updatedTask.getParameters());
        existing.setMaxConcurrentRuns(updatedTask.getMaxConcurrentRuns());
        existing.setTimeoutMinutes(updatedTask.getTimeoutMinutes());
        existing.setRetryCount(updatedTask.getRetryCount());
        existing.setRetryIntervalMinutes(updatedTask.getRetryIntervalMinutes());
        existing.setNotifyOnSuccess(updatedTask.getNotifyOnSuccess());
        existing.setNotifyOnFailure(updatedTask.getNotifyOnFailure());
        existing.setNotificationTarget(updatedTask.getNotificationTarget());
        
        try {
            scriptStorage.saveSchedulerTask(existing);
        } catch (StorageException e) {
            log.error("Failed to save scheduler task", e);
        }
        return existing;
    }
    
    public boolean deleteSchedulerTask(String taskId) {
        log.info("Deleting scheduler task: {}", taskId);
        try {
            scriptStorage.deleteSchedulerTask(taskId);
            return true;
        } catch (StorageException e) {
            log.error("Failed to delete scheduler task", e);
            return false;
        }
    }
    
    public SchedulerTaskDTO enableSchedulerTask(String taskId) {
        log.info("Enabling scheduler task: {}", taskId);
        
        return getSchedulerTask(taskId).map(task -> {
            task.setTaskStatus("ENABLED");
            try {
                scriptStorage.saveSchedulerTask(task);
            } catch (StorageException e) {
                log.error("Failed to save scheduler task", e);
            }
            return task;
        }).orElseThrow(() -> new IllegalArgumentException("Scheduler task not found: " + taskId));
    }
    
    public SchedulerTaskDTO disableSchedulerTask(String taskId) {
        log.info("Disabling scheduler task: {}", taskId);
        
        return getSchedulerTask(taskId).map(task -> {
            task.setTaskStatus("DISABLED");
            try {
                scriptStorage.saveSchedulerTask(task);
            } catch (StorageException e) {
                log.error("Failed to save scheduler task", e);
            }
            return task;
        }).orElseThrow(() -> new IllegalArgumentException("Scheduler task not found: " + taskId));
    }
    
    public SchedulerTaskDTO triggerSchedulerTask(String taskId) {
        log.info("Manually triggering scheduler task: {}", taskId);
        
        return getSchedulerTask(taskId).map(task -> {
            task.setLastStartTime(LocalDateTime.now());
            task.setManualTriggerCount(task.getManualTriggerCount() != null ? task.getManualTriggerCount() + 1 : 1);
            try {
                scriptStorage.saveSchedulerTask(task);
            } catch (StorageException e) {
                log.error("Failed to save scheduler task", e);
            }
            return task;
        }).orElseThrow(() -> new IllegalArgumentException("Scheduler task not found: " + taskId));
    }
    
    public List<SchedulerTaskDTO.ExecutionHistoryItem> getTaskExecutionHistory(String taskId) {
        return getSchedulerTask(taskId)
                .map(task -> {
                    SchedulerTaskDTO.ExecutionHistoryItem history = SchedulerTaskDTO.ExecutionHistoryItem.builder()
                            .executionId(task.getLastExecutionId())
                            .startTime(task.getLastExecutionStartTime())
                            .endTime(task.getLastExecutionEndTime())
                            .result(task.getLastExecutionResult())
                            .status(task.getLastExecutionStatus())
                            .build();
                    return List.of(history);
                })
                .orElse(List.of());
    }
}