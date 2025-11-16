package org.gz.qfinfra.rocketmq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rocketmq_fail_message")
public class RocketmqFailMessage {
    @TableId(type = IdType.AUTO)
    private Long id;                // 主键ID

    private String messageId;       // RocketMQ 消息ID

    private String topic;           // 消息主题

    private String tag;             // 消息标签

    private String messageBody;     // 消息体（JSON格式）

    private String shardingKey;     // 顺序消息分片键（可为空）

    private Integer retryCount;     // 已重试次数

    private Integer maxRetryCount;  // 最大重试次数

    private Integer status;         // 状态：0-待补偿，1-补偿中，2-已成功，3-已废弃

    private String errorMsg;        // 失败原因

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime updateTime; // 更新时间
}
