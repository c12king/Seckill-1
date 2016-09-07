package com.seckill.amqp;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageKeyGenerator;
import org.springframework.util.Assert;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/9/7 下午2:57.
 */
public class ContentMessageKeyGenerator implements MessageKeyGenerator {

    public Object getKey(Message message) {
        Assert.notNull(message);
        return DigestUtils.md5Hex(message.getBody());
    }
}
