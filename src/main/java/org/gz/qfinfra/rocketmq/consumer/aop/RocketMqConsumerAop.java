package org.gz.qfinfra.rocketmq.consumer.aop;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.gz.qfinfra.rocketmq.config.QfRocketMqProperties;
import org.gz.qfinfra.rocketmq.consumer.annotation.QfRocketMqMessageListener;
import org.gz.qfinfra.rocketmq.entity.RocketmqFailMessage;
import org.gz.qfinfra.rocketmq.service.RocketmqFailMessageService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author guozhong
 */
@Slf4j
@Aspect
public class RocketMqConsumerAop {
    @Resource
    private  RocketmqFailMessageService failMessageService;
    private final QfRocketMqProperties qfRocketMqProperties;
    public RocketMqConsumerAop(QfRocketMqProperties properties) {
        this.qfRocketMqProperties = properties;
    }

    /**
     * 环绕增强：拦截类上标注 @QfRocketMQMessageListener 且方法名为 onMessage 的方法
     * （因为注解 @Target 是 TYPE，所以需要用 @within 匹配类上的注解）
     */
    @Around("@within(com.qf.infra.rocketmq.consumer.annotation.QfRocketMQMessageListener)" +
            " && execution(* onMessage(..))")
    public Object aroundConsumer(ProceedingJoinPoint joinPoint) throws Throwable {

        // 关键修改：从目标类上获取注解（因为注解修饰的是类）
        QfRocketMqMessageListener annotation = joinPoint.getTarget().getClass()
                .getAnnotation(QfRocketMqMessageListener.class);

        if (annotation == null) {
            log.warn("[消费者AOP] 未找到 @QfRocketMQMessageListener 注解，直接执行原方法");
            return joinPoint.proceed();
        }

        // 1. 解析配置（注解配置优先，无则用全局配置）最大重试次数
        int maxRetryTimes = annotation.maxRetryTimes() > 0 ?
                annotation.maxRetryTimes() : qfRocketMqProperties.getConsumerMaxRetryTimes();
        // 2.重试次数
        long retryInterval = annotation.retryInterval() > 0 ?
                annotation.retryInterval() : qfRocketMqProperties.getProducerRetryTimes();

        // 3. 解析消息参数（RocketMQListener 的 onMessage 方法参数通常是 T 或 MessageExt）
        Object[] args = joinPoint.getArgs();
        List<MessageExt> messageExtList = parseMessageArgs(args);
        if (messageExtList == null || messageExtList.isEmpty()) {
            log.warn("[消费者] 无有效消息，topic={}", annotation.topic());
            return buildDefaultConsumeResult(annotation);
        }

        // 3. 打印消费日志
        log.info("[消费者] 开始消费，topic={}, tag={}, consumerGroup={}, 消息数量={}, 消息详情={}",
                annotation.topic(), annotation.tag(), annotation.consumerGroup(),
                messageExtList.size(), JSONUtil.toJsonStr(messageExtList.stream().map(MessageExt::getBody).toList()));

        // 4. 重试逻辑
        int currentRetry = 0;
        Throwable finalEx = null;
        while (currentRetry < maxRetryTimes) {
            try {
                // 执行用户自定义消费逻辑（onMessage 方法）
                Object result = joinPoint.proceed();
                log.info("[消费者] 消费成功，topic={}, 重试次数={}, 结果={}",
                        annotation.topic(), currentRetry, result);
                return result;
            } catch (Throwable ex) {
                currentRetry++;
                finalEx = ex;
                log.error("[消费者] 消费失败，topic={}, 重试次数={}/{}",
                        annotation.topic(), currentRetry, maxRetryTimes, ex);

                // 未达到最大重试次数，等待后重试
                if (currentRetry < maxRetryTimes) {
                    Thread.sleep(retryInterval);
                }
            }
        }

        // 5. 重试失败：持久化到 MySQL 待补偿
        log.error("[消费者] 重试{}次后仍失败，存入MySQL，topic={}",
                maxRetryTimes, annotation.topic());
        saveFailMessages(annotation, messageExtList, finalEx);

        // 6. 返回消费结果（标记为成功，避免 RocketMQ 重复投递）
        return buildDefaultConsumeResult(annotation);
    }

    /**
     * 解析消费方法参数（支持 MessageExt 或业务对象，这里从消息中提取 MessageExt）
     * 注意：如果 onMessage 入参是业务对象，需要从消息属性中获取 MessageExt（可通过拦截器提前注入）
     */
    private List<MessageExt> parseMessageArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        // 情况1：参数直接是 MessageExt
        if (args[0] instanceof MessageExt) {
            return List.of((MessageExt) args[0]);
        }
        // 情况2：参数是业务对象，但需要提前通过拦截器将 MessageExt 存入 ThreadLocal 或方法参数中
        // 这里假设已通过其他方式获取到 MessageExt（需根据实际消费逻辑补充）
        // 示例：从 ThreadLocal 中获取（需在消息反序列化后设置）
        MessageExt threadLocalMessage = MessageExtHolder.get();
        if (threadLocalMessage != null) {
            return List.of(threadLocalMessage);
        }
        return null;
    }

    /**
     * 保存失败消息到 MySQL
     */
    private void saveFailMessages(QfRocketMqMessageListener annotation, List<MessageExt> messageExtList, Throwable ex) {
        for (MessageExt messageExt : messageExtList) {
            RocketmqFailMessage failMessage = new RocketmqFailMessage();
            failMessage.setMessageId(messageExt.getMsgId());
            failMessage.setTopic(annotation.topic());
            failMessage.setTag(annotation.tag());
            failMessage.setMessageBody(new String(messageExt.getBody()));
            failMessage.setShardingKey(messageExt.getKeys());
            failMessage.setRetryCount(annotation.maxRetryTimes() > 0 ? annotation.maxRetryTimes() : qfRocketMqProperties.getConsumerMaxRetryTimes());
            failMessage.setMaxRetryCount(failMessage.getRetryCount());
            // 0-待补偿
            failMessage.setStatus(0);
            failMessage.setErrorMsg(ex.getMessage());
            failMessage.setCreateTime(LocalDateTime.now());
            failMessage.setUpdateTime(LocalDateTime.now());

            failMessageService.saveFailMessage(failMessage);
        }
    }

    /**
     * 构建默认消费结果（根据消费模式返回对应状态）
     */
    private Object buildDefaultConsumeResult(QfRocketMqMessageListener annotation) {
        if (annotation.consumeMode() == ConsumeMode.ORDERLY) {
            return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
        } else {
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
    }

    // 可选：用于存储当前线程的 MessageExt（当 onMessage 入参是业务对象时）
    private static class MessageExtHolder {
        private static final ThreadLocal<MessageExt> HOLDER = new ThreadLocal<>();
        public static void set(MessageExt messageExt) {
            HOLDER.set(messageExt);
        }
        public static MessageExt get() {
            return HOLDER.get();
        }
        public static void remove() {
            HOLDER.remove();
        }
    }

}
