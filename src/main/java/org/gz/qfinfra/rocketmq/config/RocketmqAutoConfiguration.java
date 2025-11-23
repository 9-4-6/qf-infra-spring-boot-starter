package org.gz.qfinfra.rocketmq.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.gz.qfinfra.rocketmq.consumer.aop.RocketmqConsumerAop;
import org.gz.qfinfra.rocketmq.producer.RocketmqProducer;
import org.gz.qfinfra.rocketmq.service.RocketmqFailMessageService;
import org.gz.qfinfra.rocketmq.service.impl.RocketmqFailMessageServiceImpl;
import org.gz.qfinfra.rocketmq.task.RocketmqCompensateTask;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
// 启用自定义属性配置
@EnableConfigurationProperties(RocketmqProperties.class)
// 仅当RocketMQTemplate存在时生效（确保引入了RocketMQ依赖）
@ConditionalOnClass(RocketMQTemplate.class)
// 扫描Mapper接口（MyBatis-Plus）
@MapperScan("com.qf.rocketmq.mapper")
// 开启定时任务
@EnableScheduling
public class RocketmqAutoConfiguration {
    /**
     * 注册消费者AOP拦截器
     */
    @Bean
    public RocketmqConsumerAop rocketmqConsumerAop(RocketmqProperties rocketmqProperties,
                                                   RocketmqFailMessageService failMessageService) {
        RocketmqConsumerAop aop = new RocketmqConsumerAop();
        aop.setConsumerMaxRetryTimes(rocketmqProperties.getConsumerMaxRetryTimes());
        aop.setFailMessageService(failMessageService);
        return aop;
    }

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
    public RocketmqFailMessageService rocketmqFailMessageService(RocketmqProperties rocketmqProperties) {
        RocketmqFailMessageServiceImpl service = new RocketmqFailMessageServiceImpl();
        service.setRocketmqProperties(rocketmqProperties);
        return service;
    }

    /**
     * 注册补偿定时任务
     */
    @Bean
    public RocketmqCompensateTask rocketmqCompensateTask(RocketmqFailMessageService failMessageService,
                                                         RocketmqProducer rocketmqProducer,
                                                         RocketmqProperties rocketmqProperties) {
        RocketmqCompensateTask task = new RocketmqCompensateTask();
        task.setFailMessageService(failMessageService);
        task.setRocketmqProducer(rocketmqProducer);
        task.setRocketmqProperties(rocketmqProperties);
        return task;
    }
}
