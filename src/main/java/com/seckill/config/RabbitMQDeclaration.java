//package com.seckill.config;
//
//import org.springframework.amqp.core.AbstractExchange;
//import org.springframework.amqp.core.Declarable;
//import org.springframework.amqp.core.DirectExchange;
//import org.springframework.amqp.core.Exchange;
//import org.springframework.amqp.core.ExchangeBuilder;
//import org.springframework.amqp.core.ExchangeTypes;
//import org.springframework.amqp.core.Queue;
//import org.springframework.amqp.core.QueueBuilder;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.Arrays;
//import java.util.List;
//
///**
// * @Author idler [idler41@163.com]
// * @Date 16/9/7 下午2:28.
// */
//@Configuration
//public class RabbitMQDeclaration {
//
//    //seckill exchange
//    @Value("${spring.rabbitmq.seckill.exchange.name}")
//    private String skExName;
//    @Value("${spring.rabbitmq.seckill.exchange.type}")
//    private String skExType;
//    @Value("${spring.rabbitmq.seckill.exchange.durable}")
//    private boolean skExDurable;
//    @Value("${spring.rabbitmq.seckill.exchange.autoDelete}")
//    private boolean skExAutoDel;
//    //binding
//    @Value("${spring.rabbitmq.seckill.exchange.routingKey}")
//    private String skRoutKey;
//
//    //seckill queue
//    @Value("${spring.rabbitmq.seckill.exchange.queue.name}")
//    private String skQueName;
//    @Value("${spring.rabbitmq.seckill.exchange.queue.durable}")
//    private boolean skQueDurable;
//    @Value("${spring.rabbitmq.seckill.exchange.queue.autoDelete}")
//    private boolean skQueAutoDel;
//    @Value("${spring.rabbitmq.seckill.exchange.queue.exclusive}")
//    private boolean skQueExclusive;
//
//    @Bean
//    public List<Declarable> seckillDeclarable() {
//        return Arrays.<Declarable>asList(
//                createByType(skExType, skExName, skExDurable, skExAutoDel),
//                new Queue(skQueName, skQueDurable, skQueExclusive, skQueAutoDel),
//                new org.springframework.amqp.core.Binding(skQueName, org.springframework.amqp.core.Binding.DestinationType.QUEUE, skExName, skRoutKey, null)
//        );
//    }
//
//    private AbstractExchange createByType(String type, String exchangeName, boolean durable, boolean skExAutoDel) {
//        if (ExchangeTypes.DIRECT.equals(type)) {
//            return new DirectExchange(exchangeName, durable, skExAutoDel);
//        }
//        //TODO 创建不能识别异常
//        throw new RuntimeException("");
//    }
//}
