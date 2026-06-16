package com.czx.wenshu.infrastructure.persistence.payment;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** MyBatis Mapper，对应 payment_orders 表（P9-03）。 */
@Mapper
public interface PaymentOrderMapper {

    /** 查询所有字段（通用 SELECT）。 */
    @Select("""
            SELECT id, user_id, order_no, product_type, product_key,
                   amount_fen, currency, payment_channel, status,
                   paid_at, channel_order_no, raw_callback,
                   created_at, updated_at
            FROM payment_orders WHERE id = CAST(#{id} AS UUID)
            """)
    PaymentOrderRecord findById(@Param("id") String id);

    /** 按系统订单号查询。 */
    @Select("""
            SELECT id, user_id, order_no, product_type, product_key,
                   amount_fen, currency, payment_channel, status,
                   paid_at, channel_order_no, raw_callback,
                   created_at, updated_at
            FROM payment_orders WHERE order_no = #{orderNo}
            """)
    PaymentOrderRecord findByOrderNo(@Param("orderNo") String orderNo);

    /** 按用户 ID 查询，按创建时间倒序。 */
    @Select("""
            SELECT id, user_id, order_no, product_type, product_key,
                   amount_fen, currency, payment_channel, status,
                   paid_at, channel_order_no, raw_callback,
                   created_at, updated_at
            FROM payment_orders WHERE user_id = CAST(#{userId} AS UUID)
            ORDER BY created_at DESC
            """)
    List<PaymentOrderRecord> findByUserId(@Param("userId") String userId);

    /** 插入新订单。 */
    @Insert("""
            INSERT INTO payment_orders
              (id, user_id, order_no, product_type, product_key,
               amount_fen, currency, payment_channel, status,
               paid_at, channel_order_no, raw_callback, created_at, updated_at)
            VALUES
              (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), #{orderNo},
               #{productType}, #{productKey}, #{amountFen}, #{currency},
               #{paymentChannel}, #{status}, #{paidAt}, #{channelOrderNo},
               #{rawCallback}, #{createdAt}, #{updatedAt})
            """)
    void insert(PaymentOrderRecord record);

    /** 更新订单状态、支付信息。 */
    @Update("""
            UPDATE payment_orders
            SET status = #{status}, payment_channel = #{paymentChannel},
                paid_at = #{paidAt}, channel_order_no = #{channelOrderNo},
                raw_callback = #{rawCallback}, updated_at = #{updatedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(PaymentOrderRecord record);
}
