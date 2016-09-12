package com.seckill.amqp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.rabbitmq.client.Channel;
import com.seckill.entity.SuccessKilled;
import com.seckill.exception.SeckillException;
import com.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

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

    private long sleepTime = 1000 * 10;

    public void onMessage(Message message, Channel channel) {

        try {
            SuccessKilled successKilled = (SuccessKilled) rabbitTemplate.getMessageConverter().fromMessage(message);
            LOG.info("[x] receive SuccessKilled: " + JSON.toJSONString(successKilled, SerializerFeature.WriteMapNullValue));

            //落地
            seckillService.executeSeckillProc(successKilled);
        } catch (Exception e) {
            //执行存储过程中出错，发送nack命令让consumer能消费下一个消息
            LOG.info(e.getMessage(), e);
            LOG.info("==========================loop nack start=================================");

            for (; ; ) {
                try {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
                    LOG.info("==========================loop nack end=================================");
                    return;
                } catch (IOException ioe) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                        LOG.error("线程异常中断，nack失败，message:{}",
                                JSON.toJSONString(message, SerializerFeature.WriteMapNullValue));
                        throw new RuntimeException(e1.getMessage(),e1);
                    }
                }
            }
        }

        //执行存储过程成功，只需要发送ack成功
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            LOG.info("rabbitmq连接异常，开始进入循环ack", e);
            LOG.info("==========================loop ack start=================================");
            for (; ; ) {
                try {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    LOG.info("==========================loop ack end=================================");
                    return;
                } catch (IOException ioe) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                        LOG.error("consumer{}异常中断，ack失败。下一个consumer消费message时会抛出Code为-1的SeckillException异常,随后会将该message转移到死信队列",Thread.currentThread().getName());
                        throw new RuntimeException(e1.getMessage(),e1);
                    }
                }
            }
        }

    }
}
