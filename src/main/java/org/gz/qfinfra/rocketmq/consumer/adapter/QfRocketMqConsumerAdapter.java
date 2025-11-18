package org.gz.qfinfra.rocketmq.consumer.adapter;

import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.gz.qfinfra.rocketmq.consumer.annotation.QfRocketMqMessageListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

/**
 * @author 17853
 */
@Component
public abstract class QfRocketMqConsumerAdapter<T> implements RocketMQListener<T> {
    /**
     * 手动构建原生注解（补全所有抽象方法）
     */
    public RocketMQMessageListener getRocketMqNativeAnnotation() {
        // 1. 获取自定义注解
        QfRocketMqMessageListener customAnnotation = AnnotationUtils.findAnnotation(
                getClass(), QfRocketMqMessageListener.class
        );
        if (customAnnotation == null) {
            throw new IllegalArgumentException("消费者必须添加 @QfRocketMQMessageListener 注解");
        }

        // 2. 处理 tag 与 selectorExpression 的合并（核心：自定义 tag 优先级高于 selectorExpression）
        String finalSelectorExpression;
        // 若指定了 tag，优先使用 tag 作为选择器
        if (!"*".equals(customAnnotation.tag())) {
            finalSelectorExpression = customAnnotation.tag();
        } else { // 未指定 tag，使用自定义注解的 selectorExpression
            finalSelectorExpression = customAnnotation.selectorExpression();
        }

        // 3. 实现所有抽象方法（包含自定义属性的映射）
        return new RocketMQMessageListener() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return RocketMQMessageListener.class;
            }

            // -------------------------- 核心属性映射（包含自定义扩展）--------------------------
            @Override
            public String topic() {
                return customAnnotation.topic(); // 映射自定义 topic
            }

            @Override
            public String consumerGroup() {
                return customAnnotation.consumerGroup(); // 映射自定义消费者组
            }

            @Override
            public ConsumeMode consumeMode() {
                return customAnnotation.consumeMode(); // 映射自定义消费模式
            }

            @Override
            public MessageModel messageModel() {
                return customAnnotation.messageModel(); // 映射自定义消息模型
            }

            @Override
            public SelectorType selectorType() {
                return customAnnotation.selectorType(); // 映射自定义选择器类型
            }

            @Override
            public String selectorExpression() {
                return finalSelectorExpression; // 使用合并后的 tag/selectorExpression
            }

            // -------------------------- 原生注解默认值方法（补充自定义属性的适配）--------------------------
            @Override
            public int maxReconsumeTimes() {
                // 若自定义注解指定了 maxRetryTimes 且不为 -1，则使用自定义值；否则用原生默认值 -1
                return customAnnotation.maxRetryTimes() != -1
                        ? customAnnotation.maxRetryTimes()
                        : -1;
            }

            // -------------------------- 其他原生注解默认值（保持不变）--------------------------
            @Override
            public int consumeThreadNumber() {
                return 20; // 原生默认值
            }

            @Override
            public int consumeThreadMax() {
                return 64; // 原生默认值
            }

            @Override
            public long consumeTimeout() {
                return 15L; // 原生默认值（分钟）
            }

            @Override
            public boolean enableMsgTrace() {
                return true; // 原生默认值
            }

            @Override
            public String customizedTraceTopic() {
                return "RMQ_SYS_TRACE_TOPIC"; // 原生默认值
            }

            @Override
            public String accessKey() {
                return ""; // 原生默认值
            }

            @Override
            public String secretKey() {
                return ""; // 原生默认值
            }

            @Override
            public String namespace() {
                return ""; // 原生默认值
            }

            @Override
            public int replyTimeout() {
                return 3000; // 原生默认值（3秒）
            }

            @Override
            public String nameServer() {
                return ""; // 原生默认值
            }

            @Override
            public String accessChannel() {
                return "LOCAL"; // 原生默认值
            }

            @Override
            public String tlsEnable() {
                return "false"; // 原生默认值
            }

            @Override
            public String namespaceV2() {
                return ""; // 原生默认值
            }

            @Override
            public int delayLevelWhenNextConsume() {
                return 0; // 原生默认值
            }

            @Override
            public int suspendCurrentQueueTimeMillis() {
                // 若自定义注解指定了 retryInterval 且不为 -1，则使用自定义值；否则用原生默认值 1000
                return customAnnotation.retryInterval() != -1
                        ? (int) customAnnotation.retryInterval()
                        : 1000;
            }

            @Override
            public int awaitTerminationMillisWhenShutdown() {
                return 1000; // 原生默认值
            }

            @Override
            public String instanceName() {
                return ""; // 原生默认值
            }
        };
    }
}
