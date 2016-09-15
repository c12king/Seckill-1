package com.seckill.amqp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.rabbitmq.client.Channel;
import com.seckill.entity.SuccessKilled;
import com.seckill.exception.SeckillException;
import com.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.ConnectException;

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

    /**
     * @param message
     * @param channel
     * @throws IOException
     */
    //不能用jdbc事务：假如执行完ack后，突然jdbc连接断开则会导致事务不能提交，消息没处理就丢失了!!!
    public void onMessage(Message message, Channel channel) throws IOException {

        try {
            SuccessKilled successKilled = (SuccessKilled) rabbitTemplate.getMessageConverter().fromMessage(message);
            if (successKilled != null) {
                LOG.info("[x] receive SuccessKilled: " + JSON.toJSONString(successKilled, SerializerFeature.WriteMapNullValue));
            }

            //落地
            //在业务中解决重发问题
            seckillService.executeSeckillProc(successKilled);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (SeckillException e) {
            //秒杀逻辑出错，发送nack命令让consumer能消费下一个消息
            LOG.warn("秒杀信息落地发生逻辑错误：" + e.getMessage(), e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }

    }
}
