package com.seckill.protostuff;

/**
 * redis序列化辅助类.单纯的泛型无法定义通用schema，原因是无法通过泛型T得到Class<T>
 *
 * @Author idler [idler41@163.com]
 * @Date 16/9/4 上午9:05.
 */
public class ObjectWrapper {
    private Object object;

    public ObjectWrapper(Object object) {
        super();
        this.object = object;
    }

    public ObjectWrapper() {
        super();
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }
}
