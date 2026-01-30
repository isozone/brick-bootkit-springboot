package com.zqzqq.bootkits.web.scripts.service;

import com.zqzqq.bootkits.web.scripts.dto.BatchJobDTO;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 批处理任务服务
 * 提供批量脚本执行的管理和监控功能
 * 
 * @author brick-bootkit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobService {
    
    private final ScriptStorage scriptStorage;
    @Lazy
    private final ScriptExecutionService executionService;
    
    /**
     * 创建批处理任务
     */
    public BatchJobDTO createBatchJob(BatchJobDTO batchJob) {
        log.info("Creating batch job: {}", batchJob.getJobName());
        
        if (batchJob.getJobId() == null) {
            batchJob.setJobId(UUID.randomUUID().toString());
        }
        batchJob.setCreatedAt(LocalDateTime.now());
        if (batchJob.getJobStatus() == null) {
            batchJob.setJobStatus(BatchJobDTO.BatchStatus.CREATED);
        }
        batchJob.setTotalItems(batchJob.getScriptNames() != null ? batchJob.getScriptNames().size() : 0);
        batchJob.setProcessedItems(0);
        batchJob.setSuccessItems(0);
        batchJob.setFailedItems(0);
        batchJob.setProcessedScripts(new ArrayList<>());
        
        try {
            scriptStorage.saveBatchJob(batchJob);
        } catch (StorageException e) {
            log.error("Failed to save batch job", e);
        }
        return batchJob;
    }
    
    /**
     * 获取批处理任务
     */
    public Optional<BatchJobDTO> getBatchJob(String jobId) {
        try {
            return scriptStorage.getBatchJob(jobId);
        } catch (StorageException e) {
            log.error("Failed to get batch job", e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取所有批处理任务
     */
    public List<BatchJobDTO> getAllBatchJobs() {
        try {
            return scriptStorage.getAllBatchJobs();
        } catch (StorageException e) {
            log.error("Failed to get all batch jobs", e);
            return List.of();
        }
    }
    
    /**
     * 根据状态获取批处理任务
     */
    public List<BatchJobDTO> getBatchJobsByStatus(BatchJobDTO.BatchStatus status) {
        return getAllBatchJobs().stream()
                .filter(j -> status.equals(j.getJobStatus()))
                .toList();
    }
    
    /**
     * 获取正在运行的批处理任务
     */
    public List<BatchJobDTO> getRunningBatchJobs() {
        return getAllBatchJobs().stream()
                .filter(j -> BatchJobDTO.BatchStatus.RUNNING.equals(j.getJobStatus()))
                .toList();
    }
    
    /**
     * 更新批处理任务
     */
    public BatchJobDTO updateBatchJob(String jobId, BatchJobDTO updatedJob) {
        log.info("Updating batch job: {}", jobId);
        
        Optional<BatchJobDTO> existingOpt = getBatchJob(jobId);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Batch job not found: " + jobId);
        }
        
        BatchJobDTO existing = existingOpt.get();
        existing.setDescription(updatedJob.getDescription());
        existing.setScriptNames(updatedJob.getScriptNames());
        existing.setParameters(updatedJob.getParameters());
        existing.setExecutionMode(updatedJob.getExecutionMode());
        existing.setTotalItems(existing.getScriptNames() != null ? existing.getScriptNames().size() : 0);
        
        try {
            scriptStorage.saveBatchJob(existing);
        } catch (StorageException e) {
            log.error("Failed to save batch job", e);
        }
        return existing;
    }
    
    /**
     * 删除批处理任务
     */
    public boolean deleteBatchJob(String jobId) {
        log.info("Deleting batch job: {}", jobId);
        try {
            scriptStorage.deleteBatchJob(jobId);
            return true;
        } catch (StorageException e) {
            log.error("Failed to delete batch job", e);
            return false;
        }
    }
    
    /**
     * 开始执行批处理任务
     */
    public BatchJobDTO startBatchJob(String jobId) {
        log.info("Starting batch job: {}", jobId);
        
        return getBatchJob(jobId).map(job -> {
            if (!BatchJobDTO.BatchStatus.CREATED.equals(job.getJobStatus()) &&
                !BatchJobDTO.BatchStatus.PAUSED.equals(job.getJobStatus())) {
                throw new IllegalStateException("Cannot start batch job in status: " + job.getJobStatus());
            }
            
            job.setJobStatus(BatchJobDTO.BatchStatus.RUNNING);
            job.setStartTime(LocalDateTime.now());
            try {
                scriptStorage.saveBatchJob(job);
            } catch (StorageException e) {
                log.error("Failed to save batch job", e);
            }
            
            return job;
        }).orElseThrow(() -> new IllegalArgumentException("Batch job not found: " + jobId));
    }
    
    /**
     * 停止批处理任务
     */
    public BatchJobDTO stopBatchJob(String jobId) {
        log.info("Stopping batch job: {}", jobId);
        
        return getBatchJob(jobId).map(job -> {
            job.setJobStatus(BatchJobDTO.BatchStatus.STOPPED);
            job.setEndTime(LocalDateTime.now());
            try {
                scriptStorage.saveBatchJob(job);
            } catch (StorageException e) {
                log.error("Failed to save batch job", e);
            }
            return job;
        }).orElseThrow(() -> new IllegalArgumentException("Batch job not found: " + jobId));
    }
    
    /**
     * 记录任务执行进度
     */
    public void recordItemProgress(String jobId, String scriptName, boolean success) {
        getBatchJob(jobId).ifPresent(job -> {
            job.setProcessedItems(job.getProcessedItems() != null ? job.getProcessedItems() + 1 : 1);
            if (success) {
                job.setSuccessItems(job.getSuccessItems() != null ? job.getSuccessItems() + 1 : 1);
            } else {
                job.setFailedItems(job.getFailedItems() != null ? job.getFailedItems() + 1 : 1);
            }
            
            // 更新进度百分比
            if (job.getTotalItems() != null && job.getTotalItems() > 0) {
                job.setProgress((int) ((double) job.getProcessedItems() / job.getTotalItems() * 100));
            }
            
            job.setLastProcessedScript(scriptName);
            job.setLastProcessedTime(LocalDateTime.now());
            
            // 检查是否完成
            if (job.getProcessedItems() != null && job.getTotalItems() != null && 
                job.getProcessedItems() >= job.getTotalItems()) {
                job.setJobStatus(BatchJobDTO.BatchStatus.COMPLETED);
                job.setEndTime(LocalDateTime.now());
            }
            
            try {
                scriptStorage.saveBatchJob(job);
            } catch (StorageException e) {
                log.error("Failed to save batch job", e);
            }
        });
    }
    
    /**
     * 获取批处理任务进度
     */
    public BatchJobDTO getBatchJobProgress(String jobId) {
        return getBatchJob(jobId).orElse(null);
    }
}