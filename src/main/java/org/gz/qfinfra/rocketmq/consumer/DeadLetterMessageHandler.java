package org.gz.qfinfra.rocketmq.consumer;

import org.apache.rocketmq.common.message.MessageExt;

/**
 * @author guozhong
 * @date 2025/11/28
 * @description 死信队列
 */
public interface DeadLetterMessageHandler {
    /**
     * 判断是否支持处理该消息
     */
    boolean supports(MessageExt message);

    /**
     * 处理单条死信消息
     */
    boolean handleMessage(MessageExt message);
}
