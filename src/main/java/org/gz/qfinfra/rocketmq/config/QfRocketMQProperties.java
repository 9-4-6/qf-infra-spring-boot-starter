package org.gz.qfinfra.rocketmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "qf.infra.rocketmq")
public class QfRocketMQProperties {
    /** 生产者配置 */
    private Producer producer = new Producer();

    /** 消费者配置 */
    private Consumer consumer = new Consumer();

    /** 补偿任务配置 */
    private Compensate compensate = new Compensate();

    @Data
    public static class Producer {
        private int asyncThreadPoolSize = 10; // 异步发送线程池大小
        private int sendTimeout = 3000;       // 发送超时时间（毫秒）
        private int retryTimes = 2;           // 发送重试次数
    }

    @Data
    public static class Consumer {
        private int maxRetryTimes = 3;        // 默认最大消费重试次数
        private long retryInterval = 1000;    // 默认重试间隔（毫秒）
    }

    @Data
    public static class Compensate {
        private String cron = "0 0/5 * * * ?"; // 补偿任务定时表达式
        private int batchSize = 50;            // 每次补偿批次大小
    }
}
