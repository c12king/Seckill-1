package com.seckill.amqp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.rabbitmq.client.Channel;
import com.seckill.entity.SuccessKilled;
import com.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

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

    public void onMessage(Message message, Channel channel) throws Exception {
        LOG.info("[x] receive message: " + JSON.toJSONString(message, SerializerFeature.WriteMapNullValue));

        try {
            SuccessKilled successKilled = (SuccessKilled) rabbitTemplate
                    .getMessageConverter().fromMessage(message);
            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put("seckillId", successKilled.getSeckillId());
            paramMap.put("userPhone", successKilled.getUserPhone());
            paramMap.put("killTime", successKilled.getCreateTime());
            paramMap.put("payStat", successKilled.getState());
            paramMap.put("result", null);

//            System.out.println("paramMap: " + JSON.toJSONString(paramMap, SerializerFeature.WriteMapNullValue));
            seckillService.executeSeckillProc(paramMap);
//            LOG.info("excuteResult: " + );
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }
}
