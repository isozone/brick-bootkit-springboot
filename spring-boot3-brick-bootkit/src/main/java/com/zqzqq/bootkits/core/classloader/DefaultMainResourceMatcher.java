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



package com.zqzqq.bootkits.core.classloader;

import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.utils.UrlUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 默认的主程序资源匹配器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.3
 */
public class DefaultMainResourceMatcher implements MainResourceMatcher{

    /**
     * 框架默认需要从主程序加载的资源模式
     */
    private static final Set<String> DEFAULT_FRAMEWORK_PATTERNS = new HashSet<>();
    
    static {
        // 插件引导类相关
        DEFAULT_FRAMEWORK_PATTERNS.add("com/zqzqq/bootkits/bootstrap/**");
        // 插件交互接口和启动器相关
        DEFAULT_FRAMEWORK_PATTERNS.add("com/zqzqq/bootkits/core/launcher/plugin/**");
        DEFAULT_FRAMEWORK_PATTERNS.add("com/zqzqq/bootkits/core/launcher/plugin/involved/**");
        // Spring 核心类（隔离模式下需要从主程序加载）
        // 注意：不能包含 web.servlet.mvc 相关的类，否则会导致主应用的 Controller 映射被插件覆盖
        DEFAULT_FRAMEWORK_PATTERNS.add("org/springframework/boot/**");
        DEFAULT_FRAMEWORK_PATTERNS.add("org/springframework/core/**");
        DEFAULT_FRAMEWORK_PATTERNS.add("org/springframework/context/**");
        DEFAULT_FRAMEWORK_PATTERNS.add("org/springframework/beans/**");
        DEFAULT_FRAMEWORK_PATTERNS.add("org/springframework/aop/**");
        DEFAULT_FRAMEWORK_PATTERNS.add("org/springframework/util/**");
        DEFAULT_FRAMEWORK_PATTERNS.add("org/springframework/stereotype/**");
        DEFAULT_FRAMEWORK_PATTERNS.add("org/springframework/annotation/**");
        DEFAULT_FRAMEWORK_PATTERNS.add("org/springframework/lang/**");
        DEFAULT_FRAMEWORK_PATTERNS.add("org/springframework/expression/**");
        DEFAULT_FRAMEWORK_PATTERNS.add("org/springframework/jcl/**");
    }

    private final Set<String> includePatterns;
    private final Set<String> excludePatterns;

    private final PathMatcher pathMatcher;
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultMainResourceMatcher.class);

    public DefaultMainResourceMatcher(MainResourcePatternDefiner mainResourcePatternDefiner) {
        this.includePatterns = mainResourcePatternDefiner.getIncludePatterns();
        this.excludePatterns = mainResourcePatternDefiner.getExcludePatterns();
        this.pathMatcher = new AntPathMatcher();
    }

    @Override
    public Boolean match(String resourceUrl) {
        // 首先检查用户配置的模式
        if (match(includePatterns, resourceUrl)) {
            log.debug("资源[{}]匹配用户配置模式", resourceUrl);
            return Boolean.TRUE;
        }
        // 如果用户没有配置模式，则使用默认的框架模式
        if (ObjectUtils.isEmpty(includePatterns)) {
            boolean matched = match(DEFAULT_FRAMEWORK_PATTERNS, resourceUrl);
            if (matched) {
                log.debug("资源[{}]匹配默认框架模式", resourceUrl);
            } else {
                log.debug("资源[{}]未匹配任何模式 includePatterns为空, DEFAULT_FRAMEWORK_PATTERNS匹配结果: {}", resourceUrl, matched);
            }
            return matched;
        }
        log.debug("资源[{}]未匹配任何模式", resourceUrl);
        return Boolean.FALSE;
    }

    private Boolean match(Collection<String> patterns, String url){
        if(ObjectUtils.isEmpty(patterns) || ObjectUtils.isEmpty(url)){
            return Boolean.FALSE;
        }
        // 格式化URL路径，确保不以/开头（AntPathMatcher要求）
        String formattedUrl = UrlUtils.formatMatchUrl(url);
        if (formattedUrl.startsWith("/")) {
            formattedUrl = formattedUrl.substring(1);
        }
        log.debug("尝试匹配资源[{}] against patterns: {}", formattedUrl, patterns);
        for (String pattern : patterns) {
            // 确保模式不以/开头
            String formattedPattern = pattern;
            if (formattedPattern.startsWith("/")) {
                formattedPattern = formattedPattern.substring(1);
            }
            boolean match = pathMatcher.match(formattedPattern, formattedUrl);
            log.debug("模式[{}] 匹配 结果: {}", formattedPattern, match);
            if(match){
                return !excludeMatch(excludePatterns, formattedUrl);
            }
        }
        return Boolean.FALSE;
    }

    private Boolean excludeMatch(Collection<String> patterns, String url){
        if(ObjectUtils.isEmpty(patterns) || ObjectUtils.isEmpty(url)){
            return Boolean.FALSE;
        }
        // 格式化URL路径，确保不以/开头（AntPathMatcher要求）
        String formattedUrl = UrlUtils.formatMatchUrl(url);
        if (formattedUrl.startsWith("/")) {
            formattedUrl = formattedUrl.substring(1);
        }
        for (String pattern : patterns) {
            // 确保模式不以/开头
            String formattedPattern = pattern;
            if (formattedPattern.startsWith("/")) {
                formattedPattern = formattedPattern.substring(1);
            }
            boolean match = pathMatcher.match(formattedPattern, formattedUrl);
            if(match){
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

}