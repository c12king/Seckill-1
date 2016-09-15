## 项目介绍
seckill是高并发下的秒杀功能的后台实现。其特点是在高并发下的快速响应。

秒杀业务的核心流程是：减库存、添加订单信息。这两个操作是原子性的执行，一般基于mysql的事务来实现。但在高并发场景下会出现性能瓶颈：

* mysql数据库的连接数是有限的，默认为100。连接数不够时其余的请求线程都会堵塞；
* 并发减库存的update操作与添加订单的insert操作都会进行一次当前读操作而加锁(X锁)，
没有获得锁的线程都会堵塞；

### 项目设计
为了解决高并发下出现的上述两个问题，需要将mysql的操作异步化。seckill采用基于缓存 + 消息队列方式实现。

基本流程(由于本人没有实际的支付接口调用经验，所以本项目的支付调用只是模拟)：

1. 申请秒杀，申请失败则秒杀成功
2. 秒杀成功，则进入支付页面
3. 支付调用，如果没有库存则秒杀失败
4. 有库存则秒杀成功，秒杀信息发送给mq由consumer异步落地

### 秒杀时序图(未成功加载见doc/seckill.png)
![seckill process diagram](doc/seckill.png)

## 超卖问题
### 
超卖问题关键在于库存量的统计是否正确(多web服务器场景)。

redis服务端是单线程来处理所有请求的，但客户端的复合操作(如：get num & set num = num - 1)并不能保证原子性执行。

不过redis还有原子性操作incr，由(incr -1)操作来统计库存剩余量，可解决多客户端的库存统计问题。基本流程：

1. redis缓存库存量n与秒杀申请数n*m(m默认为1，也可设置大于1，避免有人申请成功不支付，有人想支付却没有名额)
2. 每位用户秒杀申请时，申请数(incr －1)如果返回结果< 0则秒杀申请失败，否则申请秒杀成功，进入支付页面
3. 支付调用成功后库存量(incr －1)，如果返回结果< 0则秒杀失败，否则秒杀成功，发送消息到mq

SeckillServiceImpl.java

```java
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, Seckill> seckillOper;
    
    @Value("${spring.rabbitmq.seckillExchange.name}")
    private String skExName;
    @Value("${spring.rabbitmq.seckillBindingKey}")
    private String skRoutKey;
    
    //秒杀申请
    public SeckillExcution executeSeckill(final long seckillId, final long userPhone, String md5) throws SeckillException, SeckillCloseException {
        if (md5 == null || !md5.equals(WebConstants.getMD5(seckillId))) {
            throw new SeckillException("seckill data rewrite");
        }
        String applyNumKey = WebConstants.getApplyNumRedisKey(seckillId);
        long result = seckillOper.increment(applyNumKey, -1);
        if (result < 0) {
            //秒杀结束：无库存或秒杀结束等
            throw new SeckillCloseException("seckill is closed");
        } else {
            return new SeckillExcution(seckillId, SeckillStatEnum.APPLY);
        }
    }
    
    //支付通知
    public SeckillExcution parsePayInfo(PayInfo payInfo, long seckillId, long userPhone) {
        //支付成功
        if ("TRADE_SUCCESS".equals(payInfo.getTradeStatus())) {
            SuccessKilled successKilled = null;
            Long decrResult = null;
            try {
                String stockKey = WebConstants.getSeckillStockRedisKey(seckillId);
                decrResult = seckillOper.increment(stockKey, -1);

                if (decrResult < 0){
                    //秒杀结束：无库存或秒杀结束等
                    throw new SeckillCloseException("seckill is closed");
                }

                successKilled = new SuccessKilled(seckillId, userPhone, SeckillStatEnum.SUCCESS.getState());

                //MQ发送消息
                rabbitTemplate.convertAndSend(skExName, skRoutKey, successKilled);

            }catch (JedisException je) {
                LOG.error(je.getMessage(), je);
                throw new SeckillException("系统异常");
            }catch (AmqpException ae) {
                LOG.error(ae.getMessage(), ae);
                //TODO mq发送失败处理策略(异步处理)
//                executeSeckillProc(successKilled);
            }
            return new SeckillExcution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
        }
        throw new SeckillCloseException("pay error");
    }
    
    public boolean executeSeckillProc(SuccessKilled successKilled) {

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("seckillId", successKilled.getSeckillId());
        paramMap.put("userPhone", successKilled.getUserPhone());
        paramMap.put("killTime", successKilled.getCreateTime());
        paramMap.put("payStat", successKilled.getState());
        paramMap.put("result", null);

        seckillDao.killByProcedure(paramMap);

        int result = MapUtils.getInteger(paramMap, "result", -10);

        LOG.debug("执行存储过程完成,code: " + result);

        if (result == 2) {
            return true;
        }
        throw new SeckillException("系统异常code: " + result);
    }
```

## 消息落地幂等性问题
重复消费同一消息是会抛出业务异常。

#####解决方法(TODO)
在存储过程加上检查过程，发现已经执行过，返回执行成功信息。

## 数据一致性问题

### 秒杀过程
redis的操作库存减1后，必须将账单消息发送给mq服务器。

问题：秒杀成功后，web服务器崩溃或者mq服务器崩溃都将导致消息发送失败

##### 解决方法

将rabbitmq换成rocketmq。rocketmq发送消息分执行过程：

- 发送prepared消息；
- 执行业务逻辑
- 业务逻辑的事务执行成功后，发送确认消息；

RocketMQ会定期扫描消息集群中的事务消息，如果发现prepared消息，它会向消息发送者确认，根据制定的策略决定回滚还是继续发送确认消息。

### 落地过程
消费消息，update减库存，insert账单信息

问题: 库存与账单不在同一数据库中如何处理？

##### 解决方法
采用强一致性的两阶段提交的方法，JavaEE的JTA事务。

### Sagas长事务(TODO)
在Sagas事务模型中，一个长事务由一个预先定义好执行顺序的事务集合和他们对应的补偿子事务集合组成。

业务只需要进行交易编排，每个原子操作提供正反交易。

Sagas长事务似乎能同时解决上述问题，但开发的复杂度较高。

## Seckill的性能优化
### 连接池(db、rabbitmq、redis)
- 通过连接池来管理连接的创建与释放，可节省创建连接的时间。初始化时创建的线程数量也不能太多，避免大量的连接因空闲时间过久而自动断开以及过多的连接检查。

### spring TransactionSynchronizationManager(db、rabbitmq、redis)
- 通过TransactionSynchronizationManager来缓存连接，manager可避免单线程内的每次操作都从连接池获取释放连接，减轻连接池的压力。

### protostuff(redis与rabbitmq都采用protostuff序列化对象)
- protostuff基于Google protobuf，protobuf是所有序列化技术中速度最快的，而且占用空间小。(protostuff与protobuf性能差不多)

## Seckill问题
- 还没有解决数据的一致性问题
- 订单管理、支付管理、仓库管理没有明确的需求，相应的也不了解是否要拆分成3个独立的应用系统，3个独立的数据库。


