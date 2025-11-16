package org.gz.qfinfra.rocketmq.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtil {
    /**
     * 对象转 JSON 字符串
     */
    public static String toJson(Object obj) {
        try {
            return JSON.toJSONString(obj);
        } catch (JSONException e) {
            log.error("JSON 序列化失败，obj={}", obj, e);
            return "";
        }
    }

    /**
     * JSON 字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return JSON.parseObject(json, clazz);
        } catch (JSONException e) {
            log.error("JSON 反序列化失败，json={}, clazz={}", json, clazz.getName(), e);
            return null;
        }
    }
}
