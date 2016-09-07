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
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.util.backoff.ExponentialBackOff;


/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/29 上午9:56.
 */
@ConfigurationProperties(prefix = "spring.rabbitmq")
@Configuration
//TODO RabbitListenerAnnotationBeanPostProcessor declareExchangeAndBinding只看到注册，没看到定义和绑定
@EnableRabbit
public class AmqpConfig {

    //本身提供自动重连
    @Bean
    ConnectionFactory connectionFactory(
            @Value("${spring.rabbitmq.host}") String host,
            @Value("${spring.rabbitmq.port}") Integer port,
            @Value("${spring.rabbitmq.username}") String username,
            @Value("${spring.rabbitmq.password}") String password,
            @Value("${spring.rabbitmq.vhost}") String virtualHost) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        connectionFactory.setPublisherConfirms(true);
        return connectionFactory;
    }

    @Bean
    RabbitTemplate rabbitTemplate(
            @Value("${spring.rabbitmq.mandatory}") boolean mandatory,
            @Value("${spring.rabbitmq.replyTimeout}") long replyTimeout,

            ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        //没有匹配Queue时会返还消息给Producer
        template.setMandatory(mandatory);
        template.setReplyTimeout(replyTimeout);
        template.setConfirmCallback(new SecKillConfirm());
        template.setMessageConverter(new ProtostuffMessageConvert());
        return template;
    }

    @Bean
    public SeckillReceiver receiver() {
        return new SeckillReceiver();
    }

    @Bean
    MessageListenerAdapter listenerAdapter(SeckillReceiver receiver) {
        return new MessageListenerAdapter(receiver, "onMessage");
    }

//    @Value("${spring.rabbitmq.seckill.exchange.queue.name}")
//    private String skQueName;

    /**
     * @param maxConcurrentConsumers
     * @param concurrentConsumers
     * @param connectionFactory
     * @param listenerAdapter
     * @return T
     * <p>
     * TODO 没有消费者，发送一条消息耗时从60ms涨到200ms(可能是启动耗时,待验证)
     * TODO 文档描述的：if the consumer is very badly behaved indeed will it give up，badly behaved是retry和recover失败后吗?
     */
//    @ConfigurationProperties(prefix = "seckillConsumer")
    @Bean
    SimpleMessageListenerContainer messageListenerContainer(
            @Value("${spring.rabbitmq.seckillConsumer.maxConcurrentConsumers}") Integer maxConcurrentConsumers,
            @Value("${spring.rabbitmq.seckillConsumer.concurrentConsumers}") Integer concurrentConsumers,
            @Value("${spring.rabbitmq.seckillConsumer.recoveryBackOff.initialInterval}") Long initialInterval,
            @Value("${spring.rabbitmq.seckillConsumer.recoveryBackOff.multiplier}") Double multiplier,
            @Value("${spring.rabbitmq.seckillConsumer.recoveryBackOff.maxInterval}") Long maxInterval,
            Queue seckillQueue, ConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addQueues(seckillQueue);
        container.setMaxConcurrentConsumers(maxConcurrentConsumers);
        container.setConcurrentConsumers(concurrentConsumers);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setMessageListener(listenerAdapter);

        ExponentialBackOff policy = new ExponentialBackOff();
        policy.setInitialInterval(initialInterval);
        policy.setMultiplier(multiplier);
        policy.setMaxInterval(maxInterval);
        container.setRecoveryBackOff(policy);
        return container;
    }

    //作用与sender
    // TODO spring retry
    @Bean
    RetryOperationsInterceptor retryInterceptor(
            @Value("${spring.rabbitmq.retry.maxAttempts}") Integer maxAttempts,
            @Value("${spring.rabbitmq.retry.backOff.initialInterval}") Long initialInterval,
            @Value("${spring.rabbitmq.retry.backOff.multiplier}") Long multiplier,
            @Value("${spring.rabbitmq.retry.backOff.initialInterval}") Long maxInterval) {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(maxAttempts)
                .backOffOptions(initialInterval, multiplier, maxInterval)        //重试间隔、下一次重试时间*2，最大重试时间
                .build();
    }

    //seckill
    @Bean
    public DirectExchange exchange(@Value("${spring.rabbitmq.seckillExchange.name}") String exchangeName) {
        return (DirectExchange) ExchangeBuilder.directExchange(exchangeName).durable().build();
    }

    @Bean
    public Queue seckillQueue(@Value("${spring.rabbitmq.seckillQueue.name}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    public Binding seckillBinding(
            DirectExchange exchange, Queue seckillQueue,
            @Value("${spring.rabbitmq.seckillQueue.bindingKey}") String seckillBindingKey) {
        return BindingBuilder.bind(seckillQueue).to(exchange)
                .with(seckillBindingKey);
    }


//dead letter


}
