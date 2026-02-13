package com.zqzqq.bootkits.web.scripts.storage.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zqzqq.bootkits.web.scripts.dto.*;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorageType;
import com.zqzqq.bootkits.web.scripts.storage.StorageException;
import jakarta.annotation.PostConstruct;
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
    
    public FileScriptStorage(@Value("${plugin.scripts.storage.file.data-path:./plugins-scripts-data}") String dataPath) {
        this.dataPath = dataPath;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    @PostConstruct
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
            Path repoPath = Paths.get(dataPath, "repository");
            createDirectory(repoPath.toString());
            Path metaPath = Paths.get(dataPath, "repository", scriptInfo.getScriptName() + ".json");
            
            // 如果是新建脚本，设置创建时间和更新时间
            LocalDateTime now = LocalDateTime.now();
            if (scriptInfo.getCreatedAt() == null) {
                scriptInfo.setCreatedAt(now);
            }
            scriptInfo.setUpdatedAt(now);
            
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
            Path repoPath = Paths.get(dataPath, "repository");
            createDirectory(repoPath.toString());
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
            Path execDir = Paths.get(dataPath, "executions");
            // 确保目录存在
            if (!Files.exists(execDir)) {
                Files.createDirectories(execDir);
            }
            Path execPath = execDir.resolve(record.getExecutionId() + ".json");
            objectMapper.writeValue(execPath.toFile(), record);
        } catch (IOException e) {
            throw new StorageException("Failed to save execution record: " + e.getMessage(), e);
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
        lock.readLock().lock();
        try {
            Path execDir = Paths.get(dataPath, "executions");
            if (!Files.exists(execDir)) {
                return new ArrayList<>();
            }
            
            return Files.list(execDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> {
                        try {
                            ExecutionRecordDTO record = objectMapper.readValue(path.toFile(), ExecutionRecordDTO.class);
                            // 只返回指定脚本名称的记录
                            if (scriptName != null && scriptName.equals(record.getScriptName())) {
                                return record;
                            }
                            return null;
                        } catch (IOException e) {
                            log.warn("Failed to read execution record: {}", path);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> {
                        // 按开始时间倒序排序
                        if (b.getStartTime() == null) return 1;
                        if (a.getStartTime() == null) return -1;
                        return b.getStartTime().compareTo(a.getStartTime());
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Failed to get execution records", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<ExecutionRecordDTO> getAllExecutionRecords() throws StorageException {
        lock.readLock().lock();
        try {
            Path execDir = Paths.get(dataPath, "executions");
            if (!Files.exists(execDir)) {
                return new ArrayList<>();
            }
            
            return Files.list(execDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> {
                        try {
                            return objectMapper.readValue(path.toFile(), ExecutionRecordDTO.class);
                        } catch (IOException e) {
                            log.warn("Failed to read execution record: {}", path);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> {
                        // 按开始时间倒序排序
                        if (b.getStartTime() == null) return 1;
                        if (a.getStartTime() == null) return -1;
                        return b.getStartTime().compareTo(a.getStartTime());
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Failed to get all execution records", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public boolean deleteExecutionRecord(String scriptName, String executionId) throws StorageException {
        lock.writeLock().lock();
        try {
            Path execPath = Paths.get(dataPath, "executions", executionId + ".json");
            if (Files.exists(execPath)) {
                Files.delete(execPath);
                return true;
            }
            return false;
        } catch (IOException e) {
            throw new StorageException("Failed to delete execution record", e);
        } finally {
            lock.writeLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            Path schedulerPath = Paths.get(dataPath, "scheduler");
            if (!Files.exists(schedulerPath)) {
                return new ArrayList<>();
            }
            return Files.list(schedulerPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(p -> {
                        try {
                            return objectMapper.readValue(p.toFile(), SchedulerTaskDTO.class);
                        } catch (IOException e) {
                            log.warn("Failed to read scheduler task file: {}", p);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> {
                        if (b.getCreatedAt() == null) return 1;
                        if (a.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Failed to get all scheduler tasks", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void deleteSchedulerTask(String taskId) throws StorageException {
        lock.writeLock().lock();
        try {
            Path taskPath = Paths.get(dataPath, "scheduler", taskId + ".json");
            Files.deleteIfExists(taskPath);
        } catch (IOException e) {
            throw new StorageException("Failed to delete scheduler task", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void updateTaskStatus(String taskId, String status) throws StorageException {
        lock.writeLock().lock();
        try {
            Path taskPath = Paths.get(dataPath, "scheduler", taskId + ".json");
            if (!Files.exists(taskPath)) {
                return;
            }
            SchedulerTaskDTO task = objectMapper.readValue(taskPath.toFile(), SchedulerTaskDTO.class);
            task.setTaskStatus(status);
            task.setUpdatedAt(LocalDateTime.now());
            objectMapper.writeValue(taskPath.toFile(), task);
        } catch (IOException e) {
            throw new StorageException("Failed to update scheduler task status", e);
        } finally {
            lock.writeLock().unlock();
        }
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
            createDirectory(templatePath.getParent().toString());
            
            // 如果是新建模板，设置创建时间和更新时间
            LocalDateTime now = LocalDateTime.now();
            if (template.getCreatedAt() == null) {
                template.setCreatedAt(now);
            }
            template.setUpdatedAt(now);
            
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
        lock.readLock().lock();
        try {
            Path templatesPath = Paths.get(dataPath, "templates");
            if (!Files.exists(templatesPath)) {
                return new ArrayList<>();
            }
            return Files.list(templatesPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(p -> {
                        try {
                            return objectMapper.readValue(p.toFile(), ScriptTemplateDTO.class);
                        } catch (IOException e) {
                            log.warn("Failed to read template file: {}", p);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Failed to get all templates", e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void deleteTemplate(String templateId) throws StorageException {
        lock.writeLock().lock();
        try {
            Path templatePath = Paths.get(dataPath, "templates", templateId + ".json");
            Files.deleteIfExists(templatePath);
        } catch (IOException e) {
            throw new StorageException("Failed to delete template", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void shutdown() throws StorageException {
    }
    
    private void createDirectory(String path) throws IOException {
        Files.createDirectories(Paths.get(path));
    }
}