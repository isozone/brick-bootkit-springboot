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


package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.web.config.BrickWebProperties;
import com.zqzqq.bootkits.web.dto.PageResult;
import com.zqzqq.bootkits.web.dto.PluginUploadHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 插件上传历史服务
 * 负责记录和查询插件上传历史
 * 
 * @author brick-bootkit
 */
@Slf4j
@Service
public class UploadHistoryService {

    private static final int MAX_RECORDS = 1000; // 最大记录数
    private static final String HISTORY_FILE_NAME = "upload-history.json";
    
    @Autowired
    private BrickWebProperties properties;
    
    // 内存中的历史记录
    private final List<PluginUploadHistory> records = new CopyOnWriteArrayList<>();
    
    // 读写锁保证并发安全
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    private Path historyFilePath;
    
    @PostConstruct
    public void init() {
        try {
            // 确定历史文件存储路径
            String historyPath = System.getProperty("upload.history.path");
            if (historyPath == null || historyPath.isEmpty()) {
                // 默认存储在项目根目录下的 upload-history 文件夹
                String projectRoot = System.getProperty("user.dir");
                Path historyDir = Paths.get(projectRoot, "upload-history");
                if (!Files.exists(historyDir)) {
                    Files.createDirectories(historyDir);
                }
                historyFilePath = historyDir.resolve(HISTORY_FILE_NAME);
            } else {
                historyFilePath = Paths.get(historyPath, HISTORY_FILE_NAME);
            }
            
            // 加载历史记录
            loadHistory();
            log.info("上传历史服务初始化完成，历史文件路径: {}", historyFilePath);
        } catch (Exception e) {
            log.error("上传历史服务初始化失败", e);
        }
    }
    
    @PreDestroy
    public void destroy() {
        try {
            saveHistory();
            log.info("上传历史服务已关闭");
        } catch (Exception e) {
            log.error("保存上传历史失败", e);
        }
    }
    
    /**
     * 记录上传历史
     * 
     * @param history 上传历史记录
     */
    public void recordUpload(PluginUploadHistory history) {
        lock.writeLock().lock();
        try {
            // 添加到列表开头（最新的在最前面）
            records.add(0, history);
            
            // 限制最大记录数
            if (records.size() > MAX_RECORDS) {
                records.remove(records.size() - 1);
            }
            
            // 保存到文件
            saveHistory();
            
            log.info("记录上传历史: pluginId={}, version={}, status={}", 
                    history.getPluginId(), history.getVersion(), history.getStatus());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 查询上传历史
     * 
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param pluginId 插件ID（可选）
     * @param status 上传状态（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 分页结果
     */
    public PageResult<PluginUploadHistory> queryHistory(int page, int size, 
            String pluginId, PluginUploadHistory.UploadStatus status, 
            LocalDate startDate, LocalDate endDate) {
        
        lock.readLock().lock();
        try {
            // 筛选
            List<PluginUploadHistory> filtered = records.stream()
                    .filter(record -> {
                        // 插件ID筛选
                        if (pluginId != null && !pluginId.isEmpty() && 
                            !pluginId.equals(record.getPluginId())) {
                            return false;
                        }
                        
                        // 状态筛选
                        if (status != null && !status.equals(record.getStatus())) {
                            return false;
                        }
                        
                        // 日期范围筛选
                        if (startDate != null && record.getUploadTime().toLocalDate().isBefore(startDate)) {
                            return false;
                        }
                        if (endDate != null && record.getUploadTime().toLocalDate().isAfter(endDate)) {
                            return false;
                        }
                        
                        return true;
                    })
                    .collect(Collectors.toList());
            
            // 分页
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, filtered.size());
            
            if (fromIndex >= filtered.size()) {
                return PageResult.of(List.of(), filtered.size(), page, size);
            }
            
            List<PluginUploadHistory> pageRecords = filtered.subList(fromIndex, toIndex);
            return PageResult.of(pageRecords, filtered.size(), page, size);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取所有上传历史
     * 
     * @return 所有上传历史记录
     */
    public List<PluginUploadHistory> getAllHistory() {
        lock.readLock().lock();
        try {
            return new CopyOnWriteArrayList<>(records);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 根据上传ID获取历史记录
     * 
     * @param uploadId 上传ID
     * @return 上传历史记录
     */
    public PluginUploadHistory getHistoryById(String uploadId) {
        lock.readLock().lock();
        try {
            return records.stream()
                    .filter(record -> uploadId.equals(record.getUploadId()))
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 删除指定的上传历史记录
     * 
     * @param uploadId 上传ID
     * @return 是否删除成功
     */
    public boolean deleteHistory(String uploadId) {
        lock.writeLock().lock();
        try {
            boolean removed = records.removeIf(record -> uploadId.equals(record.getUploadId()));
            if (removed) {
                saveHistory();
                log.info("删除上传历史: uploadId={}", uploadId);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 删除指定日期之前的所有历史记录
     * 
     * @param beforeDate 日期
     * @return 删除的记录数
     */
    public int deleteHistoryBefore(LocalDate beforeDate) {
        lock.writeLock().lock();
        try {
            int originalSize = records.size();
            records.removeIf(record -> record.getUploadTime().toLocalDate().isBefore(beforeDate));
            int deletedCount = originalSize - records.size();
            
            if (deletedCount > 0) {
                saveHistory();
                log.info("删除 {} 条上传历史记录（日期之前: {}）", deletedCount, beforeDate);
            }
            
            return deletedCount;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 清空所有历史记录
     */
    public void clearAllHistory() {
        lock.writeLock().lock();
        try {
            int size = records.size();
            records.clear();
            saveHistory();
            log.info("清空所有上传历史记录，共 {} 条", size);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 从文件加载历史记录
     */
    private void loadHistory() {
        lock.writeLock().lock();
        try {
            if (historyFilePath == null || !Files.exists(historyFilePath)) {
                log.info("上传历史文件不存在，跳过加载");
                return;
            }
            
            String content = Files.readString(historyFilePath);
            if (content == null || content.trim().isEmpty()) {
                log.info("上传历史文件为空");
                return;
            }
            
            // 使用 Hutool 的 JSONUtil 解析 JSON
            cn.hutool.json.JSONArray jsonArray = cn.hutool.json.JSONUtil.parseArray(content);
            
            for (int i = 0; i < jsonArray.size(); i++) {
                cn.hutool.json.JSONObject jsonObject = jsonArray.getJSONObject(i);
                PluginUploadHistory history = new PluginUploadHistory();
                
                history.setUploadId(jsonObject.getStr("uploadId"));
                history.setPluginId(jsonObject.getStr("pluginId"));
                history.setPluginName(jsonObject.getStr("pluginName"));
                history.setVersion(jsonObject.getStr("version"));
                history.setUploadTime(jsonObject.get("uploadTime", LocalDateTime.class));
                history.setStatus(PluginUploadHistory.UploadStatus.valueOf(jsonObject.getStr("status")));
                history.setFilePath(jsonObject.getStr("filePath"));
                history.setFileSize(jsonObject.getLong("fileSize"));
                history.setAutoStart(jsonObject.getBool("autoStart"));
                history.setBackupPath(jsonObject.getStr("backupPath"));
                history.setErrorMessage(jsonObject.getStr("errorMessage"));
                
                records.add(history);
            }
            
            log.info("加载 {} 条上传历史记录", records.size());
            
        } catch (Exception e) {
            log.error("加载上传历史失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 保存历史记录到文件
     */
    private void saveHistory() {
        try {
            if (historyFilePath == null) {
                return;
            }
            
            // 确保目录存在
            if (!Files.exists(historyFilePath.getParent())) {
                Files.createDirectories(historyFilePath.getParent());
            }
            
            // 构建JSON
            StringBuilder json = new StringBuilder();
            json.append("[\n");
            
            for (int i = 0; i < records.size(); i++) {
                PluginUploadHistory record = records.get(i);
                json.append("  {\n");
                json.append("    \"uploadId\": \"").append(escapeJson(record.getUploadId())).append("\",\n");
                json.append("    \"pluginId\": \"").append(escapeJson(record.getPluginId())).append("\",\n");
                json.append("    \"pluginName\": \"").append(escapeJson(record.getPluginName())).append("\",\n");
                json.append("    \"version\": \"").append(escapeJson(record.getVersion())).append("\",\n");
                json.append("    \"uploadTime\": \"").append(record.getUploadTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
                json.append("    \"status\": \"").append(record.getStatus().name()).append("\",\n");
                json.append("    \"filePath\": \"").append(escapeJson(record.getFilePath())).append("\",\n");
                json.append("    \"fileSize\": ").append(record.getFileSize()).append(",\n");
                json.append("    \"autoStart\": ").append(record.getAutoStart()).append(",\n");
                json.append("    \"backupPath\": \"").append(escapeJson(record.getBackupPath())).append("\",\n");
                json.append("    \"errorMessage\": \"").append(escapeJson(record.getErrorMessage())).append("\"\n");
                json.append("  }");
                
                if (i < records.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            
            json.append("]\n");
            
            // 写入文件
            Files.writeString(historyFilePath, json.toString());
            
        } catch (Exception e) {
            log.error("保存上传历史失败", e);
        }
    }
    
    /**
     * 转义 JSON 字符串
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
    
    /**
     * 生成上传ID（时间戳+序列号）
     */
    public static String generateUploadId() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}
