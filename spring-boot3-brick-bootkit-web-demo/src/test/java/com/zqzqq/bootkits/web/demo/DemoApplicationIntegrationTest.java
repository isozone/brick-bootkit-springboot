package com.zqzqq.bootkits.web.demo;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.integration.doctor.PluginDoctorReport;
import com.zqzqq.bootkits.integration.doctor.PluginDoctorService;
import com.zqzqq.bootkits.integration.operator.PluginOperator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * web-demo 集成测试：启动完整 Spring 上下文，验证插件框架加载链路。
 * <p>
 * 覆盖：
 * - Spring 上下文可启动（Brick BootKit 自动装配生效）
 * - 主框架核心 Bean（PluginManager / PluginOperator / PluginDoctorService）可注入
 * - 插件扫描链路可执行（插件目录存在与否均不抛异常）
 * - doctor 自检可生成报告
 */
@SpringBootTest(classes = DemoApplication.class)
@DisplayName("web-demo 集成测试")
class DemoApplicationIntegrationTest {

    @Autowired(required = false)
    private PluginManager pluginManager;

    @Autowired(required = false)
    private PluginOperator pluginOperator;

    @Autowired(required = false)
    private PluginDoctorService doctorService;

    @Test
    @DisplayName("Spring 上下文应加载插件框架核心 Bean")
    void contextShouldLoadPluginFramework() {
        assertThat(pluginManager).as("PluginManager 应被自动装配").isNotNull();
        assertThat(pluginOperator).as("PluginOperator 应被自动装配").isNotNull();
        assertThat(doctorService).as("PluginDoctorService 应被自动装配").isNotNull();
    }

    @Test
    @DisplayName("插件扫描链路可执行（空目录不抛异常）")
    void pluginScanShouldNotThrow() {
        List<PluginInfo> plugins = pluginManager.getPlugins();
        assertThat(plugins).isNotNull();
    }

    @Test
    @DisplayName("doctor 自检应生成报告")
    void doctorShouldProduceReport() {
        PluginDoctorReport report = doctorService.diagnose();
        assertThat(report).isNotNull();
        assertThat(report.getGeneratedAt()).isGreaterThan(0);
        assertThat(report.getOverallStatus()).isIn("OK", "WARN", "ERROR");
        assertThat(report.getItems()).isNotNull();
    }
}
