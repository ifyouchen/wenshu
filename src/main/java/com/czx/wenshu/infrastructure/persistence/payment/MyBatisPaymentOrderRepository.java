package com.czx.wenshu.infrastructure.persistence.payment;

import com.czx.wenshu.domain.payment.PaymentOrder;
import com.czx.wenshu.domain.payment.PaymentOrderRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * {@link PaymentOrderRepository} 的 MyBatis 实现（P9-03）。
 */
@Repository
public class MyBatisPaymentOrderRepository implements PaymentOrderRepository {

    private final PaymentOrderMapper mapper;

    public MyBatisPaymentOrderRepository(PaymentOrderMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(PaymentOrder order) {
        PaymentOrderRecord rec = toRecord(order);
        // 判断是否已存在
        PaymentOrderRecord existing = mapper.findById(order.id().toString());
        if (existing == null) {
            mapper.insert(rec);
        } else {
            mapper.update(rec);
        }
    }

    @Override
    public Optional<PaymentOrder> findById(UUID id) {
        return Optional.ofNullable(mapper.findById(id.toString())).map(this::toDomain);
    }

    @Override
    public Optional<PaymentOrder> findByOrderNo(String orderNo) {
        return Optional.ofNullable(mapper.findByOrderNo(orderNo)).map(this::toDomain);
    }

    @Override
    public List<PaymentOrder> findByUserId(UUID userId) {
        return mapper.findByUserId(userId.toString()).stream()
                .map(this::toDomain).toList();
    }

    /** 持久化记录 → 领域对象。 */
    private PaymentOrder toDomain(PaymentOrderRecord rec) {
        return PaymentOrder.rehydrate(
                UUID.fromString(rec.getId()),
                UUID.fromString(rec.getUserId()),
                rec.getOrderNo(),
                rec.getProductType(),
                rec.getProductKey(),
                rec.getAmountFen(),
                rec.getCurrency(),
                rec.getPaymentChannel(),
                rec.getStatus(),
                rec.getPaidAt(),
                rec.getChannelOrderNo(),
                rec.getRawCallback(),
                rec.getCreatedAt(),
                rec.getUpdatedAt()
        );
    }

    /** 领域对象 → 持久化记录。 */
    private PaymentOrderRecord toRecord(PaymentOrder order) {
        PaymentOrderRecord rec = new PaymentOrderRecord();
        rec.setId(order.id().toString());
        rec.setUserId(order.userId().toString());
        rec.setOrderNo(order.orderNo());
        rec.setProductType(order.productType());
        rec.setProductKey(order.productKey());
        rec.setAmountFen(order.amountFen());
        rec.setCurrency(order.currency());
        rec.setPaymentChannel(order.paymentChannel());
        rec.setStatus(order.status());
        rec.setPaidAt(order.paidAt());
        rec.setChannelOrderNo(order.channelOrderNo());
        rec.setRawCallback(order.rawCallback());
        rec.setCreatedAt(order.createdAt());
        rec.setUpdatedAt(order.updatedAt());
        return rec;
    }
}
