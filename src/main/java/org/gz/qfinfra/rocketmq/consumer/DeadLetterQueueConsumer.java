package org.gz.qfinfra.rocketmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.gz.qfinfra.rocketmq.config.DeadLetterProperties;

/**
 * @author guozhong
 * @date 2025/11/28
 * @description 私信队列消费者
 */
@Slf4j
public class DeadLetterQueueConsumer {
    private DefaultMQPushConsumer consumer;
    private final DeadLetterProperties properties;
    private final DeadLetterMessageDispatcher dispatcher;

    public DeadLetterQueueConsumer(DeadLetterProperties properties,
                                   DeadLetterMessageDispatcher dispatcher) {
        this.properties = properties;
        this.dispatcher = dispatcher;
    }

    public void start() throws MQClientException {

        consumer = new DefaultMQPushConsumer(properties.getConsumerGroup());
        consumer.setNamesrvAddr(properties.getNameServer());
        // 设置最小消费线程数
        consumer.setConsumeThreadMin(properties.getConsumeThreadMin());
        // 设置最大消费线程数
        consumer.setConsumeThreadMax(properties.getConsumeThreadMax());

        // 订阅所有死信队列
        // %DLQ%* 表示订阅所有以 %DLQ% 开头的 Topic（即所有死信队列）
        // * 表示订阅所有 tags
        consumer.subscribe(properties.getTopicPattern(), properties.getTagPattern());

        // 注册消息监听器 - 当有死信消息时回调此方法
        consumer.registerMessageListener((MessageListenerConcurrently) (msg, context) -> {
            // 将收到的死信消息批量交给分发器处理
            return dispatcher.handleMessages(msg);
        });

        consumer.start();
        log.info("Global Dead Letter Queue Consumer started, group: {}",
                properties.getConsumerGroup());
    }
    public void shutdown() {
        if (consumer != null) {
            consumer.shutdown();
            log.info("Global Dead Letter Queue Consumer shutdown");
        }
    }
}
