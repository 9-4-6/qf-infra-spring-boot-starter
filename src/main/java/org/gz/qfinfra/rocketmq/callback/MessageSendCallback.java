package org.gz.qfinfra.rocketmq.callback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息发送回调接口（对外暴露，统一成功/失败处理）
 */
public interface  MessageSendCallback {

    /**
     * 发送成功回调
     * @param result 成功结果（消息ID、主题等）
     */
    void onSuccess(SendResult result);

    /**
     * 发送失败回调
     * @param ex 失败异常
     */
    void onFailure(Throwable ex);

    /**
     * 发送结果封装
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class SendResult {
        private String messageId; // 消息ID
        private String topic;     // 主题
        private String tag;       // 标签
        private long sendTime;    // 发送时间戳（毫秒）
    }
}
