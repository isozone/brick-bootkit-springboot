package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.exception.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 日志查看 Web 服务单元测试。
 */
@DisplayName("LogWebService Test")
class LogWebServiceTest {

    @TempDir
    Path tempDir;

    private LogWebService service;

    @BeforeEach
    void setUp() {
        service = new LogWebService();
    }

    @Test
    @DisplayName("无日志文件时返回 null 路径")
    void getLogFileShouldReturnNullWhenMissing() {
        // 工作目录下无日志文件，且未配置 logging.file.name
        assertThat(service.getLogFile()).isNull();
    }

    @Test
    @DisplayName("读取日志按关键字过滤")
    void readLogsShouldFilterByKeyword() throws Exception {
        // 通过系统属性模拟日志文件（读取逻辑依赖系统属性探测）
        Path logFile = tempDir.resolve("spring.log");
        Files.writeString(logFile, "INFO line-1\nERROR plugin-a failed\nINFO line-3\n",
                StandardCharsets.UTF_8);
        System.setProperty("logging.file.name", logFile.toString());
        try {
            List<String> filtered = service.readLogs("plugin-a", 100);
            assertThat(filtered).hasSize(1);
            assertThat(filtered.get(0)).contains("plugin-a");
        } finally {
            System.clearProperty("logging.file.name");
        }
    }

    @Test
    @DisplayName("读取日志无关键字返回全部")
    void readLogsShouldReturnAllWhenNoKeyword() throws Exception {
        Path logFile = tempDir.resolve("app.log");
        Files.writeString(logFile, "line-1\nline-2\nline-3\n", StandardCharsets.UTF_8);
        System.setProperty("logging.file.name", logFile.toString());
        try {
            List<String> lines = service.readLogs("", 100);
            assertThat(lines).hasSize(3);
        } finally {
            System.clearProperty("logging.file.name");
        }
    }

    @Test
    @DisplayName("日志文件不存在时抛出异常")
    void readLogsShouldFailWhenFileMissing() {
        System.setProperty("logging.file.name", tempDir.resolve("nonexistent.log").toString());
        try {
            assertThatThrownBy(() -> service.readLogs("", 10))
                    .isInstanceOf(PluginException.class)
                    .hasMessageContaining("未找到日志文件");
        } finally {
            System.clearProperty("logging.file.name");
        }
    }
}
