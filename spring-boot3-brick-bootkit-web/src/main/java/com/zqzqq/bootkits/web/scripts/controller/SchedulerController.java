package com.zqzqq.bootkits.web.scripts.controller;

import com.zqzqq.bootkits.web.scripts.dto.SchedulerTaskDTO;
import com.zqzqq.bootkits.web.scripts.service.SchedulerService;
import com.zqzqq.bootkits.web.scripts.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务控制器
 * 提供定时任务的REST API接口
 * 
 * @author brick-bootkit
 */
@Slf4j
@RestController
@RequestMapping("/plugins-web/api/v1/scripts/scheduler")
@RequiredArgsConstructor
public class SchedulerController {
    
    private final SchedulerService schedulerService;
    
    /**
     * 获取所有定时任务
     */
    @GetMapping
    public ResponseEntity<?> getAllTasks() {
        try {
            List<SchedulerTaskDTO> tasks = schedulerService.getAllTasks();
            return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", tasks,
                "total", tasks.size()
            ));
        } catch (StorageException e) {
            log.error("Failed to get all scheduler tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", 500, "message", "获取定时任务列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取单个定时任务
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTask(@PathVariable String taskId) {
        try {
            return schedulerService.getTask(taskId)
                .map(task -> ResponseEntity.ok(Map.of("code", 200, "data", task)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", 404, "message", "任务不存在")));
        } catch (StorageException e) {
            log.error("Failed to get scheduler task: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", 500, "message", "获取任务失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建定时任务
     */
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody SchedulerTaskDTO taskDTO) {
        try {
            SchedulerTaskDTO createdTask = schedulerService.createTask(taskDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("code", 200, "message", "创建成功", "data", createdTask));
        } catch (StorageException e) {
            log.error("Failed to create scheduler task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", 500, "message", "创建失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新定时任务
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable String taskId, @RequestBody SchedulerTaskDTO taskDTO) {
        try {
            SchedulerTaskDTO updatedTask = schedulerService.updateTask(taskId, taskDTO);
            return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功", "data", updatedTask));
        } catch (StorageException e) {
            log.error("Failed to update scheduler task: {}", taskId, e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", 404, "message", "任务不存在"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", 500, "message", "更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除定时任务
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable String taskId) {
        try {
            boolean deleted = schedulerService.deleteTask(taskId);
            if (deleted) {
                return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", 404, "message", "任务不存在"));
            }
        } catch (StorageException e) {
            log.error("Failed to delete scheduler task: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", 500, "message", "删除失败: " + e.getMessage()));
        }
    }
    
    /**
     * 启用定时任务
     */
    @PostMapping("/{taskId}/enable")
    public ResponseEntity<?> enableTask(@PathVariable String taskId) {
        try {
            SchedulerTaskDTO task = schedulerService.enableTask(taskId);
            if (task != null) {
                return ResponseEntity.ok(Map.of("code", 200, "message", "启用成功", "data", task));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", 404, "message", "任务不存在"));
            }
        } catch (StorageException e) {
            log.error("Failed to enable scheduler task: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", 500, "message", "启用失败: " + e.getMessage()));
        }
    }
    
    /**
     * 禁用定时任务
     */
    @PostMapping("/{taskId}/disable")
    public ResponseEntity<?> disableTask(@PathVariable String taskId) {
        try {
            SchedulerTaskDTO task = schedulerService.disableTask(taskId);
            if (task != null) {
                return ResponseEntity.ok(Map.of("code", 200, "message", "禁用成功", "data", task));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", 404, "message", "任务不存在"));
            }
        } catch (StorageException e) {
            log.error("Failed to disable scheduler task: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", 500, "message", "禁用失败: " + e.getMessage()));
        }
    }
    
    /**
     * 暂停定时任务（等同于禁用）
     */
    @PostMapping("/{taskId}/pause")
    public ResponseEntity<?> pauseTask(@PathVariable String taskId) {
        return disableTask(taskId);
    }
    
    /**
     * 立即执行定时任务
     */
    @PostMapping("/{taskId}/execute")
    public ResponseEntity<?> executeTask(@PathVariable String taskId) {
        try {
            schedulerService.executeTask(taskId);
            return ResponseEntity.ok(Map.of("code", 200, "message", "任务已提交执行"));
        } catch (StorageException e) {
            log.error("Failed to execute scheduler task: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", 500, "message", "执行失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取调度器状态
     * 注意：目前返回模拟状态
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSchedulerStatus() {
        try {
            List<SchedulerTaskDTO> tasks = schedulerService.getAllTasks();
            
            Map<String, Object> status = new HashMap<>();
            status.put("scheduledTasks", tasks.size());
            status.put("runningTasks", tasks.stream()
                .filter(t -> SchedulerTaskDTO.SchedulerStatus.RUNNING.name().equals(t.getTaskStatus()))
                .count());
            status.put("enabledTasks", tasks.stream()
                .filter(t -> SchedulerTaskDTO.SchedulerStatus.ENABLED.name().equals(t.getTaskStatus()))
                .count());
            
            return ResponseEntity.ok(Map.of("code", 200, "data", status));
        } catch (Exception e) {
            log.error("Failed to get scheduler status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", 500, "message", "获取状态失败: " + e.getMessage()));
        }
    }
}