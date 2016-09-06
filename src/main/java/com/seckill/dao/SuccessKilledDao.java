package com.seckill.dao;

import com.seckill.entity.SuccessKilled;

import java.util.List;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/24 上午9:36.
 */
public interface SuccessKilledDao {

    /**
     * 插入购买明细,联合主键可过滤重复
     * @param successKilledId
     * @param userPhone
     * @return
     */
    int insertSuccessKilled(SuccessKilled successKilled);

    /**
     * 根据id查询SuccessKilled并携带秒杀实体对象
     * @param seckillId
     * @return
     */
    SuccessKilled queryByIDWithSeckill(long seckillId,long userPhone);

    /**
     * 根据id查询SuccessKilled并携带秒杀实体对象
     * @param seckillId
     * @return
     */
    List<SuccessKilled> queryAllWithSeckill(long seckillId);
}
