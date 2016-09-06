package com.seckill.entity;

import java.util.Date;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/24 上午9:31.
 */
public class SuccessKilled {

    private long seckillId;
    private long userPhone;
    private short state;
    private Date createTime;

    //多对一:库存100，每人只能秒杀1个
    private Seckill seckill;

    public SuccessKilled(long seckillId, long userPhone, short state) {
        this.seckillId = seckillId;
        this.state = state;
        this.userPhone = userPhone;
        this.createTime = new Date();
    }

    public long getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(long seckillId) {
        this.seckillId = seckillId;
    }

    public long getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(long userPhone) {
        this.userPhone = userPhone;
    }

    public short getState() {
        return state;
    }

    public void setState(short state) {
        this.state = state;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Seckill getSeckill() {
        return seckill;
    }

    public void setSeckill(Seckill seckill) {
        this.seckill = seckill;
    }
}
