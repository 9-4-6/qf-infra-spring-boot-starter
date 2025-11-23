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

    private Integer compensateMaxRetry;

    // 通过setter注入自定义属性
    @Autowired
    public void setRocketmqProperties(RocketmqProperties rocketmqProperties) {
        this.compensateMaxRetry = rocketmqProperties.getCompensateMaxRetry();
    }

    @Override
    public void saveFailMessage(RocketmqFailMessage failMessage) {
        failMessage.setMaxRetryCount(compensateMaxRetry);
        failMessage.setStatus(0); // 0-待补偿
        failMessage.setRetryCount(0);
        this.save(failMessage);
    }

    @Override
    public List<RocketmqFailMessage> listWaitCompensateMessage() {

        // 使用LambdaQueryWrapper构造查询条件，替代自定义SQL
        LambdaQueryWrapper<RocketmqFailMessage> queryWrapper = new LambdaQueryWrapper<RocketmqFailMessage>()
                .eq(RocketmqFailMessage::getStatus, 0) // 状态为待补偿
                .lt(RocketmqFailMessage::getRetryCount, compensateMaxRetry) // 已补偿次数 < 最大补偿次数
                .orderByAsc(RocketmqFailMessage::getCreateTime); // 按创建时间升序排列

        // 调用MyBatis-Plus的list方法执行查询
        return this.list(queryWrapper);
    }

    @Override
    public boolean updateCompensateStatus(Long id, Integer status, Integer retryCount) {
        RocketmqFailMessage failMessage = new RocketmqFailMessage();
        failMessage.setId(id);
        failMessage.setStatus(status);
        failMessage.setRetryCount(retryCount);
        return this.updateById(failMessage);
    }
}
