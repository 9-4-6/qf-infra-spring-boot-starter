package org.gz.qfinfra.rocketmq.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.gz.qfinfra.rocketmq.consumer.aop.RocketmqConsumerAop;
import org.gz.qfinfra.rocketmq.producer.RocketmqProducer;
import org.gz.qfinfra.rocketmq.service.RocketmqFailMessageService;
import org.gz.qfinfra.rocketmq.service.impl.RocketmqFailMessageServiceImpl;
import org.gz.qfinfra.rocketmq.task.RocketmqCompensateTask;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author 17853
 */
@Configuration
@ConditionalOnProperty(name = "rocketmq.qf.enabled", havingValue = "true")
@EnableConfigurationProperties(RocketmqProperties.class)
@MapperScan("org.gz.qfinfra.rocketmq.mapper")
@EnableScheduling
public class RocketmqAutoConfiguration {
    /**
     * 注册生产者封装类
     */
    @Bean
    public RocketmqProducer rocketmqProducer(RocketMQTemplate rocketMQTemplate) {
        RocketmqProducer producer = new RocketmqProducer();
        producer.setRocketMQTemplate(rocketMQTemplate);
        return producer;
    }
    /**
     * 注册失败消息服务
     */
    @Bean
    public RocketmqFailMessageService rocketmqFailMessageService( ) {
        return new RocketmqFailMessageServiceImpl();
    }
    /**
     * 注册消费者AOP拦截器
     */
    @Bean
    public RocketmqConsumerAop rocketmqConsumerAop(RocketmqFailMessageService failMessageService) {
        RocketmqConsumerAop aop = new RocketmqConsumerAop();
        aop.setFailMessageService(failMessageService);
        return aop;
    }

    @Bean
    public RocketmqCompensateTask rocketmqCompensateTask(
            RocketmqConsumerAop aop,
            RocketmqFailMessageService failMessageService,
            RocketmqProperties rocketmqProperties) {
        return new RocketmqCompensateTask(aop,failMessageService,rocketmqProperties);
    }
}
