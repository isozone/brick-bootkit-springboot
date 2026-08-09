package com.zqzqq.bootkits.integration.registry;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.sdk.annotation.BrickServiceReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * {@link BrickServiceReferenceInjector} 单元测试。
 */
@DisplayName("BrickServiceReferenceInjector Test")
class BrickServiceReferenceInjectorTest {

    /** 测试服务接口 */
    public interface GreetingService {
        String greet(String name);
    }

    /** 测试 Bean：字段类型即服务接口 */
    static class ConsumerBean {
        @BrickServiceReference
        private GreetingService greetingService;

        public GreetingService getGreetingService() {
            return greetingService;
        }
    }

    /** 测试 Bean：注解显式声明接口 */
    static class ExplicitBean {
        @BrickServiceReference(value = GreetingService.class)
        private Object anyField;

        public Object getAnyField() {
            return anyField;
        }
    }

    /** 测试 Bean：可选引用 */
    static class OptionalBean {
        @BrickServiceReference(optional = true)
        private GreetingService optionalService;

        public GreetingService getOptionalService() {
            return optionalService;
        }
    }

    private PluginServiceRegistry registry;
    private BrickServiceReferenceInjector injector;

    @BeforeEach
    void setUp() {
        registry = mock(PluginServiceRegistry.class);
        injector = new BrickServiceReferenceInjector(registry);
    }

    @Test
    @DisplayName("按字段类型注入代理，调用委托给注册中心服务")
    void injectShouldCreateProxyAndDelegate() {
        GreetingService realService = mock(GreetingService.class);
        when(realService.greet("world")).thenReturn("Hello world");
        when(registry.getService(null, GreetingService.class)).thenReturn(realService);

        ConsumerBean bean = new ConsumerBean();
        injector.injectReferences(bean, "consumer-plugin");

        assertThat(bean.getGreetingService()).isNotNull();
        assertThat(bean.getGreetingService()).isNotSameAs(realService);
        assertThat(bean.getGreetingService().greet("world")).isEqualTo("Hello world");
        verify(registry).getService(null, GreetingService.class);
        verify(realService).greet("world");
    }

    @Test
    @DisplayName("注解显式声明的接口优先于字段类型")
    void injectShouldPreferExplicitInterface() {
        GreetingService realService = mock(GreetingService.class);
        when(realService.greet("explicit")).thenReturn("Hello explicit");
        when(registry.getService(null, GreetingService.class)).thenReturn(realService);

        ExplicitBean bean = new ExplicitBean();
        injector.injectReferences(bean, "consumer-plugin");

        // 懒代理：调用代理方法时才触发 getService 委托
        assertThat(bean.getAnyField()).isNotNull();
        Object result = ((GreetingService) bean.getAnyField()).greet("explicit");
        assertThat(result).isEqualTo("Hello explicit");
        verify(registry).getService(null, GreetingService.class);
        verify(realService).greet("explicit");
    }

    @Test
    @DisplayName("服务缺失时非可选代理调用抛出 IllegalStateException")
    void injectShouldThrowWhenServiceMissingForRequired() {
        when(registry.getService(null, GreetingService.class)).thenReturn(null);

        ConsumerBean bean = new ConsumerBean();
        injector.injectReferences(bean, "consumer-plugin");

        assertThat(bean.getGreetingService()).isNotNull();
        assertThatThrownBy(() -> bean.getGreetingService().greet("world"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未找到跨插件服务实现");
    }

    @Test
    @DisplayName("可选引用在服务缺失时注入代理但不抛异常")
    void injectShouldSupportOptionalReference() {
        when(registry.getService(null, GreetingService.class)).thenReturn(null);

        OptionalBean bean = new OptionalBean();
        injector.injectReferences(bean, "consumer-plugin");

        assertThat(bean.getOptionalService()).isNotNull();
        // 可选引用：字段被注入代理，调用时仍会报错（可选表示注入阶段不阻断，调用由调用方处理）
        assertThatThrownBy(() -> bean.getOptionalService().greet("world"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("无注解字段的 Bean 不受影响")
    void injectShouldIgnorePlainBeans() {
        class PlainBean {
            private String name = "plain";
        }
        PlainBean bean = new PlainBean();
        injector.injectReferences(bean, "consumer-plugin");

        assertThat(bean.name).isEqualTo("plain");
        verifyNoInteractions(registry);
    }

    @Test
    @DisplayName("null Bean 直接返回")
    void injectShouldHandleNullBean() {
        injector.injectReferences(null, "consumer-plugin");
        verifyNoInteractions(registry);
    }

    @Test
    @DisplayName("注册中心为 null 时不注入")
    void injectShouldHandleNullRegistry() {
        BrickServiceReferenceInjector emptyInjector = new BrickServiceReferenceInjector(null);
        ConsumerBean bean = new ConsumerBean();
        emptyInjector.injectReferences(bean, "consumer-plugin");

        assertThat(bean.getGreetingService()).isNull();
    }
}
