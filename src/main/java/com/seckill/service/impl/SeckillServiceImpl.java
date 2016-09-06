package com.seckill.service.impl;

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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/24 下午9:37.
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    private final static Logger LOG = LoggerFactory.getLogger(SeckillServiceImpl.class);

    //用于MD5混淆
    private final String slat = "asdjoij%F∞§¶••ªªªºº–––asdasf!!!~~d";

    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, Seckill> seckillOper;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, SuccessKilled> successKilledOper;

    @Value("${spring.rabbitmq.seckill.exchange.name}")
    private String skExName;
    @Value("${spring.rabbitmq.seckill.exchange.routingKey}")
    private String skRoutKey;

    public Seckill queryById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

    private static final String STOCK_SUFFIX = "_stocks";
    private static final String SECKILL_PREFIX = "seckill_";
    private static final String SUCCESS_SECKILL_PREFIX = "successkilled_";

    public Exposer exportSeckillUrl(long seckillId) {
        Seckill seckill = seckillOper.get(SECKILL_PREFIX + seckillId);

        long startTime = seckill.getStartTime().getTime();
        long endTime = seckill.getEndTime().getTime();
        long nowTime = new Date().getTime();
        if (nowTime < startTime || nowTime > endTime) {
            return new Exposer(false, seckillId, nowTime, startTime, endTime);
        }
        String md5 = getMD5(seckillId);
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
        if (md5 == null || !md5.equals(getMD5(seckillId))) {
            throw new SeckillException("seckill data rewrite");
        }
        String stockKey = seckillId + STOCK_SUFFIX;
        long result = seckillOper.increment(stockKey, -1);
        if (result < 0) {
            //秒杀结束：无库存或秒杀结束等
            throw new SeckillCloseException("seckill is closed");
        } else {
            SuccessKilled successKilled = new SuccessKilled(seckillId, userPhone, SeckillStatEnum.APPLY.getState());
            String successKey = SUCCESS_SECKILL_PREFIX + seckillId + userPhone;
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
            String key = SUCCESS_SECKILL_PREFIX + seckillId + userPhone;
            SuccessKilled successKilled = successKilledOper.get(key);
            successKilled.setState(SeckillStatEnum.SUCCESS.getState());
            //更新订单支付状态
            successKilledOper.set(key, successKilled);
            //MQ发送消息
            rabbitTemplate.convertAndSend(skExName, skRoutKey, successKilled);

            return new SeckillExcution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
        }
        throw new SeckillCloseException("pay error");
    }

    public boolean executeSeckillProc(Map<String, Object> paramMap) {

        seckillDao.killByProcedure(paramMap);
        int result = MapUtils.getInteger(paramMap, "result", -10);
        System.out.println("result: " + result);
        if (result == 2) {
            return true;
        }
        return false;
    }

    /**
     * 单纯的seckillId有可能破解
     *
     * @param seckillId
     * @return
     */
    private String getMD5(long seckillId) {
        String base = seckillId + "/" + slat;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }

}
