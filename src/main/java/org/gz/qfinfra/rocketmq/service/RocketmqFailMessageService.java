package org.gz.qfinfra.rocketmq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.gz.qfinfra.rocketmq.entity.RocketmqFailMessage;
import org.gz.qfinfra.rocketmq.repository.RocketmqFailMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RocketmqFailMessageService {
    private final RocketmqFailMessageMapper failMessageMapper;


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
