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


package com.zqzqq.bootkits.scripts.template.impl;

import com.zqzqq.bootkits.scripts.template.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 模板引擎默认实现
 * 提供基本的模板变量替换、条件判断和循环功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public class DefaultTemplateEngine implements TemplateEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultTemplateEngine.class);
    
    // 变量替换正则表达式
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^{}]+)\\}");
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\$\\{if\\s+([^}]+)\\}(.*?)(?:\\$\\{else\\}(.*?))?\\$\\{endif\\}", Pattern.DOTALL);
    private static final Pattern LOOP_PATTERN = Pattern.compile("\\$\\{foreach\\s+([^\\s]+)\\s+in\\s+([^}]+)\\}(.*?)\\$\\{endfor\\}", Pattern.DOTALL);
    
    // 内置函数
    private static final Map<String, BuiltInFunction> builtInFunctions = new HashMap<>();
    
    static {
        // 注册内置函数
        builtInFunctions.put("upper", new UpperFunction());
        builtInFunctions.put("lower", new LowerFunction());
        builtInFunctions.put("trim", new TrimFunction());
        builtInFunctions.put("length", new LengthFunction());
        builtInFunctions.put("substring", new SubstringFunction());
        builtInFunctions.put("replace", new ReplaceFunction());
        builtInFunctions.put("contains", new ContainsFunction());
        builtInFunctions.put("isEmpty", new IsEmptyFunction());
        builtInFunctions.put("format", new FormatFunction());
        builtInFunctions.put("date", new DateFunction());
        builtInFunctions.put("math", new MathFunction());
    }
    
    private EngineConfiguration configuration;
    
    /**
     * 构造函数
     */
    public DefaultTemplateEngine() {
        this.configuration = new EngineConfiguration();
    }
    
    /**
     * 构造函数
     *
     * @param configuration 引擎配置
     */
    public DefaultTemplateEngine(EngineConfiguration configuration) {
        this.configuration = configuration != null ? configuration : new EngineConfiguration();
    }
    
    @Override
    public RenderResult renderTemplate(String templateContent, Map<String, Object> variables, ScriptTemplate template) {
        if (templateContent == null) {
            return RenderResult.failure("模板内容不能为空");
        }
        
        if (templateContent.length() > configuration.getMaxTemplateSize()) {
            return RenderResult.failure("模板内容超出最大大小限制: " + configuration.getMaxTemplateSize() + " 字符");
        }
        
        try {
            Map<String, Object> usedVariables = new HashMap<>();
            List<String> warnings = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            
            String renderedContent = templateContent;
            
            // 1. 处理循环结构
            if (configuration.isEnableLoops()) {
                renderedContent = processLoops(renderedContent, variables, errors);
            }
            
            // 2. 处理条件结构
            if (configuration.isEnableConditionalLogic()) {
                renderedContent = processConditionals(renderedContent, variables, errors);
            }
            
            // 3. 处理内置函数
            if (configuration.isEnableFunctions()) {
                renderedContent = processFunctions(renderedContent, variables, errors);
            }
            
            // 4. 处理变量替换
            renderedContent = processVariables(renderedContent, variables, template, usedVariables, warnings, errors);
            
            // 5. 处理HTML转义
            if (configuration.isEscapeHtml()) {
                renderedContent = escapeHtml(renderedContent);
            }
            
            if (!errors.isEmpty()) {
                return RenderResult.failure(errors);
            }
            
            return RenderResult.success(renderedContent, warnings, usedVariables);
            
        } catch (Exception e) {
            logger.error("模板渲染异常", e);
            return RenderResult.failure("模板渲染异常: " + e.getMessage());
        }
    }
    
    @Override
    public ScriptTemplateManager.TemplateValidationResult validateTemplateContent(String templateContent, ScriptTemplate template) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (templateContent == null || templateContent.trim().isEmpty()) {
            errors.add("模板内容不能为空");
            return ScriptTemplateManager.TemplateValidationResult.failure("模板验证失败", errors);
        }
        
        // 检查模板大小
        if (templateContent.length() > configuration.getMaxTemplateSize()) {
            errors.add("模板内容超出最大大小限制: " + configuration.getMaxTemplateSize() + " 字符");
        }
        
        // 检查变量定义与使用
        if (template != null) {
            Set<String> definedVariables = template.getVariables().stream()
                .map(ScriptTemplate.TemplateVariable::getName)
                .collect(Collectors.toSet());
            Set<String> usedVariables = extractVariableNames(templateContent);
            
            // 检查未定义的变量
            for (String usedVar : usedVariables) {
                if (!definedVariables.contains(usedVar) && !builtInFunctions.containsKey(usedVar)) {
                    if (configuration.isStrictVariableValidation()) {
                        errors.add("变量 '" + usedVar + "' 在模板中未定义");
                    } else {
                        warnings.add("变量 '" + usedVar + "' 在模板中未定义");
                    }
                }
            }
            
            // 检查未使用的变量
            for (String definedVar : definedVariables) {
                if (!usedVariables.contains(definedVar)) {
                    warnings.add("定义的变量 '" + definedVar + "' 在模板中未使用");
                }
            }
        }
        
        // 检查语法错误
        validateSyntax(templateContent, errors, warnings);
        
        if (!errors.isEmpty()) {
            return ScriptTemplateManager.TemplateValidationResult.failure("模板验证失败", errors);
        }
        
        return ScriptTemplateManager.TemplateValidationResult.success();
    }
    
    @Override
    public List<String> extractVariables(String templateContent) {
        if (templateContent == null) {
            return new ArrayList<>();
        }
        
        Set<String> variables = new HashSet<>();
        
        // 提取简单变量引用
        Matcher variableMatcher = VARIABLE_PATTERN.matcher(templateContent);
        while (variableMatcher.find()) {
            String varName = variableMatcher.group(1).trim();
            // 过滤掉函数调用
            if (!varName.contains("(")) {
                variables.add(varName.split("[\\.\\[\\]]")[0]);
            }
        }
        
        return new ArrayList<>(variables);
    }
    
    @Override
    public String getSyntaxDocumentation() {
        return """
        脚本模板语法说明:
        
        1. 变量替换:
           ${variableName} - 基本变量替换
           ${variableName:defaultValue} - 带默认值的变量替换
           ${variable.property} - 对象属性访问
           ${array[index]} - 数组元素访问
        
        2. 条件判断:
           ${if condition}content${else}alternative${endif}
           支持的条件: ==, !=, >, <, >=, <=, &&, ||, !
        
        3. 循环结构:
           ${foreach item in list}
           ${item}
           ${endfor}
        
        4. 内置函数:
           ${upper(text)} - 转换为大写
           ${lower(text)} - 转换为小写
           ${trim(text)} - 去除首尾空格
           ${length(text)} - 获取长度
           ${substring(text,start,end)} - 字符串截取
           ${replace(text,old,new)} - 字符串替换
           ${contains(text,substr)} - 检查是否包含
           ${isEmpty(value)} - 检查是否为空
           ${format(template,args...)} - 字符串格式化
           ${date(format)} - 格式化当前日期
           ${math(expression)} - 数学计算
        
        5. 示例:
           Hello ${name:World}!
           ${if age >= 18}成年人${else}未成年人${endif}
           ${foreach user in users}Hello ${user.name}!${endfor}
           ${upper(hello)} -> HELLO
        """;
    }
    
    @Override
    public void setConfiguration(EngineConfiguration config) {
        this.configuration = config != null ? config : new EngineConfiguration();
    }
    
    @Override
    public EngineConfiguration getConfiguration() {
        return configuration;
    }
    
    // 私有方法
    
    private String processVariables(String content, Map<String, Object> variables, ScriptTemplate template,
                                   Map<String, Object> usedVariables, List<String> warnings, List<String> errors) {
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableExpr = matcher.group(1).trim();
            String replacement = processVariableExpression(variableExpr, variables, template, usedVariables, warnings, errors);
            
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
    
    private String processVariableExpression(String expr, Map<String, Object> variables, ScriptTemplate template,
                                           Map<String, Object> usedVariables, List<String> warnings, List<String> errors) {
        // 处理默认值
        if (expr.contains(":")) {
            String[] parts = expr.split(":", 2);
            String varName = parts[0].trim();
            String defaultValue = parts[1].trim();
            
            Object value = getVariableValue(varName, variables, template, warnings, errors);
            if (value == null || value.toString().trim().isEmpty()) {
                usedVariables.put(varName, defaultValue);
                return defaultValue;
            } else {
                usedVariables.put(varName, value);
                return value.toString();
            }
        }
        
        // 处理函数调用
        if (expr.contains("(") && expr.endsWith(")")) {
            return processFunctionCall(expr, variables, errors);
        }
        
        // 普通变量
        Object value = getVariableValue(expr, variables, template, warnings, errors);
        if (value != null) {
            usedVariables.put(expr, value);
            return value.toString();
        } else if (configuration.isAllowUndefinedVariables()) {
            return configuration.getDefaultVariableValue();
        } else {
            errors.add("未定义的变量: " + expr);
            return "${" + expr + "}";
        }
    }
    
    private Object getVariableValue(String varName, Map<String, Object> variables, ScriptTemplate template,
                                  List<String> warnings, List<String> errors) {
        // 从提供的变量中查找
        if (variables != null && variables.containsKey(varName)) {
            return variables.get(varName);
        }
        
        // 从模板默认变量中查找
        if (template != null) {
            ScriptTemplate.TemplateVariable varDef = template.getVariable(varName);
            if (varDef != null && varDef.getDefaultValue() != null) {
                return varDef.getDefaultValue();
            }
        }
        
        // 从系统环境变量中查找
        String envValue = System.getenv(varName);
        if (envValue != null) {
            return envValue;
        }
        
        // 从系统属性中查找
        String propValue = System.getProperty(varName);
        if (propValue != null) {
            return propValue;
        }
        
        return null;
    }
    
    private String processFunctionCall(String expr, Map<String, Object> variables, List<String> errors) {
        try {
            int parenIndex = expr.indexOf('(');
            String functionName = expr.substring(0, parenIndex).trim();
            String argsStr = expr.substring(parenIndex + 1, expr.length() - 1).trim();
            
            BuiltInFunction function = builtInFunctions.get(functionName);
            if (function == null) {
                errors.add("未知的函数: " + functionName);
                return "${" + expr + "}";
            }
            
            List<Object> arguments = parseArguments(argsStr, variables);
            
            // 验证参数类型
            if (!validateFunctionArguments(function, arguments, errors)) {
                return "${" + expr + "}";
            }
            
            Object result = function.invoke(arguments);
            return result != null ? result.toString() : "";
            
        } catch (Exception e) {
            errors.add("函数调用异常: " + expr + " - " + e.getMessage());
            return "${" + expr + "}";
        }
    }
    
    private List<Object> parseArguments(String argsStr, Map<String, Object> variables) {
        List<Object> arguments = new ArrayList<>();
        
        if (argsStr.trim().isEmpty()) {
            return arguments;
        }
        
        // 简单的参数解析（支持逗号分隔的字符串）
        String[] args = argsStr.split(",");
        for (String arg : args) {
            String trimmed = arg.trim();
            
            // 如果是字符串常量（用引号包围）
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                arguments.add(trimmed.substring(1, trimmed.length() - 1));
            } else if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
                arguments.add(trimmed.substring(1, trimmed.length() - 1));
            } else if (isNumeric(trimmed)) {
                arguments.add(Double.parseDouble(trimmed));
            } else if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
                arguments.add(Boolean.parseBoolean(trimmed));
            } else {
                // 变量引用
                Object value = variables != null ? variables.get(trimmed) : null;
                arguments.add(value != null ? value : trimmed);
            }
        }
        
        return arguments;
    }
    
    private boolean validateFunctionArguments(BuiltInFunction function, List<Object> arguments, List<String> errors) {
        List<Class<?>> expectedTypes = function.getArgumentTypes();
        
        if (expectedTypes == null || expectedTypes.isEmpty()) {
            return true; // 接受任意参数
        }
        
        if (arguments.size() != expectedTypes.size()) {
            errors.add("函数 '" + function.getName() + "' 期望 " + expectedTypes.size() + " 个参数，但得到了 " + arguments.size() + " 个");
            return false;
        }
        
        for (int i = 0; i < expectedTypes.size(); i++) {
            Class<?> expectedType = expectedTypes.get(i);
            Object arg = arguments.get(i);
            
            if (arg == null) {
                if (expectedType.isPrimitive()) {
                    errors.add("函数 '" + function.getName() + "' 的第 " + (i+1) + " 个参数不能为null");
                    return false;
                }
                continue;
            }
            
            if (!expectedType.isAssignableFrom(arg.getClass())) {
                errors.add("函数 '" + function.getName() + "' 的第 " + (i+1) + " 个参数类型不匹配，期望: " + expectedType.getSimpleName() + "，实际: " + arg.getClass().getSimpleName());
                return false;
            }
        }
        
        return true;
    }
    
    private String processConditionals(String content, Map<String, Object> variables, List<String> errors) {
        Matcher matcher = CONDITIONAL_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String condition = matcher.group(1).trim();
            String trueContent = matcher.group(2);
            String falseContent = matcher.group(3);
            
            boolean conditionResult = evaluateCondition(condition, variables, errors);
            String replacement = conditionResult ? trueContent : (falseContent != null ? falseContent : "");
            
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private boolean evaluateCondition(String condition, Map<String, Object> variables, List<String> errors) {
        try {
            // 简单的条件解析（支持基本的比较和逻辑操作）
            condition = condition.trim();
            
            // 处理逻辑操作符
            if (condition.contains("&&")) {
                String[] parts = condition.split("&&");
                for (String part : parts) {
                    if (!evaluateCondition(part.trim(), variables, errors)) {
                        return false;
                    }
                }
                return true;
            }
            
            if (condition.contains("||")) {
                String[] parts = condition.split("\\|\\|");
                for (String part : parts) {
                    if (evaluateCondition(part.trim(), variables, errors)) {
                        return true;
                    }
                }
                return false;
            }
            
            // 处理取反操作符
            if (condition.startsWith("!")) {
                return !evaluateCondition(condition.substring(1).trim(), variables, errors);
            }
            
            // 处理比较操作符
            String[] operators = {"==", "!=", ">=", "<=", ">", "<"};
            for (String op : operators) {
                if (condition.contains(op)) {
                    String[] parts = condition.split(Pattern.quote(op));
                    if (parts.length == 2) {
                        Object left = parseValue(parts[0].trim(), variables);
                        Object right = parseValue(parts[1].trim(), variables);
                        return compareValues(left, right, op);
                    }
                }
            }
            
            // 简单的布尔值检查
            Object value = parseValue(condition, variables);
            return value != null && !value.toString().isEmpty() && !"false".equalsIgnoreCase(value.toString());
            
        } catch (Exception e) {
            errors.add("条件表达式解析错误: " + condition + " - " + e.getMessage());
            return false;
        }
    }
    
    private Object parseValue(String expr, Map<String, Object> variables) {
        expr = expr.trim();
        
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            return expr.substring(1, expr.length() - 1);
        }
        
        if (expr.startsWith("'") && expr.endsWith("'")) {
            return expr.substring(1, expr.length() - 1);
        }
        
        if (isNumeric(expr)) {
            return Double.parseDouble(expr);
        }
        
        if ("true".equalsIgnoreCase(expr) || "false".equalsIgnoreCase(expr)) {
            return Boolean.parseBoolean(expr);
        }
        
        return variables != null ? variables.get(expr) : null;
    }
    
    private boolean compareValues(Object left, Object right, String operator) {
        if (left == null || right == null) {
            return "==".equals(operator) && left == right;
        }
        
        // 数值比较
        if (left instanceof Number && right instanceof Number) {
            double leftNum = ((Number) left).doubleValue();
            double rightNum = ((Number) right).doubleValue();
            return compareNumbers(leftNum, rightNum, operator);
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
            default: return false;
        }
    }
    
    private boolean compareNumbers(double left, double right, String operator) {
        switch (operator) {
            case "==": return left == right;
            case "!=": return left != right;
            case ">": return left > right;
            case "<": return left < right;
            case ">=": return left >= right;
            case "<=": return left <= right;
            default: return false;
        }
    }
    
    private String processLoops(String content, Map<String, Object> variables, List<String> errors) {
        Matcher matcher = LOOP_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String itemVar = matcher.group(1).trim();
            String listVar = matcher.group(2).trim();
            String loopContent = matcher.group(3);
            
            Object listValue = variables.get(listVar);
            String replacement = "";
            
            if (listValue instanceof Iterable) {
                StringBuilder loopResult = new StringBuilder();
                for (Object item : (Iterable<?>) listValue) {
                    Map<String, Object> loopVariables = new HashMap<>(variables);
                    loopVariables.put(itemVar, item);
                    String processedContent = processVariables(loopContent, loopVariables, null, new HashMap<>(), new ArrayList<>(), errors);
                    loopResult.append(processedContent);
                }
                replacement = loopResult.toString();
            } else if (listValue instanceof Object[]) {
                StringBuilder loopResult = new StringBuilder();
                for (Object item : (Object[]) listValue) {
                    Map<String, Object> loopVariables = new HashMap<>(variables);
                    loopVariables.put(itemVar, item);
                    String processedContent = processVariables(loopContent, loopVariables, null, new HashMap<>(), new ArrayList<>(), errors);
                    loopResult.append(processedContent);
                }
                replacement = loopResult.toString();
            } else {
                errors.add("循环变量 '" + listVar + "' 不是可迭代对象");
                replacement = loopContent;
            }
            
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processFunctions(String content, Map<String, Object> variables, List<String> errors) {
        // 这里可以实现更复杂的函数处理逻辑
        // 暂时保持简单，主要在变量替换时处理函数调用
        return content;
    }
    
    private String escapeHtml(String content) {
        if (content == null) return null;
        
        return content.replace("&", "&amp;")
                     .replace("<", "&lt;")
                     .replace(">", "&gt;")
                     .replace("\"", "&quot;")
                     .replace("'", "&#39;");
    }
    
    private Set<String> extractVariableNames(String templateContent) {
        Set<String> variables = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(templateContent);
        
        while (matcher.find()) {
            String varExpr = matcher.group(1).trim();
            // 过滤掉函数调用
            if (!varExpr.contains("(")) {
                String varName = varExpr.split("[\\.\\[\\]]")[0];
                variables.add(varName);
            }
        }
        
        return variables;
    }
    
    private void validateSyntax(String content, List<String> errors, List<String> warnings) {
        // 检查括号匹配
        int braceCount = 0;
        int parenCount = 0;
        int bracketCount = 0;
        
        for (char c : content.toCharArray()) {
            switch (c) {
                case '{': braceCount++; break;
                case '}': braceCount--; if (braceCount < 0) errors.add("多余的结束花括号 '}'"); break;
                case '(': parenCount++; break;
                case ')': parenCount--; if (parenCount < 0) errors.add("多余的结束圆括号 ')'"); break;
                case '[': bracketCount++; break;
                case ']': bracketCount--; if (bracketCount < 0) errors.add("多余的结束方括号 ']'"); break;
            }
        }
        
        if (braceCount > 0) errors.add("未匹配的开始花括号 '{'");
        if (parenCount > 0) errors.add("未匹配的开始圆括号 '('");
        if (bracketCount > 0) errors.add("未匹配的开始方括号 '['");
        
        // 检查模板结构关键词匹配
        String[] keywords = {"if", "else", "endif", "foreach", "endfor"};
        Map<String, Integer> keywordCount = new HashMap<>();
        
        for (String keyword : keywords) {
            keywordCount.put(keyword, countOccurrences(content, "${" + keyword));
        }
        
        // 验证条件结构
        int ifCount = keywordCount.get("if");
        int endifCount = keywordCount.get("endif");
        if (ifCount != endifCount) {
            errors.add("if/endif 结构不匹配: if(" + ifCount + ") endif(" + endifCount + ")");
        }
        
        // 验证循环结构
        int foreachCount = keywordCount.get("foreach");
        int endforCount = keywordCount.get("endfor");
        if (foreachCount != endforCount) {
            errors.add("foreach/endfor 结构不匹配: foreach(" + foreachCount + ") endfor(" + endforCount + ")");
        }
    }
    
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
    
    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        return str.matches("-?\\d+(\\.\\d+)?");
    }
    
    // 内置函数实现
    
    private static class UpperFunction implements BuiltInFunction {
        @Override
        public String getName() { return "upper"; }
        
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return "";
            return arguments.get(0).toString().toUpperCase();
        }
        
        @Override
        public String getDescription() { return "转换为大写"; }
        
        @Override
        public List<Class<?>> getArgumentTypes() { return Arrays.asList(String.class); }
    }
    
    private static class LowerFunction implements BuiltInFunction {
        @Override
        public String getName() { return "lower"; }
        
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return "";
            return arguments.get(0).toString().toLowerCase();
        }
        
        @Override
        public String getDescription() { return "转换为小写"; }
        
        @Override
        public List<Class<?>> getArgumentTypes() { return Arrays.asList(String.class); }
    }
    
    private static class TrimFunction implements BuiltInFunction {
        @Override
        public String getName() { return "trim"; }
        
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return "";
            return arguments.get(0).toString().trim();
        }
        
        @Override
        public String getDescription() { return "去除首尾空格"; }
        
        @Override
        public List<Class<?>> getArgumentTypes() { return Arrays.asList(String.class); }
    }
    
    private static class LengthFunction implements BuiltInFunction {
        @Override
        public String getName() { return "length"; }
        
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return 0;
            return arguments.get(0).toString().length();
        }
        
        @Override
        public String getDescription() { return "获取长度"; }
        
        @Override
        public List<Class<?>> getArgumentTypes() { return Arrays.asList(String.class); }
    }
    
    private static class SubstringFunction implements BuiltInFunction {
        @Override
        public String getName() { return "substring"; }
        
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.size() < 2) return "";
            String text = arguments.get(0).toString();
            int start = ((Number) arguments.get(1)).intValue();
            int end = arguments.size() > 2 ? ((Number) arguments.get(2)).intValue() : text.length();
            return text.substring(Math.max(0, start), Math.min(text.length(), end));
        }
        
        @Override
        public String getDescription() { return "字符串截取"; }
        
        @Override
        public List<Class<?>> getArgumentTypes() { return Arrays.asList(String.class, Number.class, Number.class); }
    }
    
    private static class ReplaceFunction implements BuiltInFunction {
        @Override
        public String getName() { return "replace"; }
        
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.size() < 3) return "";
            String text = arguments.get(0).toString();
            String oldStr = arguments.get(1).toString();
            String newStr = arguments.get(2).toString();
            return text.replace(oldStr, newStr);
        }
        
        @Override
        public String getDescription() { return "字符串替换"; }
        
        @Override
        public List<Class<?>> getArgumentTypes() { return Arrays.asList(String.class, String.class, String.class); }
    }
    
    private static class ContainsFunction implements BuiltInFunction {
        @Override
        public String getName() { return "contains"; }
        
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.size() < 2) return false;
            String text = arguments.get(0).toString();
            String substr = arguments.get(1).toString();
            return text.contains(substr);
        }
        
        @Override
        public String getDescription() { return "检查是否包含"; }
        
        @Override
        public List<Class<?>> getArgumentTypes() { return Arrays.asList(String.class, String.class); }
    }
    
    private static class IsEmptyFunction implements BuiltInFunction {
        @Override
        public String getName() { return "isEmpty"; }
        
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return true;
            Object value = arguments.get(0);
            return value == null || value.toString().trim().isEmpty();
        }
        
        @Override
        public String getDescription() { return "检查是否为空"; }
        
        @Override
        public List<Class<?>> getArgumentTypes() { return Arrays.asList(Object.class); }
    }
    
    private static class FormatFunction implements BuiltInFunction {
        @Override
        public String getName() { return "format"; }
        
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return "";
            String template = arguments.get(0).toString();
            if (arguments.size() == 1) return template;
            
            Object[] args = arguments.subList(1, arguments.size()).toArray();
            return String.format(template, args);
        }
        
        @Override
        public String getDescription() { return "字符串格式化"; }
        
        @Override
        public List<Class<?>> getArgumentTypes() { return Arrays.asList(String.class); }
    }
    
    private static class DateFunction implements BuiltInFunction {
        @Override
        public String getName() { return "date"; }
        
        @Override
        public Object invoke(List<Object> arguments) {
            java.time.format.DateTimeFormatter formatter;
            if (arguments.isEmpty()) {
                formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            } else {
                formatter = java.time.format.DateTimeFormatter.ofPattern(arguments.get(0).toString());
            }
            return java.time.LocalDateTime.now().format(formatter);
        }
        
        @Override
        public String getDescription() { return "格式化当前日期"; }
        
        @Override
        public List<Class<?>> getArgumentTypes() { return Arrays.asList(String.class); }
    }
    
    private static class MathFunction implements BuiltInFunction {
        @Override
        public String getName() { return "math"; }
        
        @Override
        public Object invoke(List<Object> arguments) {
            if (arguments.isEmpty()) return 0;
            try {
                String expr = arguments.get(0).toString();
                // 简单的数学表达式求值（只支持基本运算）
                return evaluateMathExpression(expr);
            } catch (Exception e) {
                return 0;
            }
        }
        
        @Override
        public String getDescription() { return "数学计算"; }
        
        @Override
        public List<Class<?>> getArgumentTypes() { return Arrays.asList(String.class); }
        
        private double evaluateMathExpression(String expr) {
            // 这里可以实现一个简单的数学表达式求值器
            // 暂时返回0，实际应用中可以使用专门的表达式引擎
            return 0;
        }
    }
}