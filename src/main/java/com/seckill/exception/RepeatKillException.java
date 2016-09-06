package com.seckill.exception;

/**
 * 重复秒杀异常
 * @Author idler [idler41@163.com]
 * @Date 16/8/24 下午9:31.
 */
public class RepeatKillException extends SeckillException{
    public RepeatKillException(String message) {
        super(message);
    }

    public RepeatKillException(String message, Throwable cause) {
        super(message, cause);
    }
}
