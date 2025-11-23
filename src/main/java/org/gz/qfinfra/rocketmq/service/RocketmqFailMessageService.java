package org.gz.qfinfra.rocketmq.service;

import org.gz.qfinfra.rocketmq.entity.RocketmqFailMessage;

import java.util.List;

public interface RocketmqFailMessageService {
    /**
     * 保存失败消息
     */
    void saveFailMessage(RocketmqFailMessage failMessage);

    /**
     * 查询待补偿的消息
     */
    List<RocketmqFailMessage> listWaitCompensateMessage();

    /**
     * 更新补偿状态
     */
    boolean updateCompensateStatus(Long id, Integer status, Integer retryCount);
}
