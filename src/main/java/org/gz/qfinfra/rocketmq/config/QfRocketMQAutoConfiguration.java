package org.gz.qfinfra.rocketmq.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.gz.qfinfra.rocketmq.consumer.aop.RocketMQConsumerAop;
import org.gz.qfinfra.rocketmq.producer.QfRocketMQProducer;
import org.gz.qfinfra.rocketmq.repository.RocketmqFailMessageMapper;
import org.gz.qfinfra.rocketmq.service.RocketmqFailMessageService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConditionalOnClass(name = "org.apache.rocketmq.spring.core.RocketMQTemplate") // 存在 RocketMQ 依赖时才生效
@AutoConfigureAfter({MybatisPlusAutoConfiguration.class}) // 确保 MyBatis-Plus 先初始化
@EnableConfigurationProperties(QfRocketMQProperties.class) // 启用配置属性绑定
@EnableAspectJAutoProxy(proxyTargetClass = true) // 启用 AOP（cglib 代理）
@EnableScheduling // 启用定时补偿任务
public class QfRocketMQAutoConfiguration {
    private final QfRocketMQProperties properties;

    // 注入配置属性
    public QfRocketMQAutoConfiguration(QfRocketMQProperties properties) {
        this.properties = properties;
    }

    /**
     * 注册统一生产者 Bean（外部项目直接 @Autowired 注入）
     */
    @Bean
    @ConditionalOnMissingBean // 外部项目未自定义时，使用默认实现
    public QfRocketMQProducer qfRocketMQProducer() {
        // 可通过 properties 配置生产者（如线程池大小）
        return new QfRocketMQProducer(properties.getProducer());
    }

    /**
     * 注册消费者 AOP 增强（统一日志、重试）
     */
    @Bean
    @ConditionalOnMissingBean
    public RocketMQConsumerAop rocketMQConsumerAop(RocketmqFailMessageService failMessageService) {
        return new RocketMQConsumerAop(failMessageService, properties.getConsumer());
    }

    /**
     * 注册失败消息服务（持久化+补偿）
     */
    @Bean
    @ConditionalOnMissingBean
    public RocketmqFailMessageService rocketmqFailMessageService(
            RocketmqFailMessageMapper failMessageMapper,
            QfRocketMQProducer rocketMQProducer) {
        return new RocketmqFailMessageService(
                failMessageMapper,
                rocketMQProducer,
                properties.getCompensate()
        );
    }
}
