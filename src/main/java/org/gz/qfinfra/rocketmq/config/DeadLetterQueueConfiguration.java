package org.gz.qfinfra.rocketmq.config;

import lombok.extern.slf4j.Slf4j;
import org.gz.qfinfra.rocketmq.consumer.DeadLetterMessageDispatcher;
import org.gz.qfinfra.rocketmq.consumer.DeadLetterMessageHandler;
import org.gz.qfinfra.rocketmq.consumer.DeadLetterQueueConsumer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * @author guozhong
 * @date 2025/11/28
 * @description 死信队列配置
 */
@Configuration
@ConditionalOnProperty(name = "rocketmq.qf.dead-letter.enabled", havingValue = "true")
@EnableConfigurationProperties(DeadLetterProperties.class)
@Slf4j
public class DeadLetterQueueConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public DeadLetterMessageDispatcher deadLetterMessageDispatcher(
            ObjectProvider<List<DeadLetterMessageHandler>> handlersProvider) {
        return new DeadLetterMessageDispatcher(handlersProvider.getIfAvailable(Collections::emptyList));
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    @ConditionalOnBean(DeadLetterMessageDispatcher.class)
    public DeadLetterQueueConsumer deadLetterQueueConsumer(
            DeadLetterProperties properties,
            DeadLetterMessageDispatcher dispatcher) {
        return new DeadLetterQueueConsumer(properties, dispatcher);
    }
}
