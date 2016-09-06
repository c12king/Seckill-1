package com.seckill.protostuff;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * 自定义rabbitmq序列化
 * @Author idler [idler41@163.com]
 * @Date 16/9/4 下午4:01.
 */
public class ProtostuffMessageConvert implements MessageConverter {

    private static final Schema<ObjectWrapper> schema = RuntimeSchema.getSchema(ObjectWrapper.class);

    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        if (object == null) {
            return null;
        }

        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);

        try {
            byte[] bytes = ProtostuffIOUtil.toByteArray(new ObjectWrapper(object), schema, buffer);
            return MessageBuilder.withBody(bytes).build();
        } finally {
            buffer.clear();
        }
    }

    public Object fromMessage(Message message) throws MessageConversionException {
        byte[] body = message.getBody();
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            ObjectWrapper objectWrapper = new ObjectWrapper();
            ProtostuffIOUtil.mergeFrom(body, objectWrapper, schema);
            return objectWrapper.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
