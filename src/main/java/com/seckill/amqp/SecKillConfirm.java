package com.seckill.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/29 下午10:47.
 */
public class SecKillConfirm implements RabbitTemplate.ConfirmCallback {

    private final Logger LOG = LoggerFactory.getLogger(SecKillConfirm.class);

    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (!ack) {
            LOG.warn("NACK received, case: {}" + (cause == null ? "null" : cause));
        }
    }
}
