package org.gz.qfinfra.rocketmq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.gz.qfinfra.rocketmq.config.RocketmqProperties;
import org.gz.qfinfra.rocketmq.entity.RocketmqFailMessage;
import org.gz.qfinfra.rocketmq.mapper.RocketmqFailMessageMapper;
import org.gz.qfinfra.rocketmq.service.RocketmqFailMessageService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class RocketmqFailMessageServiceImpl extends ServiceImpl<RocketmqFailMessageMapper, RocketmqFailMessage>
        implements RocketmqFailMessageService {

    private Integer batchSize;

    @Autowired
    public void setRocketmqProperties(RocketmqProperties rocketmqProperties) {
        this.batchSize = rocketmqProperties.getBatchSize();
    }

    @Override
    public void saveFailMessage(RocketmqFailMessage failMessage) {
        failMessage.setStatus(0); // 0-待补偿
        this.save(failMessage);
    }

    @Override
    public List<RocketmqFailMessage> listWaitCompensateMessage(Integer batchSize) {

        LambdaQueryWrapper<RocketmqFailMessage> queryWrapper = new LambdaQueryWrapper<RocketmqFailMessage>()
                .eq(RocketmqFailMessage::getStatus, 0)
                .orderByAsc(RocketmqFailMessage::getCreateTime)
                .last("LIMIT " + batchSize);
        return this.list(queryWrapper);
    }


    @Override
    public void updateMessageById(RocketmqFailMessage message) {
        RocketmqFailMessage failMessage = new RocketmqFailMessage();
        failMessage.setId(message.getId());
        failMessage.setStatus(message.getStatus());
        failMessage.setRetryCount(message.getRetryCount());
        failMessage.setFailReason(message.getFailReason());
        this.updateById(failMessage);

    }
}
