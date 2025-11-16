package org.gz.qfinfra.rocketmq.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gz.qfinfra.rocketmq.callback.MessageSendCallback;
import org.gz.qfinfra.rocketmq.config.QfRocketMQProperties;
import org.gz.qfinfra.rocketmq.entity.RocketmqFailMessage;
import org.gz.qfinfra.rocketmq.producer.QfRocketMQProducer;
import org.gz.qfinfra.rocketmq.repository.RocketmqFailMessageMapper;
import org.gz.qfinfra.rocketmq.util.JsonUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class CompensateFailMessageTask {
    private final QfRocketMQProducer rocketMQProducer;
    private final QfRocketMQProperties.Compensate compensateConfig;

    private final RocketmqFailMessageMapper failMessageMapper;
    /**
     * 定时补偿失败消息（按配置的 cron 表达式执行）
     */
    @Scheduled(cron = "${qf.infra.rocketmq.compensate.cron:0 0/5 * * * ?}")
    @Transactional(rollbackFor = Exception.class)
    public void compensateFailMessage() {
        log.info("[消息补偿] 开始执行补偿任务，批次大小={}", compensateConfig.getBatchSize());

        // 查询待补偿消息：状态0（待补偿）、已重试次数 < 最大重试次数
        LambdaQueryWrapper<RocketmqFailMessage> queryWrapper = new LambdaQueryWrapper<RocketmqFailMessage>()
                .eq(RocketmqFailMessage::getStatus, 0)
                .lt(RocketmqFailMessage::getRetryCount, 5)
                .last("LIMIT " + compensateConfig.getBatchSize());

        List<RocketmqFailMessage> failMessages = failMessageMapper.selectList(queryWrapper);
        log.info("[消息补偿] 待补偿消息数量：{}", failMessages.size());

        for (RocketmqFailMessage failMsg : failMessages) {
            try {
                // 更新状态为“补偿中”
                failMsg.setStatus(1);
                failMsg.setRetryCount(failMsg.getRetryCount() + 1);
                failMessageMapper.updateById(failMsg);

                // 构建主题（topic:tag）
                String topic = failMsg.getTopic() + (failMsg.getTag().isEmpty() ? "" : ":" + failMsg.getTag());
                Object messageBody = JsonUtil.fromJson(failMsg.getMessageBody(), Object.class);

                // 区分顺序消息和普通消息补偿
                if (failMsg.getShardingKey() != null && !failMsg.getShardingKey().isEmpty()) {
                    // 顺序消息补偿
                    rocketMQProducer.sendOrderly(topic, messageBody, failMsg.getShardingKey(), new MessageSendCallback() {
                        @Override
                        public void onSuccess(MessageSendCallback.SendResult result) {
                            updateMessageStatus(failMsg.getId(), 2, "补偿成功");
                            log.info("[消息补偿] 顺序消息补偿成功，id={}, messageId={}", failMsg.getId(), failMsg.getMessageId());
                        }

                        @Override
                        public void onFailure(Throwable ex) {
                            updateMessageStatus(failMsg.getId(), 0, ex.getMessage());
                            log.error("[消息补偿] 顺序消息补偿失败，id={}, messageId={}", failMsg.getId(), failMsg.getMessageId(), ex);
                        }
                    });
                } else {
                    // 普通消息补偿（同步发送）
                    rocketMQProducer.sendSync(topic, messageBody, new MessageSendCallback() {
                        @Override
                        public void onSuccess(SendResult result) {
                            updateMessageStatus(failMsg.getId(), 2, "补偿成功");
                            log.info("[消息补偿] 普通消息补偿成功，id={}, messageId={}", failMsg.getId(), failMsg.getMessageId());
                        }

                        @Override
                        public void onFailure(Throwable ex) {
                            updateMessageStatus(failMsg.getId(), 0, ex.getMessage());
                            log.error("[消息补偿] 普通消息补偿失败，id={}, messageId={}", failMsg.getId(), failMsg.getMessageId(), ex);
                        }
                    });
                }
            } catch (Exception e) {
                log.error("[消息补偿] 处理失败消息异常，id={}", failMsg.getId(), e);
                // 异常时恢复为“待补偿”状态
                updateMessageStatus(failMsg.getId(), 0, e.getMessage());
            }
        }

        log.info("[消息补偿] 本次补偿任务执行完毕");
    }

    /**
     * 更新消息状态
     */
    @Transactional(rollbackFor = Exception.class)
    void updateMessageStatus(Long id, Integer status, String errorMsg) {
        RocketmqFailMessage updateMsg = new RocketmqFailMessage();
        updateMsg.setId(id);
        updateMsg.setStatus(status);
        updateMsg.setErrorMsg(errorMsg);
        updateMsg.setUpdateTime(java.time.LocalDateTime.now());
        failMessageMapper.updateById(updateMsg);
    }
}
