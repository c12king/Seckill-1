package com.seckill.exception;

/**
 * 秒杀关闭异常
 *
 * @Author idler [idler41@163.com]
 * @Date 16/8/24 下午9:33.
 */
public class SeckillCloseException extends SeckillException {
    public SeckillCloseException(String message) {
        super(message);
    }

    public SeckillCloseException(String message, Throwable cause) {
        super(message, cause);
    }
}
