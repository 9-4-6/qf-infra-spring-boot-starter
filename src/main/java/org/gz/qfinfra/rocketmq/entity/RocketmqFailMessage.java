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
    private Long id;
    private String msgId; // RocketMQ消息ID
    private String msgTopic; // 消息主题
    private String msgTags; // 消息标签
    private String msgKeys; // 消息Key
    private String msgBody; // 消息体
    private Integer retryCount; // 已补偿次数
    private Integer status; // 状态：0-待补偿 1-成功 2-失败
    private String failReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
