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

    @Resource(name = "redisTemplate")
    private ValueOperations<String, SuccessKilled> successKilledOper;
    
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
            }catch (JedisException je) {
                LOG.error(je.getMessage(), je);
                throw new SeckillException("系统异常");
            }
            
            try {
                //MQ发送消息
                rabbitTemplate.convertAndSend(skExName, skRoutKey, successKilled);
            } catch (AmqpException ae) {
                LOG.error(ae.getMessage(), ae);
                //mq不可用，直接操作数据库
                executeSeckillProc(successKilled);
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
## 消息确认


## Seckill的性能优化
### 连接池(db、rabbitmq、redis)
- 通过连接池来管理连接的创建与释放，可节省创建连接的时间。初始化时创建的线程数量也不能太多，避免大量的连接因空闲时间过久而自动断开以及过多的连接检查。

### spring TransactionSynchronizationManager(db、rabbitmq、redis)
- 通过TransactionSynchronizationManager来缓存连接，manager可避免单线程内的每次操作都从连接池获取释放连接，减轻连接池的压力。

### protostuff(redis与rabbitmq都采用protostuff序列化对象)
- protostuff基于Google protobuf，protobuf是所有序列化技术中速度最快的，而且占用空间小。(protostuff与protobuf性能差不多)


