package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationException;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.LogWebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 日志查看 Controller 单元测试。
 */
@DisplayName("LogController Test")
class LogControllerTest {

    private LogWebService logWebService;
    private PluginWebAuthorizationService authorizationService;
    private LogController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        logWebService = mock(LogWebService.class);
        authorizationService = mock(PluginWebAuthorizationService.class);
        ObjectProvider<LogWebService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(logWebService);
        controller = new LogController(provider, authorizationService);
    }

    @Test
    @DisplayName("获取当前日志文件路径")
    void logFileShouldReturnPath() {
        when(logWebService.getLogFile()).thenReturn("/var/log/app.log");

        ApiResult<String> result = controller.logFile();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo("/var/log/app.log");
        verify(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);
    }

    @Test
    @DisplayName("读取最近日志")
    void logsShouldReturnLines() {
        List<String> lines = Arrays.asList("line-1", "line-2");
        when(logWebService.readLogs("demo-plugin", 200)).thenReturn(lines);

        ApiResult<List<String>> result = controller.logs("demo-plugin", 200);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly("line-1", "line-2");
        verify(logWebService).readLogs("demo-plugin", 200);
    }

    @Test
    @DisplayName("读取日志使用默认行数")
    void logsShouldUseDefaultLines() {
        when(logWebService.readLogs(null, 200)).thenReturn(List.of());

        ApiResult<List<String>> result = controller.logs(null, 200);

        assertThat(result.isSuccess()).isTrue();
        verify(logWebService).readLogs(null, 200);
    }

    @Test
    @DisplayName("鉴权失败时抛出授权异常")
    void logFileShouldPropagateAuthorizationFailure() {
        doThrow(new PluginWebAuthorizationException("denied"))
                .when(authorizationService).check(PluginWebPermission.PLUGIN_VIEW, null);

        assertThatThrownBy(() -> controller.logFile())
                .isInstanceOf(PluginWebAuthorizationException.class);
        verifyNoInteractions(logWebService);
    }
}
