package org.gz.qfinfra.rocketmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 17853
 */
@Data
@ConfigurationProperties(prefix = "rocketmq.qf")
public class RocketmqProperties {
    /**
     * 消费者最大重试次数（超过则存储到数据库）
     */
    private Integer consumerMaxRetryTimes = 3;

    /**
     * 补偿任务Cron表达式
     */
    private String compensateCron = "0/10 * * * * ?";

    /**
     * 最大补偿重试次数
     */
    private Integer compensateMaxRetry = 3;
}
