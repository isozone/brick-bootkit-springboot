package com.zqzqq.bootkits.web.scripts.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.web.scripts.storage.ScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.impl.FileScriptStorage;
import com.zqzqq.bootkits.web.scripts.storage.impl.JdbcScriptStorage;
import com.zqzqq.bootkits.web.scripts.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 脚本管理存储自动配置类
 * 根据配置自动选择存储实现
 * 
 * @author brick-bootkit
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "brick.scripts", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "com.baomidou.mybatisplus.core.mapper.BaseMapper")
@EnableConfigurationProperties(ScriptStorageProperties.class)
public class ScriptStorageAutoConfiguration {
    
    /**
     * 文件存储Bean（默认）
     * 当没有其他ScriptStorage实现时，使用文件存储
     */
    @Bean(name = "scriptStorage")
    @ConditionalOnMissingBean(name = "scriptStorage")
    @ConditionalOnProperty(prefix = "brick.scripts.storage", name = "type", havingValue = "file", matchIfMissing = true)
    public ScriptStorage fileScriptStorage(ScriptStorageProperties properties) {
        log.info("Initializing FileScriptStorage with data-path: {}", properties.getFile().getDataPath());
        return new FileScriptStorage(properties.getFile().getDataPath());
    }
    
    /**
     * 数据库存储Bean（可选）
     * 当存在DataSource且配置为jdbc时使用
     */
    @Bean(name = "jdbcScriptStorage")
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(name = "scriptStorage")
    @ConditionalOnProperty(prefix = "brick.scripts.storage", name = "type", havingValue = "jdbc")
    @Primary
    public ScriptStorage jdbcScriptStorage(ScriptStorageProperties properties,
                                               ScriptInfoMapper scriptInfoMapper,
                                               ScriptExecutionMapper scriptExecutionMapper,
                                               SchedulerTaskMapper schedulerTaskMapper,
                                               QueueItemMapper queueItemMapper,
                                               BatchJobMapper batchJobMapper,
                                               ScriptTemplateMapper scriptTemplateMapper,
                                               ObjectMapper objectMapper) {
        log.info("Initializing JdbcScriptStorage");
        return new JdbcScriptStorage(
                scriptInfoMapper,
                scriptExecutionMapper,
                schedulerTaskMapper,
                queueItemMapper,
                batchJobMapper,
                scriptTemplateMapper,
                objectMapper
        );
    }
    
    /**
     * 自定义存储Bean
     * 当配置为custom时使用用户自定义的存储实现
     */
    @Bean(name = "scriptStorage")
    @ConditionalOnProperty(prefix = "brick.scripts.storage", name = "type", havingValue = "custom")
    public ScriptStorage customScriptStorage(ScriptStorageProperties properties,
                                                        org.springframework.context.ApplicationContext context) {
        String beanName = properties.getCustomBeanName();
        log.info("Initializing custom ScriptStorage with bean-name: {}", beanName);
        return context.getBean(beanName, ScriptStorage.class);
    }
}