package com.seckill.entity;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/9/1 下午9:02.
 */
public class PayInfo {

    //订单Id
    private long orderId;
    //秒杀商品Id
    private long seckillId;
    //返回的支付交易号
    private long tradeNo;
    //买家付款的金额
    private long buyerPayAmount;

    //交易状态：WAIT_BUYER_PAY（交易创建，等待买家付款）、TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）、
    //TRADE_SUCCESS（交易支付成功）、TRADE_FINISHED（交易结束，不可退款）
    private String tradeStatus;

    public PayInfo(long seckillId, String tradeStatus) {
        this.seckillId = seckillId;
        this.tradeStatus = tradeStatus;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public long getSeckillId() {
        return seckillId;
    }

    public void setSeckillId(long seckillId) {
        this.seckillId = seckillId;
    }

    public long getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(long tradeNo) {
        this.tradeNo = tradeNo;
    }

    public long getBuyerPayAmount() {
        return buyerPayAmount;
    }

    public void setBuyerPayAmount(long buyerPayAmount) {
        this.buyerPayAmount = buyerPayAmount;
    }

    public String getTradeStatus() {
        return tradeStatus;
    }

    public void setTradeStatus(String tradeStatus) {
        this.tradeStatus = tradeStatus;
    }
}
