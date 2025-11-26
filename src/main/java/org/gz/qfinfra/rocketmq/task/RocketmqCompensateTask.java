package org.gz.qfinfra.rocketmq.task;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.gz.qfinfra.rocketmq.config.RocketmqProperties;
import org.gz.qfinfra.rocketmq.consumer.aop.RocketmqConsumerAop;
import org.gz.qfinfra.rocketmq.entity.RocketmqFailMessage;
import org.gz.qfinfra.rocketmq.service.RocketmqFailMessageService;

import org.springframework.scheduling.annotation.Scheduled;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author guozhong
 * RocketMQ失败消息补偿任务
 */
@Slf4j
public class RocketmqCompensateTask {

    private Map<String, RocketMQListener> topicHandlerMapping;
    private RocketmqFailMessageService failMessageService;
    private RocketmqProperties rocketmqProperties;

    public RocketmqCompensateTask(RocketmqConsumerAop aop,
                                  RocketmqFailMessageService failMessageService,
                                  RocketmqProperties rocketmqProperties) {
        this.topicHandlerMapping = aop.topicHandlerMapping;
        this.failMessageService = failMessageService;
        this.rocketmqProperties = rocketmqProperties;
    }



    @Scheduled(cron = "0/30 * * * * ?")
    public void executeCompensate() {
        if (topicHandlerMapping.isEmpty()) {
            log.debug("没有找到任何补偿Topic处理器，跳过补偿任务");
            return;
        }
        //获取需要补偿的消息
        List<RocketmqFailMessage> pendingMessages = failMessageService.listWaitCompensateMessage(rocketmqProperties.getBatchSize());
        if (pendingMessages.isEmpty()) {
            log.debug("没有待补偿的消息");
            return;
        }

        log.info("开始补偿任务，待补偿消息数量: {}", pendingMessages.size());
        int successCount = 0;
        int failureCount = 0;
        for (RocketmqFailMessage message : pendingMessages) {
            if (processMessage(message)) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        log.info("补偿任务完成，成功: {}，失败: {}，总计: {}", successCount, failureCount, pendingMessages.size());
    }

    private boolean processMessage(RocketmqFailMessage message) {
        String topic = message.getMsgTopic();
        RocketMQListener handler = topicHandlerMapping.get(topic);

        if (handler == null) {
            log.warn("未找到Topic [{}] 对应的补偿处理器，消息ID: {}", topic, message.getMsgId());
            markFailHandler(message,"未找到对应的补偿处理器");
            return false;
        }

            log.debug("开始补偿消息，Topic: [{}], 消息ID: {}, 当前重试次数: {}",
                    topic, message.getMsgId(), message.getRetryCount());
            MessageExt msg = new MessageExt();
            try{
                byte[] bodyBytes = StringUtils.isNotBlank(message.getMsgBody()) ?
                        message.getMsgBody().getBytes(StandardCharsets.UTF_8) : new byte[0];
                msg.setBody(bodyBytes);
                msg.setMsgId(message.getMsgId());
                msg.setTopic(topic);
                msg.setKeys(message.getMsgKeys());
                msg.setTags(message.getMsgTags());
                //业务处理
                handler.onMessage(msg);
                markAsSuccess(message);
                log.info("消息补偿成功，Topic: [{}], 消息ID: {}", topic, message.getMsgId());
                return true;
            } catch (Exception e){
                markFailHandler(message, "业务处理返回失败");
                log.warn("消息补偿业务处理失败，Topic: [{}], 消息ID: {}", topic, message.getMsgId());
                return false;
            }


    }

    private void markAsSuccess(RocketmqFailMessage message) {
        message.setStatus(1);
        message.setFailReason(null);
        message.setRetryCount(message.getRetryCount() + 1);
        message.setUpdateTime(LocalDateTime.now());
        failMessageService.updateMessageById(message);
    }

    private void markFailHandler(RocketmqFailMessage message,String failReason) {
        //补偿失败
        message.setStatus(2);
        message.setFailReason(failReason);
        message.setRetryCount(message.getRetryCount() + 1);
        message.setUpdateTime(LocalDateTime.now());
        failMessageService.updateMessageById(message);
    }

}
