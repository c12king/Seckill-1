package com.seckill.config;

import com.seckill.amqp.SecKillConfirm;
import com.seckill.amqp.SeckillReceiver;
import com.seckill.protostuff.ProtostuffMessageConvert;
import org.springframework.amqp.core.AbstractExchange;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import java.util.Arrays;
import java.util.List;


/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/29 上午9:56.
 */
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
//        connectionFactory.setConnectionCacheSize(channelCacheSize);
//        connectionFactory.set
        return connectionFactory;
    }


    @Bean
//    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        //没有匹配Queue时会返还消息给Producer
        template.setMandatory(true);
        template.setConfirmCallback(new SecKillConfirm());
        template.setMessageConverter(new ProtostuffMessageConvert());
        return template;
    }

    //seckill exchange
    @Value("${spring.rabbitmq.seckill.exchange.name}")
    private String skExName;
    @Value("${spring.rabbitmq.seckill.exchange.type}")
    private String skExType;
    @Value("${spring.rabbitmq.seckill.exchange.durable}")
    private boolean skExDurable;
    @Value("${spring.rabbitmq.seckill.exchange.autoDelete}")
    private boolean skExAutoDel;
    //binding
    @Value("${spring.rabbitmq.seckill.exchange.routingKey}")
    private String skRoutKey;

    //seckill queue
    @Value("${spring.rabbitmq.seckill.exchange.queue.name}")
    private String skQueName;
    @Value("${spring.rabbitmq.seckill.exchange.queue.durable}")
    private boolean skQueDurable;
    @Value("${spring.rabbitmq.seckill.exchange.queue.autoDelete}")
    private boolean skQueAutoDel;
    @Value("${spring.rabbitmq.seckill.exchange.queue.exclusive}")
    private boolean skQueExclusive;

    @Bean
    public List<Declarable> seckillDeclarable() {
        return Arrays.<Declarable>asList(
                createByType(skExType, skExName, skExDurable, skExAutoDel),
                new Queue(skQueName, skQueDurable, skQueExclusive, skQueAutoDel),
                new Binding(skQueName, Binding.DestinationType.QUEUE, skExName, skRoutKey, null)
        );
    }

    private AbstractExchange createByType(String type, String exchangeName, boolean durable, boolean skExAutoDel) {
        if (ExchangeTypes.DIRECT.equals(type)) {
            return new DirectExchange(exchangeName, durable, skExAutoDel);
        }
        //TODO 创建不能识别异常
        throw new RuntimeException("");
    }


    @Bean
    public SeckillReceiver receiver() {
        return new SeckillReceiver();
    }

    @Bean
    MessageListenerAdapter listenerAdapter(SeckillReceiver receiver) {
        return new MessageListenerAdapter(receiver, "onMessage");
    }

    /**
     * 1. If a MessageListener fails because of a business exception, the message listener container会处理异常，并监听下一个message
     *
     * 2. If the failure is caused by a dropped connection (not a business exception), then
     * the consumer that is collecting messages for the listener has to be cancelled and restarted.
     * The SimpleMessageListenerContainer handles this seamlessly, and it leaves a log to say that
     * the listener is being restarted. In fact it loops endlessly trying to restart the consumer,
     * and only if the consumer is very badly behaved indeed will it give up. One side effect is that
     * if the broker is down when the container starts, it will just keep trying until a connection
     * can be established.
     *
     * @param maxConcurrentConsumers
     * @param concurrentConsumers
     * @param connectionFactory
     * @param listenerAdapter
     * @return T
     *
     * TODO 没有消费者，发送一条消息耗时从60ms涨到200ms(可能是启动耗时,待验证)
     * TODO 文档描述的：if the consumer is very badly behaved indeed will it give up，badly behaved是retry和recover失败后吗?
     */

    @Bean
    SimpleMessageListenerContainer messageListenerContainer(
            @Value("${spring.rabbitmq.seckill.exchange.consumer.maxConcurrentConsumers}") Integer maxConcurrentConsumers,
            @Value("${spring.rabbitmq.seckill.exchange.consumer.concurrentConsumers}") Integer concurrentConsumers,
            ConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(skExName);
        container.setExposeListenerChannel(true);
        container.setMaxConcurrentConsumers(maxConcurrentConsumers);
        container.setConcurrentConsumers(concurrentConsumers);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    /**
     * 作用于SimpleMessageListenerContainer
     * 避免因业务异常而无限重启consumer
     * @return
     */
    @Bean
    RetryOperationsInterceptor interceptor() {
        return RetryInterceptorBuilder.stateless()
//                .retryPolicy()
//                .recoverer(new RepublishMessageRecoverer())
                .maxAttempts(4)
                .backOffOptions(1000, 2.0, 1000)   //重试间隔、下一次重试时间*2，最大重试时间
                .build();
    }

}
