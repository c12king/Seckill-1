package com.seckill.dto;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/25 下午3:20.
 */
public class SeckillResult<T> {
    private boolean susccess;
    private T data;
    private String error;

    public SeckillResult(boolean susccess, T data) {
        this.susccess = susccess;
        this.data = data;
    }

    public SeckillResult(boolean susccess, String error) {
        this.susccess = susccess;
        this.error = error;
    }

    public boolean isSusccess() {
        return susccess;
    }

    public void setSusccess(boolean susccess) {
        this.susccess = susccess;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
