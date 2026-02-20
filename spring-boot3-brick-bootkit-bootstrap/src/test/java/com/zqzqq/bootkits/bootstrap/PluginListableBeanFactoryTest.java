package com.zqzqq.bootkits.bootstrap;

import com.zqzqq.bootkits.bootstrap.annotation.AutowiredType;
import com.zqzqq.bootkits.bootstrap.processor.ProcessorContext;
import com.zqzqq.bootkits.spring.MainApplicationContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.DependencyDescriptor;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * PluginListableBeanFactory tests.
 */
class PluginListableBeanFactoryTest {

    @Test
    void resolveDependency_MainType_ShouldFallbackToPluginBeanInStandalone() throws Exception {
        ProcessorContext context = Mockito.mock(ProcessorContext.class);
        when(context.getMainApplicationContext()).thenReturn(new EmptyMainApplicationContext());

        PluginListableBeanFactory beanFactory = new MainOnlyBeanFactory(context);
        beanFactory.registerSingleton("sampleService", new SampleService());

        Field field = Holder.class.getDeclaredField("sampleService");
        DependencyDescriptor descriptor = new DependencyDescriptor(field, true);
        Set<String> autowiredBeanNames = new HashSet<>();
        Object dependency = beanFactory.resolveDependency(
                descriptor, "holder", autowiredBeanNames, (TypeConverter) null);

        assertNotNull(dependency);
        assertTrue(dependency instanceof SampleService);
    }

    @Test
    void resolveDependency_MainType_ShouldNotFallbackWhenNotStandalone() throws Exception {
        MainApplicationContext mainApplicationContext = Mockito.mock(MainApplicationContext.class);
        when(mainApplicationContext.resolveDependency(anyString(), any())).thenReturn(null);

        ProcessorContext context = Mockito.mock(ProcessorContext.class);
        when(context.getMainApplicationContext()).thenReturn(mainApplicationContext);

        PluginListableBeanFactory beanFactory = new MainOnlyBeanFactory(context);
        beanFactory.registerSingleton("sampleService", new SampleService());

        Field field = Holder.class.getDeclaredField("sampleService");
        DependencyDescriptor descriptor = new DependencyDescriptor(field, true);

        assertThrows(NoSuchBeanDefinitionException.class, () ->
                beanFactory.resolveDependency(descriptor, "holder", new HashSet<>(), (TypeConverter) null));
    }

    private static class MainOnlyBeanFactory extends PluginListableBeanFactory {
        public MainOnlyBeanFactory(ProcessorContext processorContext) {
            super(processorContext);
        }

        @Override
        protected AutowiredTypeResolver getAutowiredTypeResolver(ProcessorContext processorContext) {
            return dependencyDescriptor -> AutowiredType.Type.MAIN;
        }
    }

    private static class Holder {
        @SuppressWarnings("unused")
        private SampleService sampleService;
    }

    private static class SampleService {
    }
}
