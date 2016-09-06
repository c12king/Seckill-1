package com.seckill.exception;

/**
 * 秒杀相关异常
 * @Author idler [idler41@163.com]
 * @Date 16/8/24 下午9:34.
 */
public class SeckillException extends RuntimeException{
    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}
