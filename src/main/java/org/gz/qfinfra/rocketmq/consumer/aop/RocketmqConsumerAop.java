package org.gz.qfinfra.rocketmq.consumer.aop;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.gz.qfinfra.rocketmq.entity.RocketmqFailMessage;
import org.gz.qfinfra.rocketmq.service.RocketmqFailMessageService;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author guozhong
 */
@Slf4j
@Aspect
public class RocketmqConsumerAop {
    public final Map<String, RocketMQListener> topicHandlerMapping = new ConcurrentHashMap<>();
    private RocketmqFailMessageService failMessageService;


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


        // 获取消费者实例和 RocketMQMessageListener 注解
        Object handler = joinPoint.getTarget();
        RocketMQMessageListener listenerAnnotation = handler.getClass().getAnnotation(RocketMQMessageListener.class);
        if (listenerAnnotation == null) {
            return joinPoint.proceed();
        }

        //获取最大重试次数
        int maxReconsumeTimes = listenerAnnotation.maxReconsumeTimes();
        int consumerMaxRetryTimes = maxReconsumeTimes == -1 ? 16 : maxReconsumeTimes;
        Object message = args[0];
        if(!(message instanceof MessageExt)){
           //非此类型不支持补偿
            return joinPoint.proceed();
        }
        // 解析当前重试次数
        int currentReconsumeTimes = ((MessageExt) message).getReconsumeTimes();
        try {
            result = joinPoint.proceed(); // 执行消费方法
        } catch (Exception e) {
           String topic =((MessageExt) message).getTopic();
            log.error("RocketMQ消费失败，消息主题:{},当前重试次数:{},最大重试次数:{}", topic,currentReconsumeTimes,consumerMaxRetryTimes);
            // 等于最大重试次数，存储失败消息,不再抛异常
            if (currentReconsumeTimes == consumerMaxRetryTimes) {
                log.info("RocketMQ消费重试次数超限，已存储失败消息到数据库，主题：{}", topic);
                saveFailMessage(message,e);
                topicHandlerMapping.put(topic, (RocketMQListener) handler);
                log.info("注册Topic处理器映射: topic=[{}] -> handler=[{}]",
                        topic, handler.getClass().getSimpleName());
                return result;
            } else {
                // 未超限则抛出异常，让RocketMQ重试
                throw e;
            }
        }
        return result;
    }


    /**
     * 保存失败消息到数据库
     */
    private void saveFailMessage(Object message, Exception e) {
        RocketmqFailMessage failMessage = new RocketmqFailMessage();

        String topic = ((MessageExt) message).getTopic();
        String tags = ((MessageExt) message).getTags();
        byte[] body = ((MessageExt) message).getBody();
        String msgId = ((MessageExt) message).getMsgId();
        String keys = ((MessageExt) message).getKeys();
        failMessage.setMsgTopic(topic);
        failMessage.setMsgTags(tags);
        failMessage.setMsgBody(new String(body, StandardCharsets.UTF_8));
        //消息id
        failMessage.setMsgId(msgId);
        failMessage.setMsgKeys(keys);
        failMessage.setFailReason(e.getMessage());
        failMessageService.saveFailMessage(failMessage);

    }

}
