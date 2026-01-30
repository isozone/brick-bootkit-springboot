package com.zqzqq.bootkits.web.scripts.controller;

import com.zqzqq.bootkits.web.scripts.dto.BatchJobDTO;
import com.zqzqq.bootkits.web.scripts.service.BatchJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 批处理任务Controller
 * 提供批量脚本执行的管理和监控功能
 * 
 * @author brick-bootkit
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchJobController {
    
    private final BatchJobService batchJobService;
    
    /**
     * 获取所有批处理任务
     */
    @GetMapping("/jobs")
    public ResponseEntity<List<BatchJobDTO>> getAllJobs() {
        return ResponseEntity.ok(batchJobService.getAllBatchJobs());
    }
    
    /**
     * 获取批处理任务
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<BatchJobDTO> getJob(@PathVariable String jobId) {
        return batchJobService.getBatchJob(jobId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 根据状态获取批处理任务
     */
    @GetMapping("/jobs/status/{status}")
    public ResponseEntity<List<BatchJobDTO>> getJobsByStatus(
            @PathVariable BatchJobDTO.BatchStatus status) {
        return ResponseEntity.ok(batchJobService.getBatchJobsByStatus(status));
    }
    
    /**
     * 获取正在运行的批处理任务
     */
    @GetMapping("/jobs/running")
    public ResponseEntity<List<BatchJobDTO>> getRunningJobs() {
        return ResponseEntity.ok(batchJobService.getRunningBatchJobs());
    }
    
    /**
     * 创建批处理任务
     */
    @PostMapping("/jobs")
    public ResponseEntity<BatchJobDTO> createJob(@RequestBody BatchJobDTO batchJob) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchJobService.createBatchJob(batchJob));
    }
    
    /**
     * 更新批处理任务
     */
    @PutMapping("/jobs/{jobId}")
    public ResponseEntity<BatchJobDTO> updateJob(@PathVariable String jobId,
                                                   @RequestBody BatchJobDTO batchJob) {
        return ResponseEntity.ok(batchJobService.updateBatchJob(jobId, batchJob));
    }
    
    /**
     * 删除批处理任务
     */
    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<Void> deleteJob(@PathVariable String jobId) {
        boolean deleted = batchJobService.deleteBatchJob(jobId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
    
    /**
     * 开始执行批处理任务
     */
    @PostMapping("/jobs/{jobId}/start")
    public ResponseEntity<BatchJobDTO> startJob(@PathVariable String jobId) {
        return ResponseEntity.ok(batchJobService.startBatchJob(jobId));
    }
    
    /**
     * 停止批处理任务
     */
    @PostMapping("/jobs/{jobId}/stop")
    public ResponseEntity<BatchJobDTO> stopJob(@PathVariable String jobId) {
        return ResponseEntity.ok(batchJobService.stopBatchJob(jobId));
    }
    
    /**
     * 获取批处理任务进度
     */
    @GetMapping("/jobs/{jobId}/progress")
    public ResponseEntity<BatchJobDTO> getJobProgress(@PathVariable String jobId) {
        return ResponseEntity.ok(batchJobService.getBatchJobProgress(jobId));
    }
}