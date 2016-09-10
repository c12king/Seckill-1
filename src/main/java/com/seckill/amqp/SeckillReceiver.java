package com.seckill.amqp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.rabbitmq.client.Channel;
import com.seckill.constants.WebConstants;
import com.seckill.entity.Seckill;
import com.seckill.entity.SuccessKilled;
import com.seckill.exception.SeckillException;
import com.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/29 上午11:56.
 */
public class SeckillReceiver implements ChannelAwareMessageListener {

    private final Logger LOG = LoggerFactory.getLogger(SeckillReceiver.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SeckillService seckillService;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, Seckill> seckillOper;

    public void onMessage(Message message, Channel channel) {
        LOG.info("[x] receive message: " + JSON.toJSONString(message, SerializerFeature.WriteMapNullValue));

        SuccessKilled successKilled = (SuccessKilled) rabbitTemplate.getMessageConverter().fromMessage(message);
        boolean hasException = false;
        //ack
        try {
            //落地
            seckillService.executeSeckillProc(successKilled);
            //redis删除缓存
            long seckillId = successKilled.getSeckillId();
            long userPhone = successKilled.getUserPhone();
            seckillOper.getOperations().delete(WebConstants.getSuccessSeckillRedisKey(seckillId, userPhone));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            hasException = true;
//            throw new AmqpRejectAndDontRequeueException("to dead-letter");
        } finally {
            try {
                if (hasException) {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
                } else {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                }
            } catch (IOException e1) {
                LOG.error(e1.getMessage(), e1);
            }
        }
    }
}
