package com.seckill.constants;

import org.springframework.util.DigestUtils;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/9/2 下午5:14.
 */
public class WebConstants {

    //redis 秒杀业务
    private static final String STOCK_SUFFIX = "_stocks";
    private static final String SECKILL_PREFIX = "seckill_";
    private static final String SUCCESS_SECKILL_PREFIX = "successkilled_";

    public static final String getSeckillRedisKey(long seckillId) {
        return SECKILL_PREFIX + seckillId;
    }

    public static final String getSeckillStockRedisKey(long seckillId) {
        return seckillId + WebConstants.STOCK_SUFFIX;
    }

    public static final String getSuccessSeckillRedisKey(long seckillId,long userPhone) {
        return SUCCESS_SECKILL_PREFIX + seckillId + userPhone;
    }

    //用于MD5混淆
    private static final String slat = "asdjoij%F∞§¶••ªªªºº–––asdasf!!!~~d";

    /**
     * MD5加密
     * @param seckillId
     * @return
     */
    public static String getMD5(long seckillId) {
        String base = seckillId + "/" + slat;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }
}
