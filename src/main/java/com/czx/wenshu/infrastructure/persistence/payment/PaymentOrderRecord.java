package com.czx.wenshu.infrastructure.persistence.payment;

import java.time.Instant;

/** MyBatis 持久化记录，对应 payment_orders 表（P9-03）。 */
public class PaymentOrderRecord {

    private String id;
    private String userId;
    private String orderNo;
    private String productType;
    private String productKey;
    private long amountFen;
    private String currency;
    private String paymentChannel;
    private String status;
    private Instant paidAt;
    private String channelOrderNo;
    private String rawCallback;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getProductKey() { return productKey; }
    public void setProductKey(String productKey) { this.productKey = productKey; }
    public long getAmountFen() { return amountFen; }
    public void setAmountFen(long amountFen) { this.amountFen = amountFen; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getPaymentChannel() { return paymentChannel; }
    public void setPaymentChannel(String paymentChannel) { this.paymentChannel = paymentChannel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public String getChannelOrderNo() { return channelOrderNo; }
    public void setChannelOrderNo(String channelOrderNo) { this.channelOrderNo = channelOrderNo; }
    public String getRawCallback() { return rawCallback; }
    public void setRawCallback(String rawCallback) { this.rawCallback = rawCallback; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
