package org.gz.qfinfra.rocketmq.consumer.aop;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.gz.qfinfra.rocketmq.entity.RocketmqFailMessage;
import org.gz.qfinfra.rocketmq.service.RocketmqFailMessageService;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * @author guozhong
 */
@Slf4j
@Aspect
public class RocketmqConsumerAop {
    private Integer consumerMaxRetryTimes;
    private RocketmqFailMessageService failMessageService;

    // 通过setter注入属性和服务
    public void setConsumerMaxRetryTimes(Integer consumerMaxRetryTimes) {
        this.consumerMaxRetryTimes = consumerMaxRetryTimes;
    }

    public void setFailMessageService(RocketmqFailMessageService failMessageService) {
        this.failMessageService = failMessageService;
    }

    /**
     * 切入点：拦截所有RocketMQListener的onMessage方法
     */
    @Pointcut("execution(* org.apache.rocketmq.spring.core.RocketMQListener.onMessage(..))")
    public void consumerPointcut() {
    }

    /**
     * 环绕通知：捕获消费异常，判断重试次数
     */
    @Around("consumerPointcut()")
    public Object aroundConsumer(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = null;
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return joinPoint.proceed();
        }

        // 获取消费者的RocketMQMessageListener注解
        Class<?> targetClass = joinPoint.getTarget().getClass();
        RocketMQMessageListener listenerAnnotation = targetClass.getAnnotation(RocketMQMessageListener.class);
        if (listenerAnnotation == null) {
            return joinPoint.proceed();
        }

        // 解析当前重试次数
        Object message = args[0];
        int currentRetryTimes = getCurrentRetryTimes(message);
        try {
            result = joinPoint.proceed(); // 执行消费方法
        } catch (Exception e) {
            log.error("RocketMQ消费失败，当前重试次数：{}，异常：", currentRetryTimes, e);
            // 超过最大重试次数，存储失败消息
            if (currentRetryTimes >= consumerMaxRetryTimes) {
                saveFailMessage(listenerAnnotation, message, currentRetryTimes);
                log.info("RocketMQ消费重试次数超限，已存储失败消息到数据库，主题：{}", listenerAnnotation.topic());
            } else {
                throw e; // 未超限则抛出异常，让RocketMQ重试
            }
        }
        return result;
    }

    /**
     * 解析消息的重试次数（从MessageExt的__RETRY_TIMES__属性获取）
     */
    private int getCurrentRetryTimes(Object message) {
        try {
            if (message.getClass().getName().contains("MessageExt")) {
                Method getPropertyMethod = message.getClass().getMethod("getProperty", String.class);
                String retryTimes = (String) getPropertyMethod.invoke(message, "__RETRY_TIMES__");
                return StringUtils.hasText(retryTimes) ? Integer.parseInt(retryTimes) : 0;
            }
        } catch (Exception e) {
            log.error("解析消息重试次数失败", e);
        }
        return 0;
    }

    /**
     * 保存失败消息到数据库
     */
    private void saveFailMessage(RocketMQMessageListener annotation, Object message, int retryTimes) {
        RocketmqFailMessage failMessage = new RocketmqFailMessage();
        failMessage.setMsgTopic(annotation.topic());
        failMessage.setMsgTags(annotation.selectorExpression());

        // 解析消息体和消息ID
        if (message.getClass().getName().contains("MessageExt")) {
            try {
                Method getBodyMethod = message.getClass().getMethod("getBody");
                byte[] body = (byte[]) getBodyMethod.invoke(message);
                failMessage.setMsgBody(new String(body));

                Method getMsgIdMethod = message.getClass().getMethod("getMsgId");
                String msgId = (String) getMsgIdMethod.invoke(message);
                failMessage.setMsgId(msgId);

                Method getKeysMethod = message.getClass().getMethod("getKeys");
                String keys = (String) getKeysMethod.invoke(message);
                failMessage.setMsgKeys(keys);
            } catch (Exception e) {
                log.error("解析消息属性失败", e);
                failMessage.setMsgBody(JSONUtil.toJsonStr(message));
            }
        } else {
            failMessage.setMsgBody(JSONUtil.toJsonStr(message));
        }

        failMessageService.saveFailMessage(failMessage);
    }

}
