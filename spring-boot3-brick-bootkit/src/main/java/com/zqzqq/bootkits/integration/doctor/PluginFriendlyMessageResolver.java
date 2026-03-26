package com.zqzqq.bootkits.integration.doctor;

import org.springframework.util.StringUtils;

/**
 * Converts low-level plugin errors into beginner-friendly guidance.
 */
public final class PluginFriendlyMessageResolver {

    private PluginFriendlyMessageResolver() {
    }

    public static Resolution resolve(Throwable throwable) {
        if (throwable == null) {
            return new Resolution("UNKNOWN_PLUGIN_ERROR", "未知插件错误", null);
        }
        String message = throwable.getMessage();
        if (!StringUtils.hasText(message)) {
            message = throwable.getClass().getSimpleName();
        }
        return resolve(message);
    }

    public static Resolution resolve(String message) {
        if (!StringUtils.hasText(message)) {
            return new Resolution("UNKNOWN_PLUGIN_ERROR", "未知插件错误", null);
        }
        PluginIssueDefinition issue = PluginIssueDefinition.detect(message);
        String resolvedMessage = appendSuggestion(message, issue);
        return new Resolution(issue != null ? issue.getKey() : "UNKNOWN_PLUGIN_ERROR", resolvedMessage, issue);
    }

    private static String appendSuggestion(String message, PluginIssueDefinition issue) {
        if (issue == null || !StringUtils.hasText(issue.getSuggestion()) || message.contains("建议：")) {
            return message;
        }
        return message + " 建议：" + issue.getSuggestion();
    }

    public static final class Resolution {
        private final String errorKey;
        private final String message;
        private final PluginIssueDefinition issue;

        private Resolution(String errorKey, String message, PluginIssueDefinition issue) {
            this.errorKey = errorKey;
            this.message = message;
            this.issue = issue;
        }

        public String getErrorKey() {
            return errorKey;
        }

        public String getMessage() {
            return message;
        }

        public String getHintPath() {
            return issue != null ? issue.getDocPath() : null;
        }

        public String getHintAnchor() {
            return issue != null ? issue.getDocAnchor() : null;
        }

        public PluginIssueDefinition getIssue() {
            return issue;
        }
    }
}
