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

import com.zqzqq.bootkits.web.scripts.dto.QueueItemDTO;
import com.zqzqq.bootkits.web.scripts.dto.QueueStatusDTO;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 脚本队列服务
 * 提供脚本队列的管理和执行功能
 * 
 * @author brick-bootkit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {
    
    private final ScriptStorage scriptStorage;
    
    /**
     * 入队
     */
    public QueueItemDTO enqueue(String queueName, QueueItemDTO item) {
        log.info("Enqueuing item to queue: {}, script: {}", queueName, item.getScriptName());
        
        if (item.getItemId() == null) {
            item.setItemId(UUID.randomUUID().toString());
        }
        item.setQueueStatus(QueueItemDTO.QueueItemStatus.PENDING);
        item.setScheduledAt(LocalDateTime.now());
        
        try {
            scriptStorage.enqueue(item);
        } catch (StorageException e) {
            log.error("Failed to enqueue item", e);
        }
        return item;
    }
    
    /**
     * 出队（获取下一个待执行项）
     */
    public Optional<QueueItemDTO> dequeue(String queueName) {
        log.debug("Dequeuing from queue: {}", queueName);
        
        try {
            Optional<QueueItemDTO> item = scriptStorage.dequeue();
            return item;
        } catch (StorageException e) {
            log.error("Failed to dequeue item", e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取队列状态
     */
    public QueueStatusDTO getQueueStatus(String queueName) {
        try {
            return scriptStorage.getQueueStatus();
        } catch (StorageException e) {
            log.error("Failed to get queue status", e);
            return QueueStatusDTO.builder().build();
        }
    }
    
    /**
     * 清空队列
     */
    public void clearQueue() {
        log.info("Clearing queue");
        try {
            scriptStorage.clearQueue();
        } catch (StorageException e) {
            log.error("Failed to clear queue", e);
        }
    }
}
