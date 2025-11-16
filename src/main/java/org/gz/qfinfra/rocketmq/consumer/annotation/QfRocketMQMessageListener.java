package org.gz.qfinfra.rocketmq.consumer.annotation;

import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.SelectorType;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QfRocketMQMessageListener {
    String topic(); // 消息主题

    String tag() default "*"; // 消息标签

    String consumerGroup(); // 消费者组

    ConsumeMode consumeMode() default ConsumeMode.CONCURRENTLY; // 消费模式（并发/顺序）

    MessageModel messageModel() default MessageModel.CLUSTERING; // 消息模型（集群/广播）

    SelectorType selectorType() default SelectorType.TAG; // 选择器类型

    String selectorExpression() default "*"; // 选择器表达式

    int maxRetryTimes() default -1; // 最大重试次数（-1表示使用全局配置）

    long retryInterval() default -1; // 重试间隔（-1表示使用全局配置
}
