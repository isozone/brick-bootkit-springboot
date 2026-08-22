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


package com.zqzqq.bootkits.scripts.template;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板查询条件
 * 用于构建复杂的模板查询条件
 *
 * @author starBlues
 * @since 4.0.1
 */
public class TemplateQueryCriteria {
    
    private String namePattern;
    private String descriptionPattern;
    private String category;
    private List<String> tags;
    private String author;
    private String scriptType;
    private Boolean isActive;
    private Long minUsageCount;
    private Long maxUsageCount;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private LocalDateTime modifiedAfter;
    private LocalDateTime modifiedBefore;
    private List<String> templateIds;
    private int offset;
    private int limit;
    private String sortBy;
    private SortOrder sortOrder;
    
    /**
     * 排序方式
     */
    public enum SortOrder {
        ASC,
        DESC
    }
    
    /**
     * 构造函数
     */
    public TemplateQueryCriteria() {
        this.tags = new ArrayList<>();
        this.templateIds = new ArrayList<>();
        this.offset = 0;
        this.limit = 100;
        this.sortOrder = SortOrder.ASC;
    }
    
    /**
     * 设置名称模式
     *
     * @param namePattern 名称模式（支持通配符）
     * @return 查询条件
     */
    public TemplateQueryCriteria namePattern(String namePattern) {
        this.namePattern = namePattern;
        return this;
    }
    
    /**
     * 设置描述模式
     *
     * @param descriptionPattern 描述模式（支持通配符）
     * @return 查询条件
     */
    public TemplateQueryCriteria descriptionPattern(String descriptionPattern) {
        this.descriptionPattern = descriptionPattern;
        return this;
    }
    
    /**
     * 设置分类
     *
     * @param category 分类
     * @return 查询条件
     */
    public TemplateQueryCriteria categoryMethod(String category) {
        this.category = category;
        return this;
    }
    
    /**
     * 添加标签
     *
     * @param tag 标签
     * @return 查询条件
     */
    public TemplateQueryCriteria tag(String tag) {
        this.tags.add(tag);
        return this;
    }
    
    /**
     * 设置标签列表
     *
     * @param tags 标签列表
     * @return 查询条件
     */
    public TemplateQueryCriteria tags(List<String> tags) {
        this.tags = tags;
        return this;
    }
    
    /**
     * 设置作者
     *
     * @param author 作者
     * @return 查询条件
     */
    public TemplateQueryCriteria author(String author) {
        this.author = author;
        return this;
    }
    
    /**
     * 设置脚本类型
     *
     * @param scriptType 脚本类型
     * @return 查询条件
     */
    public TemplateQueryCriteria scriptType(String scriptType) {
        this.scriptType = scriptType;
        return this;
    }
    
    /**
     * 设置是否活跃
     *
     * @param isActive 是否活跃
     * @return 查询条件
     */
    public TemplateQueryCriteria isActive(Boolean isActive) {
        this.isActive = isActive;
        return this;
    }
    
    /**
     * 设置最小使用次数
     *
     * @param minUsageCount 最小使用次数
     * @return 查询条件
     */
    public TemplateQueryCriteria minUsageCount(Long minUsageCount) {
        this.minUsageCount = minUsageCount;
        return this;
    }
    
    /**
     * 设置最大使用次数
     *
     * @param maxUsageCount 最大使用次数
     * @return 查询条件
     */
    public TemplateQueryCriteria maxUsageCount(Long maxUsageCount) {
        this.maxUsageCount = maxUsageCount;
        return this;
    }
    
    /**
     * 设置创建时间范围（起始）
     *
     * @param createdAfter 创建时间（起始）
     * @return 查询条件
     */
    public TemplateQueryCriteria createdAfter(LocalDateTime createdAfter) {
        this.createdAfter = createdAfter;
        return this;
    }
    
    /**
     * 设置创建时间范围（结束）
     *
     * @param createdBefore 创建时间（结束）
     * @return 查询条件
     */
    public TemplateQueryCriteria createdBefore(LocalDateTime createdBefore) {
        this.createdBefore = createdBefore;
        return this;
    }
    
    /**
     * 设置修改时间范围（起始）
     *
     * @param modifiedAfter 修改时间（起始）
     * @return 查询条件
     */
    public TemplateQueryCriteria modifiedAfter(LocalDateTime modifiedAfter) {
        this.modifiedAfter = modifiedAfter;
        return this;
    }
    
    /**
     * 设置修改时间范围（结束）
     *
     * @param modifiedBefore 修改时间（结束）
     * @return 查询条件
     */
    public TemplateQueryCriteria modifiedBefore(LocalDateTime modifiedBefore) {
        this.modifiedBefore = modifiedBefore;
        return this;
    }
    
    /**
     * 添加模板ID
     *
     * @param templateId 模板ID
     * @return 查询条件
     */
    public TemplateQueryCriteria templateId(String templateId) {
        this.templateIds.add(templateId);
        return this;
    }
    
    /**
     * 设置模板ID列表
     *
     * @param templateIds 模板ID列表
     * @return 查询条件
     */
    public TemplateQueryCriteria templateIds(List<String> templateIds) {
        this.templateIds = templateIds;
        return this;
    }
    
    /**
     * 设置分页偏移
     *
     * @param offset 偏移量
     * @return 查询条件
     */
    public TemplateQueryCriteria offset(int offset) {
        this.offset = offset;
        return this;
    }
    
    /**
     * 设置分页限制
     *
     * @param limit 限制数量
     * @return 查询条件
     */
    public TemplateQueryCriteria limit(int limit) {
        this.limit = limit;
        return this;
    }
    
    /**
     * 设置排序字段
     *
     * @param sortBy 排序字段 (name, createdTime, lastModifiedTime, usageCount, category, scriptType)
     * @return 查询条件
     */
    public TemplateQueryCriteria sortBy(String sortBy) {
        this.sortBy = sortBy;
        return this;
    }
    
    /**
     * 设置排序顺序
     *
     * @param sortOrder 排序顺序
     * @return 查询条件
     */
    public TemplateQueryCriteria sortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }
    
    // Getters
    public String getNamePattern() { return namePattern; }
    public String getDescriptionPattern() { return descriptionPattern; }
    public String getCategory() { return category; }
    public List<String> getTags() { return tags; }
    public String getAuthor() { return author; }
    public String getScriptType() { return scriptType; }
    public Boolean getIsActive() { return isActive; }
    public Long getMinUsageCount() { return minUsageCount; }
    public Long getMaxUsageCount() { return maxUsageCount; }
    public LocalDateTime getCreatedAfter() { return createdAfter; }
    public LocalDateTime getCreatedBefore() { return createdBefore; }
    public LocalDateTime getModifiedAfter() { return modifiedAfter; }
    public LocalDateTime getModifiedBefore() { return modifiedBefore; }
    public List<String> getTemplateIds() { return templateIds; }
    public int getOffset() { return offset; }
    public int getLimit() { return limit; }
    public String getSortBy() { return sortBy; }
    public SortOrder getSortOrder() { return sortOrder; }
    
    /**
     * 检查是否设置了名称模式
     *
     * @return 是否设置了名称模式
     */
    public boolean hasNamePattern() {
        return namePattern != null && !namePattern.trim().isEmpty();
    }
    
    /**
     * 检查是否设置了描述模式
     *
     * @return 是否设置了描述模式
     */
    public boolean hasDescriptionPattern() {
        return descriptionPattern != null && !descriptionPattern.trim().isEmpty();
    }
    
    /**
     * 检查是否设置了分类
     *
     * @return 是否设置了分类
     */
    public boolean hasCategory() {
        return category != null && !category.trim().isEmpty();
    }
    
    /**
     * 检查是否设置了标签
     *
     * @return 是否设置了标签
     */
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
    
    /**
     * 检查是否设置了作者
     *
     * @return 是否设置了作者
     */
    public boolean hasAuthor() {
        return author != null && !author.trim().isEmpty();
    }
    
    /**
     * 检查是否设置了脚本类型
     *
     * @return 是否设置了脚本类型
     */
    public boolean hasScriptType() {
        return scriptType != null && !scriptType.trim().isEmpty();
    }
    
    /**
     * 检查是否设置了使用次数范围
     *
     * @return 是否设置了使用次数范围
     */
    public boolean hasUsageCountRange() {
        return minUsageCount != null || maxUsageCount != null;
    }
    
    /**
     * 检查是否设置了创建时间范围
     *
     * @return 是否设置了创建时间范围
     */
    public boolean hasCreatedTimeRange() {
        return createdAfter != null || createdBefore != null;
    }
    
    /**
     * 检查是否设置了修改时间范围
     *
     * @return 是否设置了修改时间范围
     */
    public boolean hasModifiedTimeRange() {
        return modifiedAfter != null || modifiedBefore != null;
    }
    
    /**
     * 检查是否设置了模板ID列表
     *
     * @return 是否设置了模板ID列表
     */
    public boolean hasTemplateIds() {
        return templateIds != null && !templateIds.isEmpty();
    }
    
    /**
     * 检查是否有任何查询条件
     *
     * @return 是否有任何查询条件
     */
    public boolean hasAnyCriteria() {
        return hasNamePattern() || hasDescriptionPattern() || hasCategory() || hasTags() || 
               hasAuthor() || hasScriptType() || isActive != null || hasUsageCountRange() ||
               hasCreatedTimeRange() || hasModifiedTimeRange() || hasTemplateIds();
    }
    
    /**
     * 创建查询条件构建器
     *
     * @return 查询条件构建器
     */
    public static TemplateQueryCriteria builder() {
        return new TemplateQueryCriteria();
    }
    
    /**
     * 创建简单的名称查询条件
     *
     * @param namePattern 名称模式
     * @return 查询条件
     */
    public static TemplateQueryCriteria name(String namePattern) {
        return builder().namePattern(namePattern);
    }
    
    /**
     * 创建简单的分类查询条件
     *
     * @param category 分类
     * @return 查询条件
     */
    public static TemplateQueryCriteria category(String category) {
        return builder().category(category);
    }
    
    /**
     * 创建简单的脚本类型查询条件
     *
     * @param scriptType 脚本类型
     * @return 查询条件
     */
    public static TemplateQueryCriteria type(String scriptType) {
        return builder().scriptType(scriptType);
    }
    
    /**
     * 创建活跃模板查询条件
     *
     * @return 查询条件
     */
    public static TemplateQueryCriteria activeOnly() {
        return builder().isActive(true);
    }
    
    /**
     * 创建最近修改的模板查询条件
     *
     * @param days 天数
     * @return 查询条件
     */
    public static TemplateQueryCriteria recentlyModified(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return builder().modifiedAfter(cutoff).sortBy("lastModifiedTime").sortOrder(SortOrder.DESC);
    }
    
    /**
     * 创建热门模板查询条件
     *
     * @param minUsageCount 最小使用次数
     * @return 查询条件
     */
    public static TemplateQueryCriteria popular(long minUsageCount) {
        return builder().minUsageCount(minUsageCount).sortBy("usageCount").sortOrder(SortOrder.DESC);
    }
    
    @Override
    public String toString() {
        return "TemplateQueryCriteria{" +
                "namePattern='" + namePattern + '\'' +
                ", descriptionPattern='" + descriptionPattern + '\'' +
                ", category='" + category + '\'' +
                ", tags=" + tags +
                ", author='" + author + '\'' +
                ", scriptType='" + scriptType + '\'' +
                ", isActive=" + isActive +
                ", minUsageCount=" + minUsageCount +
                ", maxUsageCount=" + maxUsageCount +
                ", createdAfter=" + createdAfter +
                ", createdBefore=" + createdBefore +
                ", modifiedAfter=" + modifiedAfter +
                ", modifiedBefore=" + modifiedBefore +
                ", templateIds=" + templateIds +
                ", offset=" + offset +
                ", limit=" + limit +
                ", sortBy='" + sortBy + '\'' +
                ", sortOrder=" + sortOrder +
                '}';
    }
}