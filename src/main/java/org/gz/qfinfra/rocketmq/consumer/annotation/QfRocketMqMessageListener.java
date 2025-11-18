package org.gz.qfinfra.rocketmq.consumer.annotation;

import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.SelectorType;

import java.lang.annotation.*;

/**
 * @author 17853
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QfRocketMqMessageListener {
    // 消息主题
    String topic();
    // 消息标签
    String tag() default "*";
    // 消费者组
    String consumerGroup();
    // 消费模式（并发/顺序）
    ConsumeMode consumeMode() default ConsumeMode.CONCURRENTLY;
    // 消息模型（集群/广播）
    MessageModel messageModel() default MessageModel.CLUSTERING;
    // 选择器类型
    SelectorType selectorType() default SelectorType.TAG;
    // 选择器表达式
    String selectorExpression() default "*";
    // 最大重试次数（-1表示使用全局配置)
    int maxRetryTimes() default -1;
    // 重试间隔（-1表示使用全局配置)
    long retryInterval() default -1;
}
