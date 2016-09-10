package com.seckill.config;

import com.seckill.protostuff.ProtostufRedisSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 参考官方文档
 * http://docs.spring.io/spring-data/redis/docs/1.7.2.RELEASE/reference/html/#get-started
 * @Author idler [idler41@163.com]
 * @Date 16/8/29 下午3:05.
 */
@Configuration
public class RedisConfig {

    private final Logger LOG = LoggerFactory.getLogger(RedisConfig.class);

    @Bean
    JedisPoolConfig jedisPoolConfig(
            @Value("${spring.redis.pool.minIdle}") int minIdle,
            @Value("${spring.redis.pool.maxIdle}") int maxIdle,
            @Value("${spring.redis.pool.maxTotal}") int maxTotal,
            @Value("${spring.redis.pool.LIFO}") boolean lifo,
            @Value("${spring.redis.pool.betweenEvictSecond}") long betweenEvictSecond,
            @Value("${spring.redis.pool.perEvictRun}") int perEvictRun) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMinIdle(minIdle);
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMaxTotal(maxTotal);
        jedisPoolConfig.setLifo(lifo);
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(betweenEvictSecond);
        jedisPoolConfig.setNumTestsPerEvictionRun(perEvictRun);
//        jedisPoolConfig.setMinEvictableIdleTimeMillis(minEvictableIdleMinutes);
//        jedisPoolConfig.setMaxWaitMillis(maxWaitMillis);
        return jedisPoolConfig;
    }

    @Bean
    JedisConnectionFactory jedisConnectionFactory(
            JedisPoolConfig poolConfig,
            @Value("${spring.redis.host}") String host,
            @Value("${spring.redis.port}") int port,
            @Value("${spring.redis.timeout}") int timeout) {
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
//        connectionFactory.setUsePool(true);
        connectionFactory.setHostName(host);
        connectionFactory.setPort(port);
        connectionFactory.setTimeout(timeout);
        connectionFactory.setPoolConfig(poolConfig);
        return connectionFactory;
    }

    @Bean
    RedisTemplate redisTemplate(JedisConnectionFactory jedisConnectionFactory) {
        RedisTemplate template = new RedisTemplate();
        template.setConnectionFactory(jedisConnectionFactory);

        //TODO 序列化类没带泛型，报unchecked警告
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new ProtostufRedisSerializer());
        return template;
    }
}
