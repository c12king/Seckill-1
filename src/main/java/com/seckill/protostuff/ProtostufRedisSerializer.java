package com.seckill.protostuff;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * 自定义redis序列化
 * @Author idler [idler41@163.com]
 * @Date 16/9/4 上午8:52.
 */
public class ProtostufRedisSerializer implements RedisSerializer<Object> {

    private static final Schema<ObjectWrapper> schema = RuntimeSchema.getSchema(ObjectWrapper.class);

    public byte[] serialize(Object object) throws SerializationException {
        if (object == null) {
            return null;
        }

        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);

        try{
            return ProtostuffIOUtil.toByteArray(new ObjectWrapper(object), schema, buffer);
        }finally {
            buffer.clear();
        }
    }

    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            ObjectWrapper objectWrapper = new ObjectWrapper();
            ProtostuffIOUtil.mergeFrom(bytes, objectWrapper, schema);
            return objectWrapper.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }

}
