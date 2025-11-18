package org.gz.qfinfra.rocketmq.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import jakarta.annotation.Resource;
import org.gz.qfinfra.rocketmq.consumer.aop.RocketMqConsumerAop;
import org.gz.qfinfra.rocketmq.producer.QfRocketMqProducer;
import org.gz.qfinfra.rocketmq.service.RocketmqFailMessageService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author 17853
 */
@Configuration
@ConditionalOnClass(name = "org.apache.rocketmq.spring.core.RocketMQTemplate") // 存在 RocketMQ 依赖时才生效
@AutoConfigureAfter({MybatisPlusAutoConfiguration.class}) // 确保 MyBatis-Plus 先初始化
@EnableConfigurationProperties(QfRocketMqProperties.class) // 启用配置属性绑定
@EnableAspectJAutoProxy(proxyTargetClass = true) // 启用 AOP（cglib 代理）
@EnableScheduling // 启用定时补偿任务
public class QfRocketMqAutoConfiguration {
    @Resource
    private QfRocketMqProperties properties;
    /**
     * 注册统一生产者 Bean
     */
    @Bean
    // 外部项目未自定义时，使用默认实现
    @ConditionalOnMissingBean
    public QfRocketMqProducer qfRocketMqProducer() {
        return new QfRocketMqProducer(properties);
    }

    /**
     * 注册消费者 AOP 增强（统一日志、重试）
     */
    @Bean
    @ConditionalOnMissingBean
    public RocketMqConsumerAop rocketMqConsumerAop() {
        return new RocketMqConsumerAop(properties);
    }

    /**
     * 注册失败消息服务（持久化）
     */
    @Bean
    @ConditionalOnMissingBean
    public RocketmqFailMessageService rocketmqFailMessageService() {
        return new RocketmqFailMessageService();
    }
}
