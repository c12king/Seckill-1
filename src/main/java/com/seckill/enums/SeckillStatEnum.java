package com.seckill.enums;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/8/24 下午10:07.
 */
public enum SeckillStatEnum {
    SUCCESS((short)2, "秒杀完成"),
    APPLY((short)1,"申请秒杀成功"),
    END((short)0, "秒杀结束"),
    REPEAT_KILL((short)-1, "重复秒杀"),
    INNER_ERROR((short)-2, "系统异常"),
    //用户修改了MD5
    DATA_REWRITE((short)-3, "数据篡改"),;

    private short state;
    private String stateInfo;

    SeckillStatEnum(short state, String stateInfo) {
        this.state = state;
        this.stateInfo = stateInfo;
    }

    public short getState() {
        return state;
    }

    public void setState(short state) {
        this.state = state;
    }

    public String getStateInfo() {
        return stateInfo;
    }

    public void setStateInfo(String stateInfo) {
        this.stateInfo = stateInfo;
    }

    public static SeckillStatEnum stateOf(int index) {
        for (SeckillStatEnum state : values()) {
            if (state.getState() == index) {
                return state;
            }
        }
        return null;
    }
}
