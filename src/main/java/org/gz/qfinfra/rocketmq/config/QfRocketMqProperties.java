package org.gz.qfinfra.rocketmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 17853
 */
@Data
@ConfigurationProperties(prefix = "qf.infra.rocketmq")
public class QfRocketMqProperties {
    // 生产者配置（包含你提到的三个属性）
    private int producerAsyncThreadPoolSize = 10;
    private int producerSendTimeout = 3000;
    private int producerRetryTimes = 2;
    private String producerGroup;
    private String producerNamesrvAddr;

    // 消费者配置
    private int consumerMaxRetryTimes = 3;
    private long consumerRetryInterval = 1000;
    private int consumerCorePoolSize = 20;
    private String consumerGroup;
    private String consumerNamesrvAddr;

    // 补偿任务配置
    private String compensateCron = "0 0/5 * * * ?";
    private int compensateBatchSize = 50;
    private int compensateMaxTimes = 10;
}
