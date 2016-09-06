package com.seckill.entity;

import java.util.Date;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/24 上午9:30.
 */
public class Seckill {

    //商品id
    private long seckillId;
    //商品名称
    private String name;
    //商品库存
    private int number;
    //秒杀开启时间
    private Date startTime;
    //秒杀结束时间
    private Date endTime;
    //秒杀创建时间
    private Date createTime;

    public long getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(long seckillId) {
        this.seckillId = seckillId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

}
