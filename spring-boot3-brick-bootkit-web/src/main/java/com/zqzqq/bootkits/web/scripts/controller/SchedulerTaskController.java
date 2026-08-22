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

import com.zqzqq.bootkits.web.scripts.dto.SchedulerTaskDTO;
import com.zqzqq.bootkits.web.scripts.service.SchedulerTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定时任务Controller
 * 
 * @author brick-bootkit
 */
@Slf4j
@RestController
@RequestMapping("/plugins-web/api/v1/scheduler")
@RequiredArgsConstructor
public class SchedulerTaskController {
    
    private final SchedulerTaskService schedulerTaskService;
    
    @GetMapping("/tasks")
    public ResponseEntity<List<SchedulerTaskDTO>> getAllTasks() {
        return ResponseEntity.ok(schedulerTaskService.getAllSchedulerTasks());
    }
    
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<SchedulerTaskDTO> getTask(@PathVariable String taskId) {
        return schedulerTaskService.getSchedulerTask(taskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/tasks/status/{status}")
    public ResponseEntity<List<SchedulerTaskDTO>> getTasksByStatus(@PathVariable String status) {
        return ResponseEntity.ok(schedulerTaskService.getAllSchedulerTasks().stream()
                .filter(t -> status.equals(t.getTaskStatus()))
                .toList());
    }
    
    @GetMapping("/tasks/running")
    public ResponseEntity<List<SchedulerTaskDTO>> getRunningTasks() {
        return ResponseEntity.ok(schedulerTaskService.getAllSchedulerTasks().stream()
                .filter(t -> "ENABLED".equals(t.getTaskStatus()))
                .toList());
    }
    
    @PostMapping("/tasks")
    public ResponseEntity<SchedulerTaskDTO> createTask(@RequestBody SchedulerTaskDTO task) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulerTaskService.createSchedulerTask(task));
    }
    
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<SchedulerTaskDTO> updateTask(@PathVariable String taskId,
                                                        @RequestBody SchedulerTaskDTO task) {
        return ResponseEntity.ok(schedulerTaskService.updateSchedulerTask(taskId, task));
    }
    
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable String taskId) {
        boolean deleted = schedulerTaskService.deleteSchedulerTask(taskId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
    
    @PostMapping("/tasks/{taskId}/enable")
    public ResponseEntity<SchedulerTaskDTO> enableTask(@PathVariable String taskId) {
        return ResponseEntity.ok(schedulerTaskService.enableSchedulerTask(taskId));
    }
    
    @PostMapping("/tasks/{taskId}/disable")
    public ResponseEntity<SchedulerTaskDTO> disableTask(@PathVariable String taskId) {
        return ResponseEntity.ok(schedulerTaskService.disableSchedulerTask(taskId));
    }
    
    @PostMapping("/tasks/{taskId}/trigger")
    public ResponseEntity<SchedulerTaskDTO> triggerTask(@PathVariable String taskId) {
        return ResponseEntity.ok(schedulerTaskService.triggerSchedulerTask(taskId));
    }
    
    @GetMapping("/tasks/{taskId}/history")
    public ResponseEntity<List<SchedulerTaskDTO.ExecutionHistoryItem>> getTaskHistory(@PathVariable String taskId) {
        return ResponseEntity.ok(schedulerTaskService.getTaskExecutionHistory(taskId));
    }
}
