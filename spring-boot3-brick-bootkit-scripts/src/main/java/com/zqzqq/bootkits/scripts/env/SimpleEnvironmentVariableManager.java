package com.zqzqq.bootkits.scripts.env;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 简化版环境变量管理器
 * 提供基础的环境变量管理功能，避免循环依赖
 *
 * @author starBlues
 * @since 4.0.1
 */
public class SimpleEnvironmentVariableManager implements EnvironmentVariableManager {
    
    private final Map<Scope, Map<String, EnvironmentVariable>> scopeVariables;
    private final Pattern keyPattern = Pattern.compile("^[A-Z][A-Z0-9_]*$");
    private final AtomicLong variableCounter = new AtomicLong(0);
    
    public SimpleEnvironmentVariableManager() {
        this.scopeVariables = new EnumMap<>(Scope.class);
        
        // 初始化各个作用域的变量映射
        for (Scope scope : Scope.values()) {
            scopeVariables.put(scope, new ConcurrentHashMap<>());
        }
        
        // 初始化默认变量
        initializeDefaultVariables();
    }
    
    /**
     * 初始化默认变量
     */
    private void initializeDefaultVariables() {
        setVariable("JAVA_HOME", System.getProperty("java.home"), VariableType.STRING, Scope.SYSTEM, "Java安装目录", "system");
        setVariable("USER_HOME", System.getProperty("user.home"), VariableType.STRING, Scope.USER, "用户主目录", "system");
        setVariable("WORKING_DIR", System.getProperty("user.dir"), VariableType.STRING, Scope.PROCESS, "当前工作目录", "system");
        setVariable("TIMESTAMP", LocalDateTime.now().toString(), VariableType.STRING, Scope.PROCESS, "当前时间戳", "system");
        setVariable("OS_NAME", System.getProperty("os.name"), VariableType.STRING, Scope.SYSTEM, "操作系统名称", "system");
        setVariable("OS_ARCH", System.getProperty("os.arch"), VariableType.STRING, Scope.SYSTEM, "操作系统架构", "system");
        setVariable("JAVA_VERSION", System.getProperty("java.version"), VariableType.STRING, Scope.SYSTEM, "Java版本", "system");
    }
    
    @Override
    public boolean setVariable(String key, Object value, VariableType type, Scope scope, String description, String source) {
        if (!validateKey(key)) {
            return false;
        }
        
        if (!validateValue(value, type)) {
            return false;
        }
        
        EnvironmentVariable variable = new EnvironmentVariable(
            key, value, type, scope, false, description, source
        );
        
        Map<String, EnvironmentVariable> scopeVars = scopeVariables.get(scope);
        if (scopeVars == null) {
            return false;
        }
        scopeVars.put(key, variable);
        variableCounter.incrementAndGet();
        return true;
    }
    
    @Override
    public boolean setVariable(String key, Object value, VariableType type) {
        return setVariable(key, value, type, Scope.PROJECT, null, "default");
    }
    
    @Override
    public Optional<EnvironmentVariable> getVariable(String key) {
        // 按优先级查找：PROCESS > PROJECT > USER > SYSTEM
        List<Scope> searchOrder = Arrays.asList(Scope.PROCESS, Scope.PROJECT, Scope.USER, Scope.SYSTEM);
        
        for (Scope scope : searchOrder) {
            Map<String, EnvironmentVariable> scopeVars = scopeVariables.get(scope);
            if (scopeVars != null) {
                EnvironmentVariable variable = scopeVars.get(key);
                if (variable != null) {
                    return Optional.of(variable);
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<Object> getValue(String key) {
        return getVariable(key).map(EnvironmentVariable::getValue);
    }
    
    @Override
    public boolean removeVariable(String key) {
        // 从所有作用域中删除
        boolean removed = false;
        for (Map<String, EnvironmentVariable> scopeVars : scopeVariables.values()) {
            if (scopeVars.remove(key) != null) {
                removed = true;
            }
        }
        return removed;
    }
    
    @Override
    public boolean hasVariable(String key) {
        return getVariable(key).isPresent();
    }
    
    @Override
    public List<EnvironmentVariable> getAllVariables() {
        return scopeVariables.values().stream()
            .flatMap(map -> map.values().stream())
            .collect(Collectors.toList());
    }
    
    @Override
    public List<EnvironmentVariable> getVariablesByScope(Scope scope) {
        Map<String, EnvironmentVariable> scopeVars = scopeVariables.get(scope);
        return scopeVars != null ? new ArrayList<>(scopeVars.values()) : Collections.emptyList();
    }
    
    @Override
    public List<EnvironmentVariable> queryVariables(EnvironmentVariableQuery query) {
        List<EnvironmentVariable> allVariables = getAllVariables();
        
        return allVariables.stream().filter(variable -> {
            // 作用域过滤
            if (query.getScope() != null && variable.getScope() != query.getScope()) {
                return false;
            }
            
            // 类型过滤
            if (query.getType() != null && variable.getType() != query.getType()) {
                return false;
            }
            
            // 键模式过滤
            if (query.getKeyPattern() != null && !matchesPattern(variable.getKey(), query.getKeyPattern())) {
                return false;
            }
            
            // 描述模式过滤
            if (query.getDescriptionPattern() != null && 
                (variable.getDescription() == null || !matchesPattern(variable.getDescription(), query.getDescriptionPattern()))) {
                return false;
            }
            
            // 只读过滤
            if (query.getReadOnly() != null && variable.isReadOnly() != query.getReadOnly()) {
                return false;
            }
            
            // 来源模式过滤
            if (query.getSourcePattern() != null && 
                (variable.getSource() == null || !matchesPattern(variable.getSource(), query.getSourcePattern()))) {
                return false;
            }
            
            // 创建时间过滤
            if (query.getCreatedAfter() != null && variable.getCreatedTime().isBefore(query.getCreatedAfter())) {
                return false;
            }
            
            if (query.getCreatedBefore() != null && variable.getCreatedTime().isAfter(query.getCreatedBefore())) {
                return false;
            }
            
            return true;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<EnvironmentVariable> findVariablesByKeyPattern(String keyPattern) {
        return getAllVariables().stream()
                .filter(variable -> matchesPattern(variable.getKey(), keyPattern))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<EnvironmentVariable> findVariablesByType(VariableType type) {
        return getAllVariables().stream()
                .filter(variable -> variable.getType() == type)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<String> getVariableKeys() {
        return getAllVariables().stream()
                .map(EnvironmentVariable::getKey)
                .sorted()
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Object> getVariableValues() {
        return getAllVariables().stream()
                .map(EnvironmentVariable::getValue)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<String> getStringValue(String key) {
        return getValue(key).map(value -> {
            if (value instanceof String) {
                return (String) value;
            }
            return String.valueOf(value);
        });
    }
    
    @Override
    public Optional<Integer> getIntegerValue(String key) {
        return getValue(key).map(value -> {
            if (value instanceof Integer) {
                return (Integer) value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }
    
    @Override
    public Optional<Long> getLongValue(String key) {
        return getValue(key).map(value -> {
            if (value instanceof Long) {
                return (Long) value;
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }
    
    @Override
    public Optional<Double> getDoubleValue(String key) {
        return getValue(key).map(value -> {
            if (value instanceof Double) {
                return (Double) value;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }
    
    @Override
    public Optional<Boolean> getBooleanValue(String key) {
        return getValue(key).map(value -> {
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return Boolean.parseBoolean(value.toString());
        });
    }
    
    @Override
    public Optional<List<?>> getListValue(String key) {
        return getValue(key).map(value -> {
            if (value instanceof List) {
                return (List<?>) value;
            }
            return null;
        });
    }
    
    @Override
    public Optional<Map<?, ?>> getMapValue(String key) {
        return getValue(key).map(value -> {
            if (value instanceof Map) {
                return (Map<?, ?>) value;
            }
            return null;
        });
    }
    
    @Override
    public Map<String, String> exportToMap(Scope scope) {
        Map<String, String> result = new HashMap<>();
        Collection<EnvironmentVariable> variables = scope == null ? getAllVariables() : getVariablesByScope(scope);
        
        for (EnvironmentVariable variable : variables) {
            result.put(variable.getKey(), String.valueOf(variable.getValue()));
        }
        
        return result;
    }
    
    @Override
    public int importFromMap(Map<String, ?> variables, Scope scope, boolean overwrite, String source) {
        if (variables == null || variables.isEmpty()) {
            return 0;
        }
        
        int imported = 0;
        for (Map.Entry<String, ?> entry : variables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (overwrite || !hasVariable(key)) {
                VariableType type = detectType(value);
                if (setVariable(key, value, type, scope, null, source)) {
                    imported++;
                }
            }
        }
        
        return imported;
    }
    
    @Override
    public int importFromSystemEnvironment(String prefix, Scope scope, boolean overwrite) {
        Map<String, String> systemEnv = System.getenv();
        Map<String, String> filtered = new HashMap<>();
        
        if (prefix != null && !prefix.isEmpty()) {
            for (Map.Entry<String, String> entry : systemEnv.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            filtered.putAll(systemEnv);
        }
        
        return importFromMap(filtered, scope, overwrite, "system-environment");
    }
    
    @Override
    public String exportToJson(Scope scope) {
        Map<String, Object> exportData = new HashMap<>();
        Map<String, String> variables = exportToMap(scope);
        
        exportData.put("timestamp", LocalDateTime.now().toString());
        exportData.put("scope", scope != null ? scope.name() : "ALL");
        exportData.put("variables", variables);
        exportData.put("count", variables.size());
        
        return new com.google.gson.Gson().toJson(exportData);
    }
    
    @Override
    public boolean importFromJson(String json, boolean overwrite) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = new com.google.gson.Gson().fromJson(json, Map.class);
            
            if (data == null || !data.containsKey("variables")) {
                return false;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, String> variables = (Map<String, String>) data.get("variables");
            
            String scopeStr = (String) data.get("scope");
            Scope scope = "ALL".equals(scopeStr) ? null : Scope.valueOf(scopeStr);
            
            return importFromMap(variables, scope, overwrite, "json-import") > 0;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public int clearScope(Scope scope) {
        Map<String, EnvironmentVariable> scopeVars = scopeVariables.get(scope);
        if (scopeVars == null) {
            return 0;
        }
        
        int count = scopeVars.size();
        scopeVars.clear();
        return count;
    }
    
    @Override
    public int clearAll() {
        int total = 0;
        for (Scope scope : Scope.values()) {
            total += clearScope(scope);
        }
        return total;
    }
    
    @Override
    public int copyScope(Scope sourceScope, Scope targetScope, boolean overwrite) {
        Map<String, EnvironmentVariable> sourceVars = scopeVariables.get(sourceScope);
        Map<String, EnvironmentVariable> targetVars = scopeVariables.get(targetScope);
        
        if (sourceVars == null || targetVars == null) {
            return 0;
        }
        
        int copied = 0;
        for (EnvironmentVariable variable : sourceVars.values()) {
            String key = variable.getKey();
            if (overwrite || !targetVars.containsKey(key)) {
                EnvironmentVariable copiedVar = new EnvironmentVariable(
                    key, variable.getValue(), variable.getType(), targetScope,
                    variable.isReadOnly(), variable.getDescription(), variable.getSource()
                );
                
                if (targetVars.put(key, copiedVar) == null) {
                    copied++;
                }
            }
        }
        
        return copied;
    }
    
    @Override
    public boolean canModifyInScope(String key, Scope targetScope) {
        EnvironmentVariable variable = getVariable(key).orElse(null);
        if (variable == null) {
            return true; // 新变量可以创建
        }
        
        // 只读变量不能修改
        if (variable.isReadOnly()) {
            return false;
        }
        
        // 系统级变量通常不能修改
        if (variable.getScope() == Scope.SYSTEM) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public EnvironmentVariableStatistics getStatistics() {
        long total = 0;
        long systemCount = 0, userCount = 0, projectCount = 0, processCount = 0;
        long readOnlyCount = 0, writableCount = 0;
        
        Map<VariableType, Long> typeDist = new EnumMap<>(VariableType.class);
        Map<String, Long> sourceDist = new HashMap<>();
        LocalDateTime lastModified = null;
        
        for (Scope scope : Scope.values()) {
            List<EnvironmentVariable> variables = getVariablesByScope(scope);
            total += variables.size();
            
            switch (scope) {
                case SYSTEM: systemCount = variables.size(); break;
                case USER: userCount = variables.size(); break;
                case PROJECT: projectCount = variables.size(); break;
                case PROCESS: processCount = variables.size(); break;
            }
            
            for (EnvironmentVariable variable : variables) {
                // 读写性统计
                if (variable.isReadOnly()) {
                    readOnlyCount++;
                } else {
                    writableCount++;
                }
                
                // 类型分布
                typeDist.merge(variable.getType(), 1L, Long::sum);
                
                // 来源分布
                sourceDist.merge(variable.getSource(), 1L, Long::sum);
                
                // 最后修改时间
                if (lastModified == null || variable.getLastModifiedTime().isAfter(lastModified)) {
                    lastModified = variable.getLastModifiedTime();
                }
            }
        }
        
        return new EnvironmentVariableStatistics(total, systemCount, userCount, projectCount, 
                                               processCount, readOnlyCount, writableCount, typeDist, 
                                               sourceDist, lastModified);
    }
    
    @Override
    public Map<Scope, Long> getScopeDistribution() {
        Map<Scope, Long> distribution = new EnumMap<>(Scope.class);
        for (Scope scope : Scope.values()) {
            distribution.put(scope, (long) getVariablesByScope(scope).size());
        }
        return distribution;
    }
    
    @Override
    public Map<VariableType, Long> getTypeDistribution() {
        return getStatistics().getTypeDistribution();
    }
    
    @Override
    public boolean validateIntegrity() {
        // 检查变量键的唯一性
        Set<String> keys = new HashSet<>();
        for (EnvironmentVariable variable : getAllVariables()) {
            if (!keys.add(variable.getKey())) {
                return false; // 重复的键
            }
        }
        
        // 检查关键系统变量是否存在
        String[] requiredVars = {"JAVA_HOME", "USER_HOME", "OS_NAME"};
        for (String required : requiredVars) {
            if (!hasVariable(required)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public boolean repairIntegrity() {
        boolean repaired = false;
        
        // 修复重复键（保留作用域优先级最高的）
        Map<String, EnvironmentVariable> keyToBestVar = new HashMap<>();
        List<EnvironmentVariable> allVars = getAllVariables();
        
        // 按作用域优先级排序：PROCESS > PROJECT > USER > SYSTEM
        List<Scope> priorityOrder = Arrays.asList(Scope.PROCESS, Scope.PROJECT, Scope.USER, Scope.SYSTEM);
        
        for (EnvironmentVariable variable : allVars) {
            String key = variable.getKey();
            EnvironmentVariable existing = keyToBestVar.get(key);
            
            if (existing == null || priorityOrder.indexOf(variable.getScope()) < priorityOrder.indexOf(existing.getScope())) {
                keyToBestVar.put(key, variable);
            }
        }
        
        // 清理重复的变量
        for (EnvironmentVariable variable : allVars) {
            EnvironmentVariable best = keyToBestVar.get(variable.getKey());
            if (best != variable) {
                scopeVariables.get(variable.getScope()).remove(variable.getKey());
                repaired = true;
            }
        }
        
        return repaired;
    }
    
    @Override
    public boolean validateKey(String key) {
        return key != null && 
               !key.trim().isEmpty() && 
               key.length() <= 256 &&
               keyPattern.matcher(key).matches();
    }
    
    @Override
    public boolean validateValue(Object value, VariableType type) {
        if (value == null) {
            return type == VariableType.STRING; // 只有字符串类型允许null值
        }
        
        try {
            switch (type) {
                case STRING:
                    return true;
                case INTEGER:
                    Integer.parseInt(value.toString());
                    return true;
                case LONG:
                    Long.parseLong(value.toString());
                    return true;
                case DOUBLE:
                    Double.parseDouble(value.toString());
                    return true;
                case BOOLEAN:
                    // 接受常见的布尔值表示
                    String boolStr = value.toString().toLowerCase();
                    return "true".equals(boolStr) || "false".equals(boolStr) || 
                           "1".equals(boolStr) || "0".equals(boolStr);
                case LIST:
                case MAP:
                    return value instanceof List || value instanceof Map;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public Object convertValue(Object value, VariableType targetType) {
        if (value == null) {
            return null;
        }
        
        try {
            switch (targetType) {
                case STRING:
                    return String.valueOf(value);
                case INTEGER:
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                    return Integer.parseInt(value.toString());
                case LONG:
                    if (value instanceof Number) {
                        return ((Number) value).longValue();
                    }
                    return Long.parseLong(value.toString());
                case DOUBLE:
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                    return Double.parseDouble(value.toString());
                case BOOLEAN:
                    if (value instanceof Boolean) {
                        return (Boolean) value;
                    }
                    String boolStr = value.toString().toLowerCase();
                    return "true".equals(boolStr) || "1".equals(boolStr);
                case LIST:
                case MAP:
                    return value; // 不转换复杂类型
                default:
                    return value;
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String formatValue(EnvironmentVariable variable) {
        if (variable == null) {
            return "null";
        }
        
        Object value = variable.getValue();
        if (value == null) {
            return "null";
        }
        
        switch (variable.getType()) {
            case LIST:
            case MAP:
                return new com.google.gson.Gson().toJson(value);
            default:
                return String.valueOf(value);
        }
    }
    
    @Override
    public String getDisplayName(EnvironmentVariable variable) {
        if (variable == null) {
            return "null";
        }
        
        return String.format("%s [%s:%s]", variable.getKey(), variable.getScope(), variable.getType());
    }
    
    @Override
    public List<EnvironmentVariable> searchVariables(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllVariables();
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return getAllVariables().stream()
                .filter(variable -> 
                    variable.getKey().toLowerCase().contains(lowerKeyword) ||
                    (variable.getDescription() != null && variable.getDescription().toLowerCase().contains(lowerKeyword)) ||
                    (variable.getSource() != null && variable.getSource().toLowerCase().contains(lowerKeyword))
                )
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean setVariables(Map<String, Object> variables, Scope scope, String source) {
        if (variables == null || variables.isEmpty()) {
            return false;
        }
        
        boolean allSuccess = true;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            VariableType type = detectType(value);
            
            if (!setVariable(key, value, type, scope, null, source)) {
                allSuccess = false;
            }
        }
        
        return allSuccess;
    }
    
    @Override
    public int removeVariables(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        
        int removed = 0;
        for (String key : keys) {
            if (removeVariable(key)) {
                removed++;
            }
        }
        
        return removed;
    }
    
    @Override
    public List<EnvironmentVariable> getVariableHistory(String key) {
        // 简化实现：返回空列表
        return Collections.emptyList();
    }
    
    @Override
    public boolean resetToSystemDefault(String key) {
        // 移除当前变量
        removeVariable(key);
        
        // 恢复系统默认值
        Map<String, String> systemDefaults = getSystemDefaults();
        if (systemDefaults.containsKey(key)) {
            String value = systemDefaults.get(key);
            return setVariable(key, value, VariableType.STRING, Scope.SYSTEM, "系统默认值", "system-default");
        }
        
        return false;
    }
    
    @Override
    public String backupVariables(Scope scope) {
        String backupId = "backup_" + System.currentTimeMillis();
        // 简化实现：只是返回备份ID
        return backupId;
    }
    
    @Override
    public boolean restoreVariables(String backupId, boolean overwrite) {
        // 简化实现：不执行实际恢复
        return false;
    }
    
    @Override
    public List<String> listBackups() {
        // 简化实现：返回空列表
        return Collections.emptyList();
    }
    
    @Override
    public boolean deleteBackup(String backupId) {
        // 简化实现：不执行实际删除
        return false;
    }
    
    @Override
    public String getManagerInfo() {
        EnvironmentVariableStatistics stats = getStatistics();
        return String.format("SimpleEnvironmentVariableManager{variables=%d, scopes=%d, types=%d}",
                stats.getTotalVariables(), 
                scopeVariables.size(),
                getTypeDistribution().size());
    }
    
    @Override
    public boolean supportsFeature(String feature) {
        // 简化实现：支持基础功能
        Set<String> supportedFeatures = new HashSet<>(Arrays.asList(
            "scope-management",
            "type-conversion",
            "basic-operations"
        ));
        return supportedFeatures.contains(feature);
    }
    
    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
            "基础环境变量管理",
            "作用域支持",
            "类型转换",
            "导入导出",
            "查询和搜索",
            "统计分析"
        );
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 匹配模式（支持通配符*）
     */
    private boolean matchesPattern(String input, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }
        
        String regex = pattern.replace("*", ".*");
        return input != null && input.matches(regex);
    }
    
    /**
     * 检测值的类型
     */
    private VariableType detectType(Object value) {
        if (value == null) {
            return VariableType.STRING;
        }
        
        if (value instanceof Integer) {
            return VariableType.INTEGER;
        } else if (value instanceof Long) {
            return VariableType.LONG;
        } else if (value instanceof Double || value instanceof Float) {
            return VariableType.DOUBLE;
        } else if (value instanceof Boolean) {
            return VariableType.BOOLEAN;
        } else if (value instanceof List) {
            return VariableType.LIST;
        } else if (value instanceof Map) {
            return VariableType.MAP;
        } else {
            return VariableType.STRING;
        }
    }
    
    /**
     * 获取系统默认值
     */
    private Map<String, String> getSystemDefaults() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("JAVA_HOME", System.getProperty("java.home"));
        defaults.put("USER_HOME", System.getProperty("user.home"));
        defaults.put("WORKING_DIR", System.getProperty("user.dir"));
        defaults.put("TIMESTAMP", LocalDateTime.now().toString());
        defaults.put("OS_NAME", System.getProperty("os.name"));
        defaults.put("OS_ARCH", System.getProperty("os.arch"));
        defaults.put("JAVA_VERSION", System.getProperty("java.version"));
        return defaults;
    }
    
    @Override
    public String toString() {
        return getManagerInfo();
    }
}
