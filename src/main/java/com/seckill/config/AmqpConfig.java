package com.seckill.config;

import com.seckill.amqp.SecKillConfirm;
import com.seckill.amqp.SeckillReceiver;
import com.seckill.protostuff.ProtostuffMessageConvert;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Arrays;
import java.util.List;


/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/29 上午9:56.
 */
//@ConfigurationProperties(prefix = "spring.rabbitmq")
@Configuration
//TODO RabbitListenerAnnotationBeanPostProcessor declareExchangeAndBinding只看到注册，没看到定义和绑定
@EnableRabbit
public class AmqpConfig {


    //mq sender

    @Value("${spring.rabbitmq.host}")
    private String host;
    @Value("${spring.rabbitmq.port}")
    private Integer port;
    @Value("${spring.rabbitmq.username}")
    private String username;
    @Value("${spring.rabbitmq.password}")
    private String password;
    @Value("${spring.rabbitmq.vhost}")
    private String virtualHost;

    @Bean
    ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        connectionFactory.setPublisherConfirms(true);
        return connectionFactory;
    }

    @Value("${spring.rabbitmq.mandatory}")
    private Boolean mandatory;
    @Value("${spring.rabbitmq.replyTimeout}")
    private long replyTimeout;
//    @Value("${spring.rabbitmq.seckillDeadQueue.bindingKey}")
//    private String bindingKey;
//    @Value("${spring.rabbitmq.seckillDeadQueue.name}")
//    private String deadQueueName;

    @Bean
    RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        //没有匹配Queue时会返还消息给Producer
        template.setMandatory(mandatory);
        template.setReplyTimeout(replyTimeout);
//        template.setQueue(deadQueueName);
//        template.setRoutingKey(bindingKey);
        template.setConfirmCallback(new SecKillConfirm());
        template.setMessageConverter(new ProtostuffMessageConvert());
        return template;
    }

    //mq receiver
    @Bean
    public SeckillReceiver receiver() {
        return new SeckillReceiver();
    }

    @Bean
    MessageListenerAdapter listenerAdapter() {
        return new MessageListenerAdapter(receiver(), "onMessage");
    }

    @Value("${spring.rabbitmq.seckillConsumer.maxConcurrentConsumers}")
    private Integer maxConcurrentConsumers;
    @Value("${spring.rabbitmq.seckillConsumer.concurrentConsumers}")
    private Integer concurrentConsumers;
//    @Value("${spring.rabbitmq.seckillConsumer.recoveryBackOff.initialInterval}")
//    private Long initialIntervalRecover;
//    @Value("${spring.rabbitmq.seckillConsumer.recoveryBackOff.multiplier}")
//    private Double multiplierRecover;
//    @Value("${spring.rabbitmq.seckillConsumer.recoveryBackOff.maxInterval}")
//    private Long maxIntervalRecover;


    // TODO 没有消费者，发送一条消息耗时从60ms涨到200ms(可能是启动耗时,待验证)
    /*
     * TODO 文档描述的：if the consumer is very badly behaved indeed will it give up，badly behaved是retry和recover失败后吗?
     * 不是：是抛出AmqpRejectAndDontRequeueException异常
     */
//    @Bean
//    SimpleMessageListenerContainer messageListenerContainer() {
//        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory());
//        container.addQueues(seckillQueue());
//        container.setMaxConcurrentConsumers(maxConcurrentConsumers);
//        container.setConcurrentConsumers(concurrentConsumers);
//        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
//        container.setMessageListener(listenerAdapter());
//
//        container.setDefaultRequeueRejected(false);
//        return container;
//    }

    //SimpleMessageListenerContainer的consumer有效
    @Bean
    RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
//                .maxAttempts(maxAttempts)  //与backOffOption同时使用会失效
                .backOffOptions(initialInterval, multiplier, maxInterval)        //重试间隔、下一次重试时间*2，最大重试时间
//                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    //作用与sender
    // TODO spring retry

//    @Value("${spring.rabbitmq.retry.maxAttempts}")
//    private Integer maxAttempts;
    @Value("${spring.rabbitmq.retry.backOff.initialInterval}")
    private Long initialInterval;
    @Value("${spring.rabbitmq.retry.backOff.multiplier}")
    private Long multiplier;
    @Value("${spring.rabbitmq.retry.backOff.initialInterval}")
    private Long maxInterval;


    //mq decreable

    //seckill

    @Value("${spring.rabbitmq.seckillExchange.name}")
    private String exchangeName;

    @Bean
    public DirectExchange exchange() {
        return (DirectExchange) ExchangeBuilder.directExchange(exchangeName).durable().build();
    }

    @Value("${spring.rabbitmq.seckillBindingKey}")
    private String seckillBindingKey;
    @Value("${spring.rabbitmq.seckillQueue.name}")
    private String name;

    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", deadExchangeName)
                .withArgument("x-dead-letter-routing-key", deadBindingKey)
//                .withArgument("x-message-ttl", 5000)
                .build();
    }

    //dead letter
    @Value("${spring.rabbitmq.deadExchange.name}")
    private String deadExchangeName;
    @Value("${spring.rabbitmq.seckillDeadQueue.name}")
    private String deadQueueName;
    @Value("${spring.rabbitmq.deadBindingKey}")
    private String deadBindingKey;

    @Bean
    public DirectExchange deadExchange() {
        return (DirectExchange) ExchangeBuilder.directExchange(deadExchangeName).durable().build();
    }

    @Bean
    public Queue seckillDeadLetterQueue() {
        return QueueBuilder.durable(deadQueueName)
                .build();
    }

    //binding
    @Bean
    public List<Binding> binding() {
        return Arrays.asList(
                BindingBuilder.bind(seckillQueue()).to(exchange()).with(seckillBindingKey),
                BindingBuilder.bind(seckillDeadLetterQueue()).to(deadExchange()).with(deadBindingKey)
        );
    }
}
