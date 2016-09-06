package com.seckill.dto;

/**
 * 暴露秒杀地址DTO
 *
 * @Author idler [idler41@163.com]
 * @Date 16/8/24 下午9:21.
 */
public class Exposer {

    //是否开启秒杀
    private boolean exposed;

    //加密
    private String md5;

    private long seckillId;

    //系统当前时间(毫秒)
    private long now;

    //开启时间
    private long startMillis;

    //结束时间
    private long endMillis;

    public Exposer(boolean exposed, String md5, long seckillId) {
        this.exposed = exposed;
        this.md5 = md5;
        this.seckillId = seckillId;
    }

    public Exposer(boolean exposed, long seckillId, long now, long startMillis, long endMillis) {
        this.exposed = exposed;
        this.seckillId = seckillId;
        this.now = now;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
    }

    public Exposer(boolean exposed, long seckillId) {
        this.exposed = exposed;
        this.seckillId = seckillId;
    }


    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(long seckillId) {
        this.seckillId = seckillId;
    }

    public long getNow() {
        return now;
    }

    public void setNow(long now) {
        this.now = now;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public void setStartMillis(long startMillis) {
        this.startMillis = startMillis;
    }

    public long getEndMillis() {
        return endMillis;
    }

    public void setEndMillis(long endMillis) {
        this.endMillis = endMillis;
    }
}
