package org.gz.qfinfra.rocketmq.producer;


import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.gz.qfinfra.rocketmq.callback.RocketmqSendFailCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;


/**
 * @author guozhong
 * RocketMQ生产者封装
 */
public class RocketmqProducer {
    private static final Logger log = LoggerFactory.getLogger(RocketmqProducer.class);

    private RocketMQTemplate rocketMQTemplate;

    // 通过setter注入RocketMQTemplate
    public void setRocketMQTemplate(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * 同步发送消息
     * @param topic 主题
     * @param tags 标签
     * @param keys 消息Key
     * @param msg 消息体
     * @param failCallback 失败回调
     */
    public <T> void sendSyncMessage(String topic, String tags, String keys, T msg, RocketmqSendFailCallback failCallback) {
        Assert.hasText(topic, "消息主题不能为空");
        Assert.notNull(msg, "消息体不能为空");

        String destination = buildDestination(topic, tags);
        try {
            Message<T> message = buildMessage(msg, keys);
            SendResult sendResult = rocketMQTemplate.syncSend(destination, message);
            if (sendResult.getSendStatus() != org.apache.rocketmq.client.producer.SendStatus.SEND_OK) {
                log.error("同步发送消息失败，主题：{}，标签：{}，Key：{}，结果：{}", topic, tags, keys, sendResult);
                failCallback.onFail(msg, topic, tags, keys, new RuntimeException("发送状态非SEND_OK"), sendResult);
            }
        } catch (Exception e) {
            log.error("同步发送消息异常，主题：{}，标签：{}，Key：{}", topic, tags, keys, e);
            failCallback.onFail(msg, topic, tags, keys, e, null);
        }
    }

    /**
     * 异步发送消息
     * @param topic 主题
     * @param tags 标签
     * @param keys 消息Key
     * @param msg 消息体
     * @param failCallback 失败回调
     */
    public <T> void sendAsyncMessage(String topic, String tags, String keys, T msg, RocketmqSendFailCallback failCallback) {
        Assert.hasText(topic, "消息主题不能为空");
        Assert.notNull(msg, "消息体不能为空");

        String destination = buildDestination(topic, tags);
        try {
            Message<T> message = buildMessage(msg, keys);
            rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    if (sendResult.getSendStatus() != org.apache.rocketmq.client.producer.SendStatus.SEND_OK) {
                        log.error("异步发送消息状态异常，主题：{}，标签：{}，Key：{}，结果：{}", topic, tags, keys, sendResult);
                        failCallback.onFail(msg, topic, tags, keys, new RuntimeException("发送状态非SEND_OK"), sendResult);
                    }
                }

                @Override
                public void onException(Throwable e) {
                    log.error("异步发送消息异常，主题：{}，标签：{}，Key：{}", topic, tags, keys, e);
                    failCallback.onFail(msg, topic, tags, keys, new Exception(e), null);
                }
            });
        } catch (Exception e) {
            log.error("异步发送消息初始化异常，主题：{}，标签：{}，Key：{}", topic, tags, keys, e);
            failCallback.onFail(msg, topic, tags, keys, e, null);
        }
    }

    /**
     * 构建消息目的地（topic:tags）
     */
    private String buildDestination(String topic, String tags) {
        return tags == null ? topic : topic + ":" + tags;
    }

    /**
     * 构建Spring Message对象（设置消息Key）
     */
    private <T> Message<T> buildMessage(T msg, String keys) {
        MessageBuilder<T> builder = MessageBuilder.withPayload(msg);
        if (keys != null) {
            builder.setHeader(RocketMQHeaders.KEYS, keys);
        }
        return builder.build();
    }
}
