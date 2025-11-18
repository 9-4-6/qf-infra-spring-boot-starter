package org.gz.qfinfra.rocketmq.producer;


import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.gz.qfinfra.rocketmq.callback.MessageSendCallback;
import org.gz.qfinfra.rocketmq.config.QfRocketMqProperties;
import org.gz.qfinfra.rocketmq.entity.SendR;
import org.gz.qfinfra.rocketmq.util.JsonUtil;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.messaging.Message;


/**
 * @author guozhong
 */
@Slf4j
@Component
public class QfRocketMqProducer {
    @Resource
    private  RocketMQTemplate rocketMqTemplate;

    private final QfRocketMqProperties qfRocketMqProperties;
    public QfRocketMqProducer(QfRocketMqProperties properties) {
        this.qfRocketMqProperties = properties;
    }

    /**
     * 同步发送消息（阻塞等待结果）
     * @param topic 主题（格式：topic:tag）
     * @param message 消息体（任意对象）
     * @param callback 发送回调
     */
    public void sendSync(String topic, Object message, MessageSendCallback callback) {
        try {
            log.info("[同步发送] 开始发送，topic={}, message={}", topic, JsonUtil.toJson(message));
            Message<?> springMessage = buildMessage(message);

            // 调用 RocketMQ 同步发送（使用配置的超时时间）
            SendResult sendResult = rocketMqTemplate.syncSend(
                    topic, springMessage, qfRocketMqProperties.getProducerSendTimeout(),
                    qfRocketMqProperties.getProducerRetryTimes()
            );

            // 封装结果并回调
            SendR result = buildSendResult(topic, sendResult);
            callback.onSuccess(result);
            log.info("[同步发送] 成功，result={}", JsonUtil.toJson(result));
        } catch (Throwable ex) {
            log.error("[同步发送] 失败，topic={}, message={}", topic, JsonUtil.toJson(message), ex);
            callback.onFailure(ex);
        }
    }

    /**
     * 异步发送消息（非阻塞）
     * @param topic 主题（格式：topic:tag）
     * @param message 消息体（任意对象）
     * @param callback 发送回调
     */
    public void sendAsync(String topic, Object message, MessageSendCallback callback) {
            try {
                log.info("[异步发送] 开始发送，topic={}, message={}", topic, JsonUtil.toJson(message));
                Message<?> springMessage = buildMessage(message);

                // 调用 RocketMQ 异步发送
                rocketMqTemplate.asyncSend(topic, springMessage, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        SendR result = buildSendResult(topic, sendResult);
                        callback.onSuccess(result);
                        log.info("[异步发送] 成功，result={}", JsonUtil.toJson(result));
                    }

                    @Override
                    public void onException(Throwable ex) {
                        log.error("[异步发送] 失败，topic={}, message={}", topic, JsonUtil.toJson(message), ex);
                        callback.onFailure(ex);
                    }
                }, qfRocketMqProperties.getProducerSendTimeout(),
                        qfRocketMqProperties.getProducerRetryTimes());
            } catch (Throwable ex) {
                log.error("[异步发送] 提交失败，topic={}, message={}", topic, JsonUtil.toJson(message), ex);
                callback.onFailure(ex);
            }

    }

    /**
     * 顺序发送消息（同一分片键保证顺序）
     * @param topic 主题（格式：topic:tag）
     * @param message 消息体（任意对象）
     * @param shardingKey 分片键（同一key进入同一队列）
     * @param callback 发送回调
     */
    public void sendOrderly(String topic, Object message, String shardingKey, MessageSendCallback callback) {
        try {
            log.info("[顺序发送] 开始发送，topic={}, shardingKey={}, message={}",
                    topic, shardingKey, JsonUtil.toJson(message));
            Message<?> springMessage = buildMessage(message);

            // 调用 RocketMQ 顺序发送
            SendResult sendResult = rocketMqTemplate.syncSendOrderly(
                    topic, springMessage, shardingKey, qfRocketMqProperties.getProducerSendTimeout());

            SendR result = buildSendResult(topic, sendResult);
            callback.onSuccess(result);
            log.info("[顺序发送] 成功，result={}", JsonUtil.toJson(result));
        } catch (Throwable ex) {
            log.error("[顺序发送] 失败，topic={}, shardingKey={}, message={}",
                    topic, shardingKey, JsonUtil.toJson(message), ex);
            callback.onFailure(ex);
        }
    }

    /**
     * 构建 Spring Message（统一JSON序列化）
     */
    private Message<?> buildMessage(Object message) {
        return MessageBuilder.withPayload(JsonUtil.toJson(message))
                .setHeader("content-type", "application/json")
                .build();
    }

    /**
     * 封装发送结果
     */
    private SendR buildSendResult(String topic, SendResult sendResult) {
        String[] topicTag = topic.split(":");
        String pureTopic = topicTag[0];
        String tag = topicTag.length > 1 ? topicTag[1] : "";

        return new SendR(
                sendResult.getMsgId(),
                pureTopic,
                tag,
                System.currentTimeMillis()
        );
    }
}
