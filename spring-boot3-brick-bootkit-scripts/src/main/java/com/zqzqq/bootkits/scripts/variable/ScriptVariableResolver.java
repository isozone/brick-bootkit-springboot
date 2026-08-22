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


package com.zqzqq.bootkits.scripts.variable;

import java.util.List;
import java.util.Map;

/**
 * 脚本变量解析器接口
 * 负责解析脚本中的变量引用，包括环境变量、系统属性、配置变量等
 *
 * @author starBlues
 * @since 4.0.1
 */
public interface ScriptVariableResolver {
    
    /**
     * 变量替换结果
     */
    class VariableReplacementResult {
        private boolean success;
        private String replacedContent;
        private List<String> warnings;
        private List<String> errors;
        private Map<String, Object> resolvedVariables;
        private long resolutionTimeMs;
        
        public VariableReplacementResult() {}
        
        public VariableReplacementResult(boolean success, String replacedContent, List<String> warnings, 
                                       List<String> errors, Map<String, Object> resolvedVariables, long resolutionTimeMs) {
            this.success = success;
            this.replacedContent = replacedContent;
            this.warnings = warnings;
            this.errors = errors;
            this.resolvedVariables = resolvedVariables;
            this.resolutionTimeMs = resolutionTimeMs;
        }
        
        public static VariableReplacementResult success(String replacedContent, Map<String, Object> resolvedVariables, 
                                                       List<String> warnings, long resolutionTimeMs) {
            return new VariableReplacementResult(true, replacedContent, warnings, null, resolvedVariables, resolutionTimeMs);
        }
        
        public static VariableReplacementResult failure(String error) {
            return new VariableReplacementResult(false, null, null, List.of(error), null, 0);
        }
        
        public static VariableReplacementResult failure(List<String> errors) {
            return new VariableReplacementResult(false, null, null, errors, null, 0);
        }
        
        public boolean isSuccess() { return success; }
        public String getReplacedContent() { return replacedContent; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getErrors() { return errors; }
        public Map<String, Object> getResolvedVariables() { return resolvedVariables; }
        public long getResolutionTimeMs() { return resolutionTimeMs; }
    }
    
    /**
     * 变量解析策略
     */
    enum VariableStrategy {
        /**
         * 从环境变量解析
         */
        ENVIRONMENT,
        /**
         * 从系统属性解析
         */
        SYSTEM_PROPERTY,
        /**
         * 从配置文件解析
         */
        CONFIGURATION,
        /**
         * 从变量上下文解析
         */
        CONTEXT,
        /**
         * 条件表达式
         */
        CONDITIONAL,
        /**
         * 函数调用
         */
        FUNCTION,
        /**
         * 默认值
         */
        DEFAULT_VALUE
    }
    
    /**
     * 解析脚本中的变量
     *
     * @param scriptContent 脚本内容
     * @param context 变量上下文
     * @return 变量替换结果
     */
    VariableReplacementResult resolveVariables(String scriptContent, VariableContext context);
    
    /**
     * 添加变量解析器
     *
     * @param resolver 自定义变量解析器
     */
    void addVariableResolver(CustomVariableResolver resolver);
    
    /**
     * 移除变量解析器
     *
     * @param resolver 自定义变量解析器
     */
    void removeVariableResolver(CustomVariableResolver resolver);
    
    /**
     * 设置变量替换配置
     *
     * @param config 配置
     */
    void setConfiguration(VariableResolverConfiguration config);
    
    /**
     * 获取变量替换配置
     *
     * @return 配置
     */
    VariableResolverConfiguration getConfiguration();
    
    /**
     * 获取支持的变量类型
     *
     * @return 支持的变量类型列表
     */
    List<VariableStrategy> getSupportedStrategies();
    
    /**
     * 自定义变量解析器接口
     */
    interface CustomVariableResolver {
        /**
         * 解析变量
         *
         * @param variableName 变量名
         * @param context 变量上下文
         * @return 解析结果，如果不能解析则返回null
         */
        Object resolve(String variableName, VariableContext context);
        
        /**
         * 获取解析器的优先级
         *
         * @return 优先级，数值越大优先级越高
         */
        int getPriority();
        
        /**
         * 获取解析器的名称
         *
         * @return 名称
         */
        String getName();
    }
    
    /**
     * 变量解析器配置
     */
    class VariableResolverConfiguration {
        private boolean strictMode;
        private boolean allowUndefinedVariables;
        private String defaultValue;
        private boolean resolveSystemProperties;
        private boolean resolveEnvironmentVariables;
        private boolean resolveConfigurationVariables;
        private boolean resolveContextVariables;
        private boolean enableConditionalVariables;
        private boolean enableFunctionVariables;
        private boolean enableDefaultValues;
        private long maxResolutionTimeMs;
        private int maxVariableLength;
        private Map<String, String> customVariablePrefixes;
        private Map<String, String> customVariableSuffixes;
        
        public VariableResolverConfiguration() {
            this.strictMode = false;
            this.allowUndefinedVariables = true;
            this.defaultValue = "";
            this.resolveSystemProperties = true;
            this.resolveEnvironmentVariables = true;
            this.resolveConfigurationVariables = true;
            this.resolveContextVariables = true;
            this.enableConditionalVariables = true;
            this.enableFunctionVariables = true;
            this.enableDefaultValues = true;
            this.maxResolutionTimeMs = 5000; // 5秒
            this.maxVariableLength = 1024; // 1KB
            this.customVariablePrefixes = new java.util.HashMap<>();
            this.customVariableSuffixes = new java.util.HashMap<>();
            
            // 默认变量格式
            customVariablePrefixes.put("env", "${env:");
            customVariablePrefixes.put("sys", "${sys:");
            customVariablePrefixes.put("cfg", "${cfg:");
            customVariablePrefixes.put("ctx", "${ctx:");
            customVariablePrefixes.put("func", "${func:");
            customVariablePrefixes.put("cond", "${cond:");
        }
        
        // Getters and Setters
        public boolean isStrictMode() { return strictMode; }
        public void setStrictMode(boolean strictMode) { this.strictMode = strictMode; }
        
        public boolean isAllowUndefinedVariables() { return allowUndefinedVariables; }
        public void setAllowUndefinedVariables(boolean allowUndefinedVariables) { this.allowUndefinedVariables = allowUndefinedVariables; }
        
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        
        public boolean isResolveSystemProperties() { return resolveSystemProperties; }
        public void setResolveSystemProperties(boolean resolveSystemProperties) { this.resolveSystemProperties = resolveSystemProperties; }
        
        public boolean isResolveEnvironmentVariables() { return resolveEnvironmentVariables; }
        public void setResolveEnvironmentVariables(boolean resolveEnvironmentVariables) { this.resolveEnvironmentVariables = resolveEnvironmentVariables; }
        
        public boolean isResolveConfigurationVariables() { return resolveConfigurationVariables; }
        public void setResolveConfigurationVariables(boolean resolveConfigurationVariables) { this.resolveConfigurationVariables = resolveConfigurationVariables; }
        
        public boolean isResolveContextVariables() { return resolveContextVariables; }
        public void setResolveContextVariables(boolean resolveContextVariables) { this.resolveContextVariables = resolveContextVariables; }
        
        public boolean isEnableConditionalVariables() { return enableConditionalVariables; }
        public void setEnableConditionalVariables(boolean enableConditionalVariables) { this.enableConditionalVariables = enableConditionalVariables; }
        
        public boolean isEnableFunctionVariables() { return enableFunctionVariables; }
        public void setEnableFunctionVariables(boolean enableFunctionVariables) { this.enableFunctionVariables = enableFunctionVariables; }
        
        public boolean isEnableDefaultValues() { return enableDefaultValues; }
        public void setEnableDefaultValues(boolean enableDefaultValues) { this.enableDefaultValues = enableDefaultValues; }
        
        public long getMaxResolutionTimeMs() { return maxResolutionTimeMs; }
        public void setMaxResolutionTimeMs(long maxResolutionTimeMs) { this.maxResolutionTimeMs = maxResolutionTimeMs; }
        
        public int getMaxVariableLength() { return maxVariableLength; }
        public void setMaxVariableLength(int maxVariableLength) { this.maxVariableLength = maxVariableLength; }
        
        public Map<String, String> getCustomVariablePrefixes() { return customVariablePrefixes; }
        public void setCustomVariablePrefixes(Map<String, String> customVariablePrefixes) { this.customVariablePrefixes = customVariablePrefixes; }
        
        public Map<String, String> getCustomVariableSuffixes() { return customVariableSuffixes; }
        public void setCustomVariableSuffixes(Map<String, String> customVariableSuffixes) { this.customVariableSuffixes = customVariableSuffixes; }
    }
}