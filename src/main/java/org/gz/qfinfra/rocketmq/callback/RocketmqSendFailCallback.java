package org.gz.qfinfra.rocketmq.callback;

import org.apache.rocketmq.client.producer.SendResult;

/**
 * 消息发送回调接口（对外暴露，失败处理）
 * @author guozhong
 */
@FunctionalInterface
public interface RocketmqSendFailCallback {
    /**
     * 失败回调方法
     * @param msg 消息体
     * @param topic 主题
     * @param tags 标签
     * @param keys 消息Key
     * @param e 异常
     * @param sendResult 发送结果（可能为null）
     */
    void onFail(Object msg, String topic, String tags, String keys, Exception e, SendResult sendResult);

}
