package com.czx.wenshu.domain.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 支付订单仓储端口（P9-03）。
 * 由基础设施层 MyBatis 实现。
 */
public interface PaymentOrderRepository {

    /** 保存或更新订单。 */
    void save(PaymentOrder order);

    /** 按主键查询。 */
    Optional<PaymentOrder> findById(UUID id);

    /** 按系统订单号查询（回调验证使用）。 */
    Optional<PaymentOrder> findByOrderNo(String orderNo);

    /** 查询用户所有订单（按创建时间倒序）。 */
    List<PaymentOrder> findByUserId(UUID userId);
}
