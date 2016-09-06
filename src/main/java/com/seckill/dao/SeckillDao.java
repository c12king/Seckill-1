package com.seckill.dao;

import com.seckill.entity.Seckill;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/24 上午9:34.
 */
public interface SeckillDao {

    /**减库存
     * @param seckillId
     * @param createTime
     * @return
     */
    int reduceNumber(long seckillId, Date createTime);

    int update(Seckill seckill);

    Seckill queryById(long seckillId);

    List<Seckill> queryAll(int offset, int limit);

    /**
     * 使用存储过程执行秒杀
     * @param paramMap
     */
    void killByProcedure(Map<String, Object> paramMap);
}
