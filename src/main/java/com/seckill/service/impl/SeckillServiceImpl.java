package com.seckill.service.impl;

import com.seckill.constants.WebConstants;
import com.seckill.dao.SeckillDao;
import com.seckill.dto.Exposer;
import com.seckill.dto.SeckillExcution;
import com.seckill.entity.PayInfo;
import com.seckill.entity.Seckill;
import com.seckill.entity.SuccessKilled;
import com.seckill.enums.SeckillStatEnum;
import com.seckill.exception.RepeatKillException;
import com.seckill.exception.SeckillCloseException;
import com.seckill.exception.SeckillException;
import com.seckill.service.SeckillService;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/24 下午9:37.
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    private final static Logger LOG = LoggerFactory.getLogger(SeckillServiceImpl.class);

    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, Seckill> seckillOper;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, SuccessKilled> successKilledOper;

    @Value("${spring.rabbitmq.seckillExchange.name}")
    private String skExName;
    @Value("${spring.rabbitmq.seckillQueue.bindingKey}")
    private String skRoutKey;

    public Seckill queryById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }



    public Exposer exportSeckillUrl(long seckillId) {
        Seckill seckill = seckillOper.get(WebConstants.getSeckillRedisKey(seckillId));

        long startTime = seckill.getStartTime().getTime();
        long endTime = seckill.getEndTime().getTime();
        long nowTime = new Date().getTime();
        if (nowTime < startTime || nowTime > endTime) {
            return new Exposer(false, seckillId, nowTime, startTime, endTime);
        }
        String md5 = WebConstants.getMD5(seckillId);
        return new Exposer(true, md5, seckillId);
    }


    /**
     * redis 原子性decr操作，前(库存量*n)位秒杀成功则缓存订单信息，
     * 进入支付流程，其余的秒杀失败；
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    public SeckillExcution executeSeckill(final long seckillId, final long userPhone, String md5) throws SeckillException, SeckillCloseException {
        if (md5 == null || !md5.equals(WebConstants.getMD5(seckillId))) {
            throw new SeckillException("seckill data rewrite");
        }
        String stockKey = WebConstants.getSeckillStockRedisKey(seckillId);
        long result = seckillOper.increment(stockKey, -1);
        if (result < 0) {
            //秒杀结束：无库存或秒杀结束等
            throw new SeckillCloseException("seckill is closed");
        } else {
            SuccessKilled successKilled = new SuccessKilled(seckillId, userPhone, SeckillStatEnum.APPLY.getState());
            String successKey = WebConstants.getSuccessSeckillRedisKey(seckillId, userPhone);
            //缓存秒杀成功订单信息
            successKilledOper.set(successKey, successKilled, 30, TimeUnit.MINUTES);
            return new SeckillExcution(seckillId, SeckillStatEnum.APPLY);
        }
    }


    /**
     * @param payInfo
     * @param seckillId
     * @param userPhone
     * @return TODO @Transcational与@RabbitListener配合使用 http://docs.spring.io/spring-amqp/docs/1.6.2.RELEASE/reference/html/_reference.html#collection-declaration
     */
    public SeckillExcution parsePayInfo(PayInfo payInfo, long seckillId, long userPhone) {
        //支付成功
        if ("TRADE_SUCCESS".equals(payInfo.getTradeStatus())) {
            String key = WebConstants.getSuccessSeckillRedisKey(seckillId, userPhone);
            SuccessKilled successKilled = successKilledOper.get(key);
            successKilled.setState(SeckillStatEnum.SUCCESS.getState());
            //更新订单支付状态
            successKilledOper.set(key, successKilled);
            //MQ发送消息
            try {
                rabbitTemplate.convertAndSend(skExName, skRoutKey, successKilled);
            } catch (AmqpIOException aioe) {
                LOG.error(aioe.getMessage(),aioe);
                // TODO 发送失败处理措施
            } catch (AmqpException ae) {
                LOG.error(ae.getMessage(), ae);
                // TODO 发送失败处理措施
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
        throw new SeckillException("执行存储过程异常code: " + result);
    }



}
