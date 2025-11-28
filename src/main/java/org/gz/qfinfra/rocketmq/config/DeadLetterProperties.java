package org.gz.qfinfra.rocketmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author guozhong
 * @date 2025/11/28
 * @description 私信队列服务
 */
@Data
@ConfigurationProperties(prefix = "rocketmq.qf.dead-letter")
public class DeadLetterProperties {
    private String nameServer;
    private String consumerGroup = "dlq-global-processor";
    private int consumeThreadMin = 5;
    private int consumeThreadMax = 10;
    private String topicPattern = "%DLQ%*";
    private String tagPattern = "*";
}
