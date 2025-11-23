package org.gz.qfinfra.rocketmq.task;

import lombok.extern.slf4j.Slf4j;
import org.gz.qfinfra.rocketmq.callback.RocketmqSendFailCallback;
import org.gz.qfinfra.rocketmq.config.RocketmqProperties;
import org.gz.qfinfra.rocketmq.entity.RocketmqFailMessage;
import org.gz.qfinfra.rocketmq.producer.RocketmqProducer;
import org.gz.qfinfra.rocketmq.service.RocketmqFailMessageService;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
/**
 * @author guozhong
 * RocketMQ失败消息补偿任务
 */
@Slf4j
public class RocketmqCompensateTask {
    private RocketmqFailMessageService failMessageService;
    private RocketmqProducer rocketmqProducer;
    private String compensateCron;

    // 通过setter注入依赖和属性
    public void setFailMessageService(RocketmqFailMessageService failMessageService) {
        this.failMessageService = failMessageService;
    }

    public void setRocketmqProducer(RocketmqProducer rocketmqProducer) {
        this.rocketmqProducer = rocketmqProducer;
    }

    public void setRocketmqProperties(RocketmqProperties rocketmqProperties) {
        this.compensateCron = rocketmqProperties.getCompensateCron();
    }

    /**
     * 定时补偿任务（Cron表达式从配置中获取）
     */
    @Scheduled(cron = "${compensateCron:0/10 * * * * ?}")
    public void compensateFailMessage() {
        log.info("开始执行RocketMQ失败消息补偿任务");
        List<RocketmqFailMessage> failMessages = failMessageService.listWaitCompensateMessage();
        if (failMessages.isEmpty()) {
            log.info("暂无待补偿的失败消息");
            return;
        }

        for (RocketmqFailMessage failMessage : failMessages) {
            Long id = failMessage.getId();
            int currentRetryCount = failMessage.getRetryCount() + 1;
            try {
                // 重新发送消息
                rocketmqProducer.sendSyncMessage(
                        failMessage.getMsgTopic(),
                        failMessage.getMsgTags(),
                        failMessage.getMsgKeys(),
                        failMessage.getMsgBody(),
                        buildCompensateFailCallback(failMessage, currentRetryCount)
                );
                // 补偿成功，更新状态
                failMessageService.updateCompensateStatus(id, 1, currentRetryCount);
                log.info("失败消息补偿成功，ID：{}，主题：{}", id, failMessage.getMsgTopic());
            } catch (Exception e) {
                log.error("失败消息补偿异常，ID：{}，主题：{}", id, failMessage.getMsgTopic(), e);
                // 判断是否超过最大补偿次数
                if (currentRetryCount >= failMessage.getMaxRetryCount()) {
                    failMessageService.updateCompensateStatus(id, 2, currentRetryCount); // 补偿失败
                    log.warn("失败消息补偿次数超限，标记为补偿失败，ID：{}", id);
                } else {
                    failMessageService.updateCompensateStatus(id, 0, currentRetryCount); // 继续待补偿
                }
            }
        }
    }

    /**
     * 构建补偿发送的失败回调
     */
    private RocketmqSendFailCallback buildCompensateFailCallback(RocketmqFailMessage failMessage, int currentRetryCount) {
        return (msg, topic, tags, keys, e, sendResult) -> {
            Long id = failMessage.getId();
            if (currentRetryCount >= failMessage.getMaxRetryCount()) {
                failMessageService.updateCompensateStatus(id, 2, currentRetryCount);
                log.warn("补偿发送失败且次数超限，标记为补偿失败，ID：{}", id);
            } else {
                failMessageService.updateCompensateStatus(id, 0, currentRetryCount);
                log.info("补偿发送失败，继续待补偿，ID：{}", id);
            }
        };
    }
}
