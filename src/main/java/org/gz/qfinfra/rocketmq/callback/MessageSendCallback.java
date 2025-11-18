package org.gz.qfinfra.rocketmq.callback;

import org.gz.qfinfra.rocketmq.entity.SendR;

/**
 * 消息发送回调接口（对外暴露，统一成功/失败处理）
 * @author guozhong
 */
public interface MessageSendCallback {
    /**
     * 发送成功回调
     * @param result 成功结果（消息ID、主题等）
     */
    void onSuccess(SendR result);

    /**
     * 发送失败回调
     * @param ex 失败异常
     */
    void onFailure(Throwable ex);

}
