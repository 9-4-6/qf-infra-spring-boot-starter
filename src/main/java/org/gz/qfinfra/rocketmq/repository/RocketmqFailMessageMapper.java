package org.gz.qfinfra.rocketmq.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.gz.qfinfra.rocketmq.entity.RocketmqFailMessage;
/**
 * @author guozhong
 */
@Mapper
public interface RocketmqFailMessageMapper extends BaseMapper<RocketmqFailMessage> {
}
