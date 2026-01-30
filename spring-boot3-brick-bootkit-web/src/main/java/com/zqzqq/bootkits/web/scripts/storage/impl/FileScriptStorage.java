package com.zqzqq.bootkits.web.scripts.storage.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zqzqq.bootkits.web.scripts.dto.*;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorageType;
import com.zqzqq.bootkits.web.scripts.storage.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 文件存储实现（默认）
 * 使用JSON文件存储数据
 * 
 * @author brick-bootkit
 */
@Slf4j
public class FileScriptStorage implements ScriptStorage {
    
    private final String dataPath;
    private final ObjectMapper objectMapper;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public FileScriptStorage(@Value("${brick.scripts.storage.file.data-path:./brick-scripts-data}") String dataPath) {
        this.dataPath = dataPath;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    @Override
    public void initialize() throws StorageException {
        try {
            createDirectory(dataPath);
            createDirectory(dataPath + "/repository");
            createDirectory(dataPath + "/executions");
            createDirectory(dataPath + "/scheduler");
            createDirectory(dataPath + "/queue");
            createDirectory(dataPath + "/batch");
            createDirectory(dataPath + "/templates");
            log.info("FileScriptStorage initialized at: {}", dataPath);
        } catch (Exception e) {
            throw new StorageException("Failed to initialize file storage", e);
        }
    }
    
    @Override
    public ScriptStorageType getType() {
        return ScriptStorageType.FILE;
    }
    
    @Override
    public void saveScriptInfo(ScriptInfoDTO scriptInfo) throws StorageException {
        lock.writeLock().lock();
        try {
            Path metaPath = Paths.get(dataPath, "repository", scriptInfo.getScriptName() + ".json");
            objectMapper.writeValue(metaPath.toFile(), scriptInfo);
        } catch (IOException e) {
            throw new StorageException("Failed to save script info", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public Optional<ScriptInfoDTO> getScriptInfo(String scriptName) throws StorageException {
        lock.readLock().lock();
        try {
            Path metaPath = Paths.get(dataPath, "repository", scriptName + ".json");
            if (!Files.exists(metaPath)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(metaPath.toFile(), ScriptInfoDTO.class));
        } catch (IOException e) {
            throw new StorageException("Failed to get script info", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<ScriptInfoDTO> getAllScriptInfo() throws StorageException {
        lock.readLock().lock();
        try {
            Path repoPath = Paths.get(dataPath, "repository");
            if (!Files.exists(repoPath)) {
                return new ArrayList<>();
            }
            return Files.list(repoPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(p -> {
                        try {
                            return objectMapper.readValue(p.toFile(), ScriptInfoDTO.class);
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Failed to get all script info", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void deleteScriptInfo(String scriptName) throws StorageException {
        lock.writeLock().lock();
        try {
            Path metaPath = Paths.get(dataPath, "repository", scriptName + ".json");
            Path scriptPath = Paths.get(dataPath, "repository", scriptName);
            Files.deleteIfExists(metaPath);
            Files.deleteIfExists(scriptPath);
        } catch (IOException e) {
            throw new StorageException("Failed to delete script info", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void saveScriptContent(String scriptName, String content) throws StorageException {
        lock.writeLock().lock();
        try {
            Path scriptPath = Paths.get(dataPath, "repository", scriptName);
            Files.write(scriptPath, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new StorageException("Failed to save script content", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public Optional<String> getScriptContent(String scriptName) throws StorageException {
        lock.readLock().lock();
        try {
            Path scriptPath = Paths.get(dataPath, "repository", scriptName);
            if (!Files.exists(scriptPath)) {
                return Optional.empty();
            }
            return Optional.of(Files.readString(scriptPath, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new StorageException("Failed to get script content", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void saveScriptVersion(ScriptVersionDTO version) throws StorageException {
        // Simplified implementation
    }
    
    @Override
    public List<ScriptVersionDTO> getScriptVersions(String scriptName) throws StorageException {
        return new ArrayList<>();
    }
    
    @Override
    public void restoreScriptVersion(String scriptName, String version) throws StorageException {
    }
    
    @Override
    public void saveExecutionRecord(ExecutionRecordDTO record) throws StorageException {
        lock.writeLock().lock();
        try {
            Path execPath = Paths.get(dataPath, "executions", record.getExecutionId() + ".json");
            objectMapper.writeValue(execPath.toFile(), record);
        } catch (IOException e) {
            throw new StorageException("Failed to save execution record", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public Optional<ExecutionRecordDTO> getExecutionRecord(String executionId) throws StorageException {
        lock.readLock().lock();
        try {
            Path execPath = Paths.get(dataPath, "executions", executionId + ".json");
            if (!Files.exists(execPath)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(execPath.toFile(), ExecutionRecordDTO.class));
        } catch (IOException e) {
            throw new StorageException("Failed to get execution record", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<ExecutionRecordDTO> getExecutionRecords(int page, int size, String scriptName, String status) throws StorageException {
        return new ArrayList<>();
    }
    
    @Override
    public List<ExecutionRecordDTO> getExecutionRecords(String scriptName) throws StorageException {
        return new ArrayList<>();
    }
    
    @Override
    public List<ExecutionRecordDTO> getAllExecutionRecords() throws StorageException {
        return new ArrayList<>();
    }
    
    @Override
    public boolean deleteExecutionRecord(String scriptName, String executionId) throws StorageException {
        return false;
    }
    
    @Override
    public void appendExecutionLog(String executionId, String logLine) throws StorageException {
    }
    
    @Override
    public List<String> getExecutionLogs(String executionId) throws StorageException {
        return new ArrayList<>();
    }
    
    @Override
    public void saveSchedulerTask(SchedulerTaskDTO task) throws StorageException {
        lock.writeLock().lock();
        try {
            Path taskPath = Paths.get(dataPath, "scheduler", task.getTaskId() + ".json");
            objectMapper.writeValue(taskPath.toFile(), task);
        } catch (IOException e) {
            throw new StorageException("Failed to save scheduler task", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public Optional<SchedulerTaskDTO> getSchedulerTask(String taskId) throws StorageException {
        lock.readLock().lock();
        try {
            Path taskPath = Paths.get(dataPath, "scheduler", taskId + ".json");
            if (!Files.exists(taskPath)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(taskPath.toFile(), SchedulerTaskDTO.class));
        } catch (IOException e) {
            throw new StorageException("Failed to get scheduler task", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<SchedulerTaskDTO> getAllSchedulerTasks() throws StorageException {
        return new ArrayList<>();
    }
    
    @Override
    public void deleteSchedulerTask(String taskId) throws StorageException {
    }
    
    @Override
    public void updateTaskStatus(String taskId, String status) throws StorageException {
    }
    
    @Override
    public void enqueue(QueueItemDTO item) throws StorageException {
        lock.writeLock().lock();
        try {
            Path queuePath = Paths.get(dataPath, "queue", item.getItemId() + ".json");
            objectMapper.writeValue(queuePath.toFile(), item);
        } catch (IOException e) {
            throw new StorageException("Failed to enqueue", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public Optional<QueueItemDTO> dequeue() throws StorageException {
        return Optional.empty();
    }
    
    @Override
    public QueueStatusDTO getQueueStatus() throws StorageException {
        return QueueStatusDTO.builder().build();
    }
    
    @Override
    public void clearQueue() throws StorageException {
    }
    
    @Override
    public void saveBatchJob(BatchJobDTO job) throws StorageException {
        lock.writeLock().lock();
        try {
            Path jobPath = Paths.get(dataPath, "batch", job.getJobId() + ".json");
            objectMapper.writeValue(jobPath.toFile(), job);
        } catch (IOException e) {
            throw new StorageException("Failed to save batch job", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public Optional<BatchJobDTO> getBatchJob(String jobId) throws StorageException {
        lock.readLock().lock();
        try {
            Path jobPath = Paths.get(dataPath, "batch", jobId + ".json");
            if (!Files.exists(jobPath)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(jobPath.toFile(), BatchJobDTO.class));
        } catch (IOException e) {
            throw new StorageException("Failed to get batch job", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<BatchJobDTO> getAllBatchJobs() throws StorageException {
        return new ArrayList<>();
    }
    
    @Override
    public void deleteBatchJob(String jobId) throws StorageException {
    }
    
    @Override
    public void saveTemplate(ScriptTemplateDTO template) throws StorageException {
        lock.writeLock().lock();
        try {
            Path templatePath = Paths.get(dataPath, "templates", template.getTemplateId() + ".json");
            objectMapper.writeValue(templatePath.toFile(), template);
        } catch (IOException e) {
            throw new StorageException("Failed to save template", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public Optional<ScriptTemplateDTO> getTemplate(String templateId) throws StorageException {
        lock.readLock().lock();
        try {
            Path templatePath = Paths.get(dataPath, "templates", templateId + ".json");
            if (!Files.exists(templatePath)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(templatePath.toFile(), ScriptTemplateDTO.class));
        } catch (IOException e) {
            throw new StorageException("Failed to get template", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<ScriptTemplateDTO> getAllTemplates() throws StorageException {
        return new ArrayList<>();
    }
    
    @Override
    public void deleteTemplate(String templateId) throws StorageException {
    }
    
    @Override
    public void shutdown() throws StorageException {
    }
    
    private void createDirectory(String path) throws IOException {
        Files.createDirectories(Paths.get(path));
    }
}