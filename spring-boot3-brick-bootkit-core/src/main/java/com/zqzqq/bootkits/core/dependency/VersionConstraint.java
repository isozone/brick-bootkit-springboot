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


package com.zqzqq.bootkits.core.dependency;

import com.zqzqq.bootkits.core.version.VersionUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 版本约束
 * 定义插件版本的范围约束和兼容性要求
 */
public class VersionConstraint {
    
    private static final Pattern RANGE_PATTERN = Pattern.compile("([><=]{1,2})\\s*(\\d+(?:\\.\\d+)*(?:\\.\\w+)?)");
    private static final Pattern AND_PATTERN = Pattern.compile("\\s*,\\s*");
    
    private final String originalConstraint;
    private final ConstraintOperator operator;
    private final String version;
    private final String upperBound;
    private final boolean isRange;
    
    private VersionConstraint(String constraint, ConstraintOperator operator, String version) {
        this.originalConstraint = constraint;
        this.operator = operator;
        this.version = version;
        this.upperBound = null;
        this.isRange = false;
    }
    
    private VersionConstraint(String constraint, ConstraintOperator operator, String version, String upperBound) {
        this.originalConstraint = constraint;
        this.operator = operator;
        this.version = version;
        this.upperBound = upperBound;
        this.isRange = true;
    }
    
    /**
     * 解析版本约束字符串
     */
    public static VersionConstraint parse(String constraint) {
        if (constraint == null || constraint.trim().isEmpty()) {
            throw new IllegalArgumentException("版本约束不能为空");
        }
        
        String trimmed = constraint.trim();
        
        // 处理范围约束 [1.0,2.0)
        if (trimmed.matches("\\[.*\\]") || trimmed.matches("\\(.*\\)")) {
            return parseRange(trimmed);
        }
        
        // 处理单个约束
        Matcher matcher = RANGE_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            String opStr = matcher.group(1);
            String ver = matcher.group(2);
            
            ConstraintOperator operator = ConstraintOperator.fromString(opStr);
            return new VersionConstraint(trimmed, operator, ver);
        }
        
        throw new IllegalArgumentException("无效的版本约束格式: " + constraint);
    }
    
    /**
     * 解析范围约束
     */
    private static VersionConstraint parseRange(String range) {
        // 移除方括号
        String content = range.substring(1, range.length() - 1);
        
        // 分割下界和上界
        String[] parts = content.split(",", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("范围约束格式错误: " + range);
        }
        
        String lowerBound = parts[0].trim();
        String upperBound = parts[1].trim();
        
        ConstraintOperator lowerOp = ConstraintOperator.GREATER_THAN_EQUAL;
        ConstraintOperator upperOp = ConstraintOperator.LESS_THAN;
        
        String lowerVersion = parseBound(lowerBound, true);
        String upperVersion = parseBound(upperBound, false);
        
        return new VersionConstraint(range, null, lowerVersion, upperVersion);
    }
    
    /**
     * 解析边界值
     */
    private static String parseBound(String bound, boolean isLower) {
        if (bound.isEmpty()) {
            return null;
        }
        
        String trimmed = bound.trim();
        if (trimmed.startsWith(">")) {
            return trimmed.substring(1).trim();
        } else if (trimmed.startsWith(">=")) {
            return trimmed.substring(2).trim();
        } else if (trimmed.startsWith("<")) {
            return trimmed.substring(1).trim();
        } else if (trimmed.startsWith("<=")) {
            return trimmed.substring(2).trim();
        } else if (trimmed.startsWith("=")) {
            return trimmed.substring(1).trim();
        }
        
        // 无操作符，默认边界
        return trimmed;
    }
    
    /**
     * 检查版本是否满足约束
     */
    public boolean isSatisfied(String version) {
        if (version == null || version.trim().isEmpty()) {
            return false;
        }
        
        if (isRange) {
            return isSatisfiedRange(version);
        } else {
            return isSatisfiedSingle(version);
        }
    }
    
    /**
     * 检查单版本约束
     */
    private boolean isSatisfiedSingle(String version) {
        if (operator == null || this.version == null) {
            return false;
        }
        
        int comparison = VersionUtils.compareVersions(version, this.version);
        
        switch (operator) {
            case EQUALS:
                return comparison == 0;
            case NOT_EQUALS:
                return comparison != 0;
            case GREATER_THAN:
                return comparison > 0;
            case GREATER_THAN_EQUAL:
                return comparison >= 0;
            case LESS_THAN:
                return comparison < 0;
            case LESS_THAN_EQUAL:
                return comparison <= 0;
            default:
                return false;
        }
    }
    
    /**
     * 检查范围约束
     */
    private boolean isSatisfiedRange(String version) {
        boolean lowerSatisfied = true;
        boolean upperSatisfied = true;
        
        // 检查下界
        if (version != null) {
            int comparison = VersionUtils.compareVersions(version, this.version);
            lowerSatisfied = comparison >= 0; // >= 下界
        }
        
        // 检查上界
        if (upperBound != null) {
            int comparison = VersionUtils.compareVersions(version, upperBound);
            upperSatisfied = comparison < 0; // < 上界（默认使用小括号）
        }
        
        return lowerSatisfied && upperSatisfied;
    }
    
    /**
     * 获取约束的操作符
     */
    public ConstraintOperator getOperator() {
        return operator;
    }
    
    /**
     * 获取约束的版本
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * 是否为范围约束
     */
    public boolean isRange() {
        return isRange;
    }
    
    /**
     * 获取下界版本
     */
    public String getLowerBound() {
        return version;
    }
    
    /**
     * 获取上界版本
     */
    public String getUpperBound() {
        return upperBound;
    }
    
    /**
     * 获取原始约束字符串
     */
    public String getOriginalConstraint() {
        return originalConstraint;
    }
    
    /**
     * 转换为字符串表示
     */
    @Override
    public String toString() {
        return originalConstraint;
    }
    
    /**
     * 版本约束操作符
     */
    public enum ConstraintOperator {
        EQUALS("=", "等于"),
        NOT_EQUALS("!=", "不等于"),
        GREATER_THAN(">", "大于"),
        GREATER_THAN_EQUAL(">=", "大于等于"),
        LESS_THAN("<", "小于"),
        LESS_THAN_EQUAL("<=", "小于等于");
        
        private final String symbol;
        private final String description;
        
        ConstraintOperator(String symbol, String description) {
            this.symbol = symbol;
            this.description = description;
        }
        
        public String getSymbol() {
            return symbol;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static ConstraintOperator fromString(String symbol) {
            for (ConstraintOperator op : values()) {
                if (op.getSymbol().equals(symbol)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("未知的版本操作符: " + symbol);
        }
        
        @Override
        public String toString() {
            return symbol + " (" + description + ")";
        }
    }
}