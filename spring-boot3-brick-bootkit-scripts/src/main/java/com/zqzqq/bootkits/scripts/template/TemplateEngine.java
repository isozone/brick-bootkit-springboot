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

import java.util.Map;
import java.util.List;

/**
 * 模板引擎接口
 * 负责模板内容的变量替换、条件判断、循环等功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public interface TemplateEngine {
    
    /**
     * 模板渲染结果
     */
    class RenderResult {
        private boolean success;
        private String renderedContent;
        private List<String> warnings;
        private List<String> errors;
        private Map<String, Object> usedVariables;
        
        public RenderResult() {}
        
        public RenderResult(boolean success, String renderedContent, List<String> warnings, List<String> errors, Map<String, Object> usedVariables) {
            this.success = success;
            this.renderedContent = renderedContent;
            this.warnings = warnings;
            this.errors = errors;
            this.usedVariables = usedVariables;
        }
        
        public static RenderResult success(String renderedContent, List<String> warnings, Map<String, Object> usedVariables) {
            return new RenderResult(true, renderedContent, warnings, null, usedVariables);
        }
        
        public static RenderResult failure(String error) {
            return new RenderResult(false, null, null, List.of(error), null);
        }
        
        public static RenderResult failure(List<String> errors) {
            return new RenderResult(false, null, null, errors, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getRenderedContent() { return renderedContent; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getErrors() { return errors; }
        public Map<String, Object> getUsedVariables() { return usedVariables; }
    }
    
    /**
     * 渲染模板内容
     *
     * @param templateContent 模板内容
     * @param variables 变量映射
     * @param template 模板对象（用于获取变量定义）
     * @return 渲染结果
     */
    RenderResult renderTemplate(String templateContent, Map<String, Object> variables, ScriptTemplate template);
    
    /**
     * 验证模板内容
     *
     * @param templateContent 模板内容
     * @param template 模板对象
     * @return 验证结果
     */
    ScriptTemplateManager.TemplateValidationResult validateTemplateContent(String templateContent, ScriptTemplate template);
    
    /**
     * 从模板内容提取变量
     *
     * @param templateContent 模板内容
     * @return 提取的变量列表
     */
    List<String> extractVariables(String templateContent);
    
    /**
     * 获取模板语法说明
     *
     * @return 语法说明
     */
    String getSyntaxDocumentation();
    
    /**
     * 设置模板引擎配置
     *
     * @param config 引擎配置
     */
    void setConfiguration(EngineConfiguration config);
    
    /**
     * 获取模板引擎配置
     *
     * @return 引擎配置
     */
    EngineConfiguration getConfiguration();
    
    /**
     * 模板引擎配置
     */
    class EngineConfiguration {
        private boolean strictVariableValidation;
        private boolean allowUndefinedVariables;
        private boolean escapeHtml;
        private String defaultVariableValue;
        private String customVariablePrefix;
        private String customVariableSuffix;
        private int maxTemplateSize;
        private boolean enableConditionalLogic;
        private boolean enableLoops;
        private boolean enableFunctions;
        
        public EngineConfiguration() {
            this.strictVariableValidation = true;
            this.allowUndefinedVariables = false;
            this.escapeHtml = false;
            this.defaultVariableValue = "";
            this.customVariablePrefix = "${";
            this.customVariableSuffix = "}";
            this.maxTemplateSize = 1024 * 1024; // 1MB
            this.enableConditionalLogic = true;
            this.enableLoops = true;
            this.enableFunctions = true;
        }
        
        // Getters and Setters
        public boolean isStrictVariableValidation() { return strictVariableValidation; }
        public void setStrictVariableValidation(boolean strictVariableValidation) { this.strictVariableValidation = strictVariableValidation; }
        
        public boolean isAllowUndefinedVariables() { return allowUndefinedVariables; }
        public void setAllowUndefinedVariables(boolean allowUndefinedVariables) { this.allowUndefinedVariables = allowUndefinedVariables; }
        
        public boolean isEscapeHtml() { return escapeHtml; }
        public void setEscapeHtml(boolean escapeHtml) { this.escapeHtml = escapeHtml; }
        
        public String getDefaultVariableValue() { return defaultVariableValue; }
        public void setDefaultVariableValue(String defaultVariableValue) { this.defaultVariableValue = defaultVariableValue; }
        
        public String getCustomVariablePrefix() { return customVariablePrefix; }
        public void setCustomVariablePrefix(String customVariablePrefix) { this.customVariablePrefix = customVariablePrefix; }
        
        public String getCustomVariableSuffix() { return customVariableSuffix; }
        public void setCustomVariableSuffix(String customVariableSuffix) { this.customVariableSuffix = customVariableSuffix; }
        
        public int getMaxTemplateSize() { return maxTemplateSize; }
        public void setMaxTemplateSize(int maxTemplateSize) { this.maxTemplateSize = maxTemplateSize; }
        
        public boolean isEnableConditionalLogic() { return enableConditionalLogic; }
        public void setEnableConditionalLogic(boolean enableConditionalLogic) { this.enableConditionalLogic = enableConditionalLogic; }
        
        public boolean isEnableLoops() { return enableLoops; }
        public void setEnableLoops(boolean enableLoops) { this.enableLoops = enableLoops; }
        
        public boolean isEnableFunctions() { return enableFunctions; }
        public void setEnableFunctions(boolean enableFunctions) { this.enableFunctions = enableFunctions; }
    }
    
    /**
     * 内置函数接口
     */
    interface BuiltInFunction {
        String getName();
        Object invoke(List<Object> arguments);
        String getDescription();
        List<Class<?>> getArgumentTypes();
    }
}