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


package com.zqzqq.bootkits.scripts.variable.impl;

import com.zqzqq.bootkits.scripts.variable.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 默认脚本变量解析器实现
 * 提供强大的变量替换功能，支持环境变量、系统属性、条件变量等
 *
 * @author starBlues
 * @since 4.0.1
 */
public class DefaultScriptVariableResolver implements ScriptVariableResolver {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultScriptVariableResolver.class);
    
    // 基本变量模式
    private static final Pattern BASIC_VARIABLE_PATTERN = Pattern.compile("\\$\\{([^{}]+)\\}");
    private static final Pattern TYPED_VARIABLE_PATTERN = Pattern.compile("\\$\\{([a-zA-Z]+):([^}]+)\\}");
    
    // 条件变量模式
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\$\\{cond:([^}]+)\\}");
    
    // 函数变量模式
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("\\$\\{func:([^\\(]+)\\(([^)]*)\\)\\}");
    
    // 内置函数
    private static final Map<String, BuiltInFunction> builtInFunctions = new HashMap<>();
    
    // 自定义解析器
    private final List<CustomVariableResolver> customResolvers = new ArrayList<>();
    
    // 解析统计
    private final AtomicLong totalResolutions = new AtomicLong(0);
    private final AtomicLong failedResolutions = new AtomicLong(0);
    
    // 配置
    private VariableResolverConfiguration configuration;
    
    static {
        // 注册内置函数
        builtInFunctions.put("env", new EnvironmentFunction());
        builtInFunctions.put("sys", new SystemPropertyFunction());
        builtInFunctions.put("cfg", new ConfigurationFunction());
        builtInFunctions.put("ctx", new ContextFunction());
        builtInFunctions.put("date", new DateFunction());
        builtInFunctions.put("random", new RandomFunction());
        builtInFunctions.put("math", new MathFunction());
        builtInFunctions.put("upper", new UpperFunction());
        builtInFunctions.put("lower", new LowerFunction());
        builtInFunctions.put("trim", new TrimFunction());
        builtInFunctions.put("substring", new SubstringFunction());
        builtInFunctions.put("replace", new ReplaceFunction());
        builtInFunctions.put("format", new FormatFunction());
    }
    
    /**
     * 构造函数
     */
    public DefaultScriptVariableResolver() {
        this.configuration = new VariableResolverConfiguration();
    }
    
    /**
     * 构造函数
     *
     * @param configuration 配置
     */
    public DefaultScriptVariableResolver(VariableResolverConfiguration configuration) {
        this.configuration = configuration != null ? configuration : new VariableResolverConfiguration();
    }
    
    @Override
    public VariableReplacementResult resolveVariables(String scriptContent, VariableContext context) {
        if (scriptContent == null) {
            return VariableReplacementResult.failure("脚本内容不能为空");
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            Map<String, Object> resolvedVariables = new HashMap<>();
            List<String> warnings = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            
            String result = scriptContent;
            
            // 检查超时
            checkTimeout(startTime);
            
            // 1. 处理基本变量替换
            if (configuration.isResolveContextVariables()) {
                result = resolveBasicVariables(result, context, resolvedVariables, warnings, errors);
                checkTimeout(startTime);
            }
            
            // 2. 处理带类型的变量
            if (configuration.isResolveEnvironmentVariables() || configuration.isResolveSystemProperties()) {
                result = resolveTypedVariables(result, context, resolvedVariables, warnings, errors);
                checkTimeout(startTime);
            }
            
            // 3. 处理条件变量
            if (configuration.isEnableConditionalVariables()) {
                result = resolveConditionalVariables(result, context, resolvedVariables, warnings, errors);
                checkTimeout(startTime);
            }
            
            // 4. 处理函数变量
            if (configuration.isEnableFunctionVariables()) {
                result = resolveFunctionVariables(result, context, resolvedVariables, warnings, errors);
                checkTimeout(startTime);
            }
            
            // 5. 处理默认值
            if (configuration.isEnableDefaultValues()) {
                result = resolveDefaultValues(result, resolvedVariables, warnings, errors);
                checkTimeout(startTime);
            }
            
            long resolutionTime = System.currentTimeMillis() - startTime;
            totalResolutions.incrementAndGet();
            
            if (!errors.isEmpty()) {
                failedResolutions.incrementAndGet();
                return VariableReplacementResult.failure(errors);
            }
            
            return VariableReplacementResult.success(result, resolvedVariables, warnings, resolutionTime);
            
        } catch (TimeoutException e) {
            return VariableReplacementResult.failure("变量解析超时: " + e.getMessage());
        } catch (Exception e) {
            logger.error("变量解析异常", e);
            failedResolutions.incrementAndGet();
            return VariableReplacementResult.failure("变量解析异常: " + e.getMessage());
        }
    }
    
    @Override
    public void addVariableResolver(CustomVariableResolver resolver) {
        if (resolver != null) {
            customResolvers.add(resolver);
            // 按优先级排序
            customResolvers.sort(Comparator.comparingInt(CustomVariableResolver::getPriority).reversed());
        }
    }
    
    @Override
    public void removeVariableResolver(CustomVariableResolver resolver) {
        if (resolver != null) {
            customResolvers.remove(resolver);
        }
    }
    
    @Override
    public void setConfiguration(VariableResolverConfiguration config) {
        this.configuration = config != null ? config : new VariableResolverConfiguration();
    }
    
    @Override
    public VariableResolverConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public List<VariableStrategy> getSupportedStrategies() {
        return Arrays.asList(
            VariableStrategy.ENVIRONMENT,
            VariableStrategy.SYSTEM_PROPERTY,
            VariableStrategy.CONFIGURATION,
            VariableStrategy.CONTEXT,
            VariableStrategy.CONDITIONAL,
            VariableStrategy.FUNCTION,
            VariableStrategy.DEFAULT_VALUE
        );
    }
    
    // 私有方法
    
    private void checkTimeout(long startTime) throws TimeoutException {
        if (System.currentTimeMillis() - startTime > configuration.getMaxResolutionTimeMs()) {
            throw new TimeoutException("变量解析超过最大时间限制: " + configuration.getMaxResolutionTimeMs() + "ms");
        }
    }
    
    private String resolveBasicVariables(String content, VariableContext context, 
                                       Map<String, Object> resolvedVariables, 
                                       List<String> warnings, List<String> errors) {
        Matcher matcher = BASIC_VARIABLE_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableExpr = matcher.group(1).trim();
            
            // 跳过已处理的高级变量格式
            if (isAdvancedVariableFormat(variableExpr)) {
                continue;
            }
            
            String replacement = resolveBasicVariable(variableExpr, context, resolvedVariables, warnings, errors);
            
            // 处理转义字符
            replacement = replacement.replace("\\", "\\\\")
                                   .replace("$", "\\$")
                                   .replace("{", "\\{")
                                   .replace("}", "\\}");
            
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String resolveTypedVariables(String content, VariableContext context,
                                       Map<String, Object> resolvedVariables,
                                       List<String> warnings, List<String> errors) {
        Matcher matcher = TYPED_VARIABLE_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String type = matcher.group(1).trim().toLowerCase();
            String variableExpr = matcher.group(2).trim();
            
            String replacement = resolveTypedVariable(type, variableExpr, context, resolvedVariables, warnings, errors);
            
            // 处理转义字符
            replacement = replacement.replace("\\", "\\\\")
                                   .replace("$", "\\$")
                                   .replace("{", "\\{")
                                   .replace("}", "\\}");
            
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String resolveConditionalVariables(String content, VariableContext context,
                                             Map<String, Object> resolvedVariables,
                                             List<String> warnings, List<String> errors) {
        Matcher matcher = CONDITIONAL_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String conditionExpr = matcher.group(1).trim();
            
            boolean conditionResult = evaluateCondition(conditionExpr, context, errors);
            String replacement = conditionResult ? "true" : "false";
            
            // 处理转义字符
            replacement = replacement.replace("\\", "\\\\")
                                   .replace("$", "\\$")
                                   .replace("{", "\\{")
                                   .replace("}", "\\}");
            
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String resolveFunctionVariables(String content, VariableContext context,
                                          Map<String, Object> resolvedVariables,
                                          List<String> warnings, List<String> errors) {
        Matcher matcher = FUNCTION_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String functionName = matcher.group(1).trim();
            String arguments = matcher.group(2).trim();
            
            String replacement = invokeFunction(functionName, arguments, context, errors);
            
            // 处理转义字符
            replacement = replacement.replace("\\", "\\\\")
                                   .replace("$", "\\$")
                                   .replace("{", "\\{")
                                   .replace("}", "\\}");
            
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String resolveDefaultValues(String content, Map<String, Object> resolvedVariables,
                                      List<String> warnings, List<String> errors) {
        // 处理默认值语法 ${variable:defaultValue}
        Pattern defaultPattern = Pattern.compile("\\$\\{([^:}]+):([^}]*)\\}");
        Matcher matcher = defaultPattern.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            String defaultValue = matcher.group(2).trim();
            
            if (!resolvedVariables.containsKey(variableName)) {
                // 变量未解析，使用默认值
                resolvedVariables.put(variableName, defaultValue);
                warnings.add("变量 '" + variableName + "' 未定义，使用默认值: " + defaultValue);
            }
            
            String replacement = resolvedVariables.containsKey(variableName) ? 
                resolvedVariables.get(variableName).toString() : defaultValue;
            
            // 处理转义字符
            replacement = replacement.replace("\\", "\\\\")
                                   .replace("$", "\\$")
                                   .replace("{", "\\{")
                                   .replace("}", "\\}");
            
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String resolveBasicVariable(String variableExpr, VariableContext context,
                                      Map<String, Object> resolvedVariables,
                                      List<String> warnings, List<String> errors) {
        // 尝试从上下文中获取
        Object value = context.getVariable(variableExpr);
        if (value != null) {
            resolvedVariables.put(variableExpr, value);
            return value.toString();
        }
        
        // 尝试从自定义解析器获取
        for (CustomVariableResolver resolver : customResolvers) {
            Object resolverValue = resolver.resolve(variableExpr, context);
            if (resolverValue != null) {
                resolvedVariables.put(variableExpr, resolverValue);
                return resolverValue.toString();
            }
        }
        
        // 尝试从系统属性获取
        if (configuration.isResolveSystemProperties()) {
            String sysValue = System.getProperty(variableExpr);
            if (sysValue != null) {
                resolvedVariables.put(variableExpr, sysValue);
                return sysValue;
            }
        }
        
        // 尝试从环境变量获取
        if (configuration.isResolveEnvironmentVariables()) {
            String envValue = System.getenv(variableExpr);
            if (envValue != null) {
                resolvedVariables.put(variableExpr, envValue);
                return envValue;
            }
        }
        
        // 未找到变量
        if (configuration.isAllowUndefinedVariables()) {
            String defaultValue = configuration.getDefaultValue();
            resolvedVariables.put(variableExpr, defaultValue);
            return defaultValue;
        } else {
            errors.add("未定义的变量: " + variableExpr);
            return "${" + variableExpr + "}";
        }
    }
    
    private String resolveTypedVariable(String type, String variableExpr, VariableContext context,
                                      Map<String, Object> resolvedVariables,
                                      List<String> warnings, List<String> errors) {
        String fullVariableName = type + ":" + variableExpr;
        
        switch (type) {
            case "env":
                if (configuration.isResolveEnvironmentVariables()) {
                    String envValue = System.getenv(variableExpr);
                    if (envValue != null) {
                        resolvedVariables.put(fullVariableName, envValue);
                        return envValue;
                    }
                }
                break;
                
            case "sys":
                if (configuration.isResolveSystemProperties()) {
                    String sysValue = System.getProperty(variableExpr);
                    if (sysValue != null) {
                        resolvedVariables.put(fullVariableName, sysValue);
                        return sysValue;
                    }
                }
                break;
                
            case "cfg":
                if (configuration.isResolveConfigurationVariables()) {
                    // 从配置文件获取（简化实现）
                    String cfgValue = getConfigurationVariable(variableExpr);
                    if (cfgValue != null) {
                        resolvedVariables.put(fullVariableName, cfgValue);
                        return cfgValue;
                    }
                }
                break;
                
            case "ctx":
                if (configuration.isResolveContextVariables()) {
                    Object ctxValue = context.getVariable(variableExpr);
                    if (ctxValue != null) {
                        resolvedVariables.put(fullVariableName, ctxValue);
                        return ctxValue.toString();
                    }
                }
                break;
                
            default:
                // 尝试调用函数
                if (configuration.isEnableFunctionVariables()) {
                    String functionResult = invokeFunction(type, variableExpr, context, errors);
                    if (!errors.isEmpty()) {
                        return "${" + fullVariableName + "}";
                    }
                    resolvedVariables.put(fullVariableName, functionResult);
                    return functionResult;
                }
                break;
        }
        
        // 未找到变量
        if (configuration.isAllowUndefinedVariables()) {
            String defaultValue = configuration.getDefaultValue();
            resolvedVariables.put(fullVariableName, defaultValue);
            return defaultValue;
        } else {
            errors.add("未定义的变量: " + fullVariableName);
            return "${" + fullVariableName + "}";
        }
    }
    
    private boolean evaluateCondition(String conditionExpr, VariableContext context, List<String> errors) {
        try {
            conditionExpr = conditionExpr.trim();
            
            // 处理逻辑操作符
            if (conditionExpr.contains("&&")) {
                String[] parts = conditionExpr.split("&&");
                for (String part : parts) {
                    if (!evaluateCondition(part.trim(), context, errors)) {
                        return false;
                    }
                }
                return true;
            }
            
            if (conditionExpr.contains("||")) {
                String[] parts = conditionExpr.split("\\|\\|");
                for (String part : parts) {
                    if (evaluateCondition(part.trim(), context, errors)) {
                        return true;
                    }
                }
                return false;
            }
            
            // 处理取反操作符
            if (conditionExpr.startsWith("!")) {
                return !evaluateCondition(conditionExpr.substring(1).trim(), context, errors);
            }
            
            // 处理比较操作符
            String[] operators = {"==", "!=", ">=", "<=", ">", "<", "contains", "startsWith", "endsWith"};
            for (String op : operators) {
                if (conditionExpr.contains(op)) {
                    String[] parts = conditionExpr.split(Pattern.quote(op), 2);
                    if (parts.length == 2) {
                        Object left = parseValue(parts[0].trim(), context);
                        Object right = parseValue(parts[1].trim(), context);
                        return compareValues(left, right, op);
                    }
                }
            }
            
            // 简单的布尔值检查
            Object value = parseValue(conditionExpr, context);
            return value != null && !value.toString().isEmpty() && !"false".equalsIgnoreCase(value.toString());
            
        } catch (Exception e) {
            errors.add("条件表达式解析错误: " + conditionExpr + " - " + e.getMessage());
            return false;
        }
    }
    
    private String invokeFunction(String functionName, String arguments, VariableContext context, List<String> errors) {
        try {
            BuiltInFunction function = builtInFunctions.get(functionName);
            if (function == null) {
                errors.add("未知的函数: " + functionName);
                return "${func:" + functionName + "(" + arguments + ")}";
            }
            
            List<Object> args = parseArguments(arguments, context);
            Object result = function.invoke(args);
            return result != null ? result.toString() : "";
            
        } catch (Exception e) {
            errors.add("函数调用异常: " + functionName + "(" + arguments + ") - " + e.getMessage());
            return "${func:" + functionName + "(" + arguments + ")}";
        }
    }
    
    private Object parseValue(String expr, VariableContext context) {
        expr = expr.trim();
        
        // 字符串常量
        if ((expr.startsWith("\"") && expr.endsWith("\"")) || (expr.startsWith("'") && expr.endsWith("'"))) {
            return expr.substring(1, expr.length() - 1);
        }
        
        // 数字
        if (isNumeric(expr)) {
            return Double.parseDouble(expr);
        }
        
        // 布尔值
        if ("true".equalsIgnoreCase(expr) || "false".equalsIgnoreCase(expr)) {
            return Boolean.parseBoolean(expr);
        }
        
        // 变量引用
        return context.getVariable(expr);
    }
    
    private List<Object> parseArguments(String argsStr, VariableContext context) {
        List<Object> arguments = new ArrayList<>();
        
        if (argsStr.trim().isEmpty()) {
            return arguments;
        }
        
        // 简单的参数解析
        String[] args = argsStr.split(",");
        for (String arg : args) {
            arguments.add(parseValue(arg.trim(), context));
        }
        
        return arguments;
    }
    
    private boolean compareValues(Object left, Object right, String operator) {
        if (left == null || right == null) {
            return "==".equals(operator) && left == right;
        }
        
        // 字符串比较
        String leftStr = left.toString();
        String rightStr = right.toString();
        
        switch (operator) {
            case "==": return leftStr.equals(rightStr);
            case "!=": return !leftStr.equals(rightStr);
            case ">": return leftStr.compareTo(rightStr) > 0;
            case "<": return leftStr.compareTo(rightStr) < 0;
            case ">=": return leftStr.compareTo(rightStr) >= 0;
            case "<=": return leftStr.compareTo(rightStr) <= 0;
            case "contains": return leftStr.contains(rightStr);
            case "startsWith": return leftStr.startsWith(rightStr);
            case "endsWith": return leftStr.endsWith(rightStr);
            default: return false;
        }
    }
    
    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        return str.matches("-?\\d+(\\.\\d+)?");
    }
    
    private boolean isAdvancedVariableFormat(String expr) {
        return expr.contains(":") || expr.contains("(") || expr.contains("cond:");
    }
    
    private String getConfigurationVariable(String key) {
        // 简化实现，实际应该从配置文件或配置服务获取
        return null;
    }
    
    /**
     * 获取解析统计信息
     *
     * @return 统计信息
     */
    public ResolutionStatistics getStatistics() {
        return new ResolutionStatistics(
            totalResolutions.get(),
            failedResolutions.get(),
            customResolvers.size(),
            builtInFunctions.size(),
            configuration
        );
    }
    
    /**
     * 解析统计信息
     */
    public static class ResolutionStatistics {
        private final long totalResolutions;
        private final long failedResolutions;
        private final int customResolverCount;
        private final int builtInFunctionCount;
        private final VariableResolverConfiguration configuration;
        
        public ResolutionStatistics(long totalResolutions, long failedResolutions, 
                                  int customResolverCount, int builtInFunctionCount,
                                  VariableResolverConfiguration configuration) {
            this.totalResolutions = totalResolutions;
            this.failedResolutions = failedResolutions;
            this.customResolverCount = customResolverCount;
            this.builtInFunctionCount = builtInFunctionCount;
            this.configuration = configuration;
        }
        
        public long getTotalResolutions() { return totalResolutions; }
        public long getFailedResolutions() { return failedResolutions; }
        public long getSuccessRate() { 
            return totalResolutions > 0 ? (totalResolutions - failedResolutions) * 100 / totalResolutions : 0; 
        }
        public int getCustomResolverCount() { return customResolverCount; }
        public int getBuiltInFunctionCount() { return builtInFunctionCount; }
        public VariableResolverConfiguration getConfiguration() { return configuration; }
    }
    
    /**
     * 内置函数接口
     */
    interface BuiltInFunction {
        Object invoke(List<Object> arguments);
    }
    
    // 内置函数实现
    
    private static class EnvironmentFunction implements BuiltInFunction {
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return "";
            String key = arguments.get(0).toString();
            return System.getenv(key);
        }
    }
    
    private static class SystemPropertyFunction implements BuiltInFunction {
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return "";
            String key = arguments.get(0).toString();
            return System.getProperty(key);
        }
    }
    
    private static class ConfigurationFunction implements BuiltInFunction {
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return "";
            String key = arguments.get(0).toString();
            // 简化实现
            return System.getProperty("config." + key);
        }
    }
    
    private static class ContextFunction implements BuiltInFunction {
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return "";
            String key = arguments.get(0).toString();
            // 这里需要传入context，但函数接口限制，简化处理
            return "${ctx:" + key + "}";
        }
    }
    
    private static class DateFunction implements BuiltInFunction {
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) {
                return java.time.LocalDateTime.now().toString();
            }
            String format = arguments.get(0).toString();
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
                return java.time.LocalDateTime.now().format(formatter);
            } catch (Exception e) {
                return java.time.LocalDateTime.now().toString();
            }
        }
    }
    
    private static class RandomFunction implements BuiltInFunction {
        private final Random random = new Random();
        
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) {
                return random.nextInt(100);
            }
            int max = Integer.parseInt(arguments.get(0).toString());
            return random.nextInt(max);
        }
    }
    
    private static class MathFunction implements BuiltInFunction {
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return 0;
            try {
                String expr = arguments.get(0).toString();
                // 简化实现，实际应该使用表达式引擎
                return evaluateSimpleMath(expr);
            } catch (Exception e) {
                return 0;
            }
        }
        
        private double evaluateSimpleMath(String expr) {
            // 这里应该实现一个数学表达式求值器
            return 0;
        }
    }
    
    private static class UpperFunction implements BuiltInFunction {
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return "";
            return arguments.get(0).toString().toUpperCase();
        }
    }
    
    private static class LowerFunction implements BuiltInFunction {
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return "";
            return arguments.get(0).toString().toLowerCase();
        }
    }
    
    private static class TrimFunction implements BuiltInFunction {
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return "";
            return arguments.get(0).toString().trim();
        }
    }
    
    private static class SubstringFunction implements BuiltInFunction {
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.size() < 2) return "";
            String text = arguments.get(0).toString();
            int start = Integer.parseInt(arguments.get(1).toString());
            int end = arguments.size() > 2 ? Integer.parseInt(arguments.get(2).toString()) : text.length();
            return text.substring(Math.max(0, start), Math.min(text.length(), end));
        }
    }
    
    private static class ReplaceFunction implements BuiltInFunction {
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.size() < 3) return "";
            String text = arguments.get(0).toString();
            String oldStr = arguments.get(1).toString();
            String newStr = arguments.get(2).toString();
            return text.replace(oldStr, newStr);
        }
    }
    
    private static class FormatFunction implements BuiltInFunction {
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return "";
            String template = arguments.get(0).toString();
            if (arguments.size() == 1) return template;
            
            Object[] args = arguments.subList(1, arguments.size()).toArray();
            return String.format(template, args);
        }
    }
    
    /**
     * 超时异常
     */
    private static class TimeoutException extends Exception {
        public TimeoutException(String message) {
            super(message);
        }
    }
}
