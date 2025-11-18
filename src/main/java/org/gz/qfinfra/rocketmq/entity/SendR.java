package org.gz.qfinfra.rocketmq.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author guozhong
 * @date 2025/11/18
 * @description 发送结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendR {
    // 消息ID
    private String messageId;
    // 主题
    private String topic;
    // 标签
    private String tag;
    // 发送时间戳（毫秒）
    private long sendTime;
}
