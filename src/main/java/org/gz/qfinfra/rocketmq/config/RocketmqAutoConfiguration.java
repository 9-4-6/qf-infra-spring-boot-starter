package org.gz.qfinfra.rocketmq.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.gz.qfinfra.rocketmq.producer.RocketmqProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * @author 17853
 */
@Configuration
@ConditionalOnProperty(name = "rocketmq.qf.producer.enabled", havingValue = "true")
public class RocketmqAutoConfiguration {
    /**
     * 注册生产者封装类
     */
    @Bean
    public RocketmqProducer rocketmqProducer(RocketMQTemplate rocketMqTemplate) {
        RocketmqProducer producer = new RocketmqProducer();
        producer.setRocketMqTemplate(rocketMqTemplate);
        return producer;
    }

}
