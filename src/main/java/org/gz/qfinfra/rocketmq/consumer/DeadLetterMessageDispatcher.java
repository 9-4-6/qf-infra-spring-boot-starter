package org.gz.qfinfra.rocketmq.consumer;


import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.ArrayList;
import java.util.List;

/**
 * @author guozhong
 * @date 2025/11/28
 * @description 死信队列分发其
 */
@Slf4j
public class DeadLetterMessageDispatcher {

    private final List<DeadLetterMessageHandler> handlers;

    public DeadLetterMessageDispatcher(List<DeadLetterMessageHandler> handlers) {
        this.handlers = handlers != null ? handlers : new ArrayList<>();
        log.info("DeadLetterMessageDispatcher initialized with {} handlers", this.handlers.size());
    }

    public ConsumeConcurrentlyStatus handleMessages(List<MessageExt> messages) {
        boolean hasFailure = false;
        for (MessageExt message : messages) {
            try {
                boolean processed = false;
                // 让所有处理器尝试处理
                for (DeadLetterMessageHandler handler : handlers) {
                    if (handler.supports(message)) {
                        boolean success = handler.handleMessage(message);
                        if (success) {
                            processed = true;
                            break; // 一个处理器成功就跳出
                        }
                    }
                }

                if (!processed) {
                    log.warn("No handler processed dead letter message: {}", message.getMsgId());
                    // 使用默认处理
                    defaultHandleMessage(message);
                }

            } catch (Exception e) {
                log.error("Process dead letter message failed: {}", message.getMsgId(), e);
                hasFailure = true;
            }
        }

        return hasFailure ? ConsumeConcurrentlyStatus.RECONSUME_LATER :
                ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private void defaultHandleMessage(MessageExt message) {
        // 默认处理：记录日志
        String consumerGroup = extractConsumerGroup(message.getTopic());
        log.warn("Default dead letter handling - Group: {}, MsgId: {}",
                consumerGroup, message.getMsgId());
    }

    private String extractConsumerGroup(String dlqTopic) {
        return dlqTopic.replace("%DLQ%", "");
    }
}
