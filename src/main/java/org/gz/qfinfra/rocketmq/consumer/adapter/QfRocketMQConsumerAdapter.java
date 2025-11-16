package org.gz.qfinfra.rocketmq.consumer.adapter;

import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.gz.qfinfra.rocketmq.consumer.annotation.QfRocketMQMessageListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

@Component
public abstract class QfRocketMQConsumerAdapter<T> implements RocketMQListener<T> {
    /**
     * 手动构建原生注解（补全所有抽象方法）
     */
    public RocketMQMessageListener getRocketMQNativeAnnotation() {
        // 1. 获取自定义注解
        QfRocketMQMessageListener customAnnotation = AnnotationUtils.findAnnotation(
                getClass(), QfRocketMQMessageListener.class
        );
        if (customAnnotation == null) {
            throw new IllegalArgumentException("消费者必须添加 @QfRocketMQMessageListener 注解");
        }

        // 2. 实现所有抽象方法（按原生默认值+自定义注解映射）
        return new RocketMQMessageListener() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return RocketMQMessageListener.class;
            }

            // -------------------------- 核心属性映射（自定义注解 → 原生注解）--------------------------
            @Override
            public String topic() {
                return customAnnotation.topic();
            }

            @Override
            public String consumerGroup() {
                return customAnnotation.consumerGroup();
            }

            @Override
            public ConsumeMode consumeMode() {
                return customAnnotation.consumeMode();
            }

            @Override
            public MessageModel messageModel() {
                return customAnnotation.messageModel();
            }

            @Override
            public SelectorType selectorType() {
                return customAnnotation.selectorType();
            }

            @Override
            public String selectorExpression() {
                return customAnnotation.selectorExpression();
            }


            // -------------------------- 原生注解默认值方法（补充截图中缺失的所有方法）--------------------------
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
            public int maxReconsumeTimes() {
                return -1; // 禁用原生重试
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

            // -------------------------- 截图中新增的方法（按原生默认值实现）--------------------------
            @Override
            public int replyTimeout() {
                return 3000; // 原生默认值（3秒）
            }

            @Override
            public String nameServer() {
                return ""; // 同 namesrvAddr，原生默认值
            }

            @Override
            public String accessChannel() {
                return "LOCAL"; // 原生默认值（本地通道）
            }

            @Override
            public String tlsEnable() {
                return "false"; // 原生默认值（禁用TLS）
            }

            @Override
            public String namespaceV2() {
                return ""; // 原生默认值
            }

            @Override
            public int delayLevelWhenNextConsume() {
                return 0; // 原生默认值（不延迟）
            }

            @Override
            public int suspendCurrentQueueTimeMillis() {
                return 1000; // 原生默认值（暂停1秒）
            }

            @Override
            public int awaitTerminationMillisWhenShutdown() {
                return 1000; // 原生默认值（关闭等待1秒）
            }

            @Override
            public String instanceName() {
                return ""; // 原生默认值（自动生成实例名）
            }
        };
    }
}
