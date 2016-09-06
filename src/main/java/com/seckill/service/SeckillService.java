package com.seckill.service;

import com.seckill.dto.Exposer;
import com.seckill.dto.SeckillExcution;
import com.seckill.entity.PayInfo;
import com.seckill.entity.Seckill;
import com.seckill.exception.SeckillCloseException;
import com.seckill.exception.SeckillException;

import java.util.Map;

/**
 * 业务接口：站在"使用者"角度设计接口
 * 三个方面：方法定义粒度(业务有哪些操作和行为)，参数，返回类型
 *
 * @Author idler [idler41@163.com]
 * @Date 16/8/24 下午5:52.
 */
public interface SeckillService {

    /**
     * 查询单个秒杀记录
     *
     * @param seckillId
     * @return
     */
    Seckill queryById(long seckillId);

    /**
     * 秒杀开始时输出秒杀接口地址，
     * 否则输出系统时间和秒杀时间
     *
     * @param seckillId
     */
    Exposer exportSeckillUrl(long seckillId);


    /**
     * 执行秒杀操作
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     */
    SeckillExcution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, SeckillCloseException;

    /**
     * 接收同步支付结果
     *
     * @param payInfo
     * @return
     */
    SeckillExcution parsePayInfo(PayInfo payInfo, long seckillId, long userPhone);

    boolean executeSeckillProc(Map<String,Object> paramMap);
}
