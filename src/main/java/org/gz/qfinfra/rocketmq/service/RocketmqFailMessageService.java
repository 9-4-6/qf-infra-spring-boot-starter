package org.gz.qfinfra.rocketmq.service;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.gz.qfinfra.rocketmq.entity.RocketmqFailMessage;
import org.gz.qfinfra.rocketmq.repository.RocketmqFailMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author 17853
 */
@Slf4j
@Service
public class RocketmqFailMessageService {
    @Resource
    private  RocketmqFailMessageMapper failMessageMapper;


    /**
     * 保存失败消息到 MySQL
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveFailMessage(RocketmqFailMessage failMessage) {
        failMessageMapper.insert(failMessage);
        log.info("[失败消息持久化] 成功，id={}, messageId={}, topic={}",
                failMessage.getId(), failMessage.getMessageId(), failMessage.getTopic());
    }



}
