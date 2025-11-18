package org.gz.qfinfra.rocketmq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author 17853
 */
@Data
@TableName("rocketmq_fail_message")
public class RocketmqFailMessage {
    // 主键ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // RocketMQ 消息ID
    private String messageId;
    // 消息主题
    private String topic;
    // 消息标签
    private String tag;
    // 消息体（JSON格式）
    private String messageBody;
    // 顺序消息分片键（可为空）
    private String shardingKey;
    // 已重试次数
    private Integer retryCount;
    // 最大重试次数
    private Integer maxRetryCount;
    // 状态：0-待补偿，1-补偿中，2-已成功，3-已废弃
    private Integer status;
    // 失败原因
    private String errorMsg;
    // 创建时间
    private LocalDateTime createTime;
    // 更新时间
    private LocalDateTime updateTime;
}
