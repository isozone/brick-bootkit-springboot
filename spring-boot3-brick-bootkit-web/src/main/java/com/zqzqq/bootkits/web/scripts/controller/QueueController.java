package com.zqzqq.bootkits.web.scripts.controller;

import com.zqzqq.bootkits.web.scripts.dto.QueueItemDTO;
import com.zqzqq.bootkits.web.scripts.dto.QueueStatusDTO;
import com.zqzqq.bootkits.web.scripts.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 队列Controller
 * 提供队列的管理和操作功能
 * 
 * @author brick-bootkit
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/queues")
@RequiredArgsConstructor
public class QueueController {
    
    private final QueueService queueService;
    
    /**
     * 获取队列状态
     */
    @GetMapping
    public ResponseEntity<QueueStatusDTO> getQueueStatus() {
        return ResponseEntity.ok(queueService.getQueueStatus(null));
    }
    
    /**
     * 入队
     */
    @PostMapping("/enqueue")
    public ResponseEntity<QueueItemDTO> enqueue(@RequestBody QueueItemDTO item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(queueService.enqueue("default", item));
    }
    
    /**
     * 出队
     */
    @PostMapping("/dequeue")
    public ResponseEntity<QueueItemDTO> dequeue() {
        return queueService.dequeue("default")
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
    
    /**
     * 清空队列
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearQueue() {
        queueService.clearQueue();
        return ResponseEntity.ok().build();
    }
}