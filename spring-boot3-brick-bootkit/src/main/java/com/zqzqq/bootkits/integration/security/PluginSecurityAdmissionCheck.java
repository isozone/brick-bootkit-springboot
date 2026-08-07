package com.zqzqq.bootkits.integration.security;

import com.zqzqq.bootkits.core.admission.PluginAdmissionCheck;
import com.zqzqq.bootkits.core.admission.PluginAdmissionContext;
import com.zqzqq.bootkits.core.admission.PluginAdmissionDecision;
import com.zqzqq.bootkits.core.admission.PluginAdmissionOperation;
import com.zqzqq.bootkits.core.security.PluginSecurityManager;
import com.zqzqq.bootkits.core.security.PluginSecurityValidationResult;

import java.nio.file.Path;
import java.util.List;

/**
 * 插件安装安全准入检查。
 * <p>
 * 通过实现 {@link PluginAdmissionCheck} SPI 自动接入主框架的 admission pipeline：
 * 在插件安装（INSTALL）时调用 {@link PluginSecurityManager} 执行代码扫描、
 * 权限与安全策略校验，并依据违规数量产出 WARN / REJECT 决策。
 * <p>
 * 校验策略：
 * <ul>
 *   <li>1-2 个违规：WARN（允许安装，记录告警）</li>
 *   <li>3 个及以上违规：REJECT（在 enforce 模式下阻止安装）</li>
 * </ul>
 */
public class PluginSecurityAdmissionCheck implements PluginAdmissionCheck {

    public static final String CHECK_NAME = "plugin-security";

    private static final int REJECT_VIOLATION_THRESHOLD = 3;

    private final PluginSecurityManager securityManager;

    public PluginSecurityAdmissionCheck(PluginSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public String getName() {
        return CHECK_NAME;
    }

    @Override
    public PluginAdmissionDecision check(PluginAdmissionContext context) {
        if (context.getOperation() != PluginAdmissionOperation.INSTALL) {
            return PluginAdmissionDecision.pass("operation " + context.getOperation() + " 无需安全检查");
        }

        Path pluginPath = context.getPluginPath();
        if (pluginPath == null) {
            return PluginAdmissionDecision.warn("插件路径为空，跳过安全检查");
        }

        PluginSecurityValidationResult result =
                securityManager.validatePluginSecurity(context.getPluginId(), pluginPath);
        if (result == null || result.isValid()) {
            return PluginAdmissionDecision.pass("插件安全校验通过");
        }

        List<String> violations = result.getViolations();
        if (violations == null || violations.isEmpty()) {
            return PluginAdmissionDecision.warn("插件安全校验存在告警: " + result.getSummary());
        }

        String detail = String.join("; ", violations);
        if (violations.size() >= REJECT_VIOLATION_THRESHOLD) {
            return PluginAdmissionDecision.reject("插件存在 " + violations.size()
                    + " 个安全违规: " + detail);
        }
        return PluginAdmissionDecision.warn("插件存在 " + violations.size()
                + " 个安全违规（未达拒绝阈值）: " + detail);
    }
}
