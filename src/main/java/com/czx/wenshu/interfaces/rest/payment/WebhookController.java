package com.czx.wenshu.interfaces.rest.payment;

import com.czx.wenshu.application.payment.PaymentService;
import com.czx.wenshu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付回调接口（P9-03）。
 *
 * <p>接收微信/支付宝支付完成后的异步回调通知，验证签名后更新订单状态。</p>
 *
 * <p>验签要点（生产实现）：</p>
 * <ul>
 *   <li>微信 v3：验证 Wechatpay-Signature-Type + 平台证书公钥</li>
 *   <li>支付宝：验证 RSA2/RSA 签名，使用支付宝公钥</li>
 * </ul>
 *
 * <p>当前为占位实现：验签始终通过，不需要真实渠道凭据。</p>
 */
@Tag(name = "Webhook", description = "支付渠道回调通知处理")
@RestController
@RequestMapping("/api/v1/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final PaymentService paymentService;

    public WebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * 处理支付回调（P9-03）。
     *
     * <p>微信/支付宝在支付完成后，POST 到此端点。
     * 无需用户鉴权（通过签名验证渠道来源合法性）。</p>
     *
     * @param channel    支付渠道（query param：wechat / alipay）
     * @param orderNo    系统订单号（query param）
     * @param channelNo  渠道订单号（query param）
     * @param rawPayload 原始回调 Body（用于验签）
     * @return 处理结果（微信要求 200 + "SUCCESS"，支付宝要求 "success"）
     */
    @Operation(summary = "支付回调处理（P9-03）",
               description = "验证渠道签名后更新订单状态。生产端替换为真实渠道 SDK 验签逻辑。")
    @PostMapping(value = "/payment", consumes = MediaType.ALL_VALUE)
    public Result<Map<String, String>> handlePaymentCallback(
            @RequestParam(defaultValue = "wechat") String channel,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String channelNo,
            @RequestBody(required = false) String rawPayload) {

        log.info("[WebhookController] 收到支付回调 channel={} orderNo={} channelNo={}",
                channel, orderNo, channelNo);

        if (orderNo == null || orderNo.isBlank()) {
            log.warn("[WebhookController] 回调缺少 orderNo，忽略");
            return Result.ok(Map.of("status", "ignored", "reason", "missing orderNo"));
        }

        String result = paymentService.processWebhook(
                channel, orderNo, channelNo, rawPayload != null ? rawPayload : "");
        log.info("[WebhookController] 支付回调处理完成 orderNo={} result={}", orderNo, result);
        return Result.ok(Map.of("status", result));
    }
}
