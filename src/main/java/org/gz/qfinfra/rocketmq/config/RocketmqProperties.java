package org.gz.qfinfra.rocketmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 17853
 */
@Data
@ConfigurationProperties(prefix = "rocketmq.qf")
public class RocketmqProperties {

    private Integer batchSize = 100;

}
