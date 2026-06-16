package com.czx.wenshu.interfaces.rest.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.application.user.SecurityAlertEmailSender;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/**
 * 支付订单（P9-03）与团队管理（P9-07）集成测试。
 *
 * <p>覆盖场景：</p>
 * <ol>
 *   <li>POST /subscriptions/checkout — 未鉴权 401</li>
 *   <li>POST /subscriptions/checkout — 创建订阅订单返回 orderNo 和 payUrl</li>
 *   <li>POST /subscriptions/topup — 创建字数包订单</li>
 *   <li>POST /webhook/payment — 处理支付回调（占位验签）</li>
 *   <li>POST /teams — 未鉴权 401</li>
 *   <li>POST /teams — 创建团队返回团队信息</li>
 *   <li>GET /teams — 查询我的团队列表（含刚创建的团队）</li>
 *   <li>POST /teams/{id}/invites — 邀请成员（返回 inviteCode）</li>
 *   <li>POST /teams/invites/{code}/accept — 第二用户接受邀请成功</li>
 *   <li>GET /teams/{id}/members — 查询成员列表（含 2 人）</li>
 * </ol>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/users-test-schema.sql")
class PaymentAndTeamTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private String tokenA;   // 团队创建者（admin）
    private String tokenB;   // 被邀请用户
    private String userBId;

    @TestConfiguration
    static class MailConfig {
        @Bean @Primary VerificationEmailSender v() { return (e, t, x) -> {}; }
        @Bean @Primary PasswordResetEmailSender p() { return (e, t, x) -> {}; }
        @Bean @Primary SecurityAlertEmailSender s() { return (e, a, d, t) -> {}; }
    }

    @BeforeEach
    void setUp() {
        tokenA = register("payment-a@example.com", "password123", "用户A");
        tokenB = register("payment-b@example.com", "password123", "用户B");
        // 获取用户 B 的 ID
        ResponseEntity<Map> me = restTemplate.exchange("/api/v1/user/me", HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenB)), Map.class);
        Map<String, Object> data = (Map<String, Object>) me.getBody().get("data");
        userBId = (String) data.get("id");
    }

    // ── P9-03：支付订单 ──────────────────────────────────────────────────────

    @Test
    void checkout_withoutToken_returns401() {
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/subscriptions/checkout",
                Map.of("planKey", "pro"),
                Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void checkout_proplan_returnsOrderInfo() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/subscriptions/checkout", HttpMethod.POST,
                new HttpEntity<>(Map.of("planKey", "pro"), authHeaders(tokenA)), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("orderNo").toString()).startsWith("WS");
        assertThat(data.get("status")).isEqualTo("pending");
        assertThat(data.get("payUrl").toString()).contains("PAYMENT_NOT_CONFIGURED");
    }

    @Test
    void topup_returns200() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/subscriptions/topup", HttpMethod.POST,
                new HttpEntity<>(Map.of("topupKey", "topup_100k"), authHeaders(tokenA)), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("productType")).isEqualTo("topup");
    }

    @Test
    void webhook_withOrderNo_processesPaid() {
        // 先创建订单
        ResponseEntity<Map> checkoutResp = restTemplate.exchange(
                "/api/v1/subscriptions/checkout", HttpMethod.POST,
                new HttpEntity<>(Map.of("planKey", "pro"), authHeaders(tokenA)), Map.class);
        String orderNo = ((Map<String, Object>) checkoutResp.getBody().get("data")).get("orderNo").toString();

        // 触发回调
        String url = "/api/v1/webhook/payment?channel=wechat&orderNo=" + orderNo + "&channelNo=WX123456";
        ResponseEntity<Map> webhookResp = restTemplate.postForEntity(url, "{}", Map.class);
        assertThat(webhookResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) webhookResp.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("success");
    }

    // ── P9-07：团队管理 ──────────────────────────────────────────────────────

    @Test
    void createTeam_withoutToken_returns401() {
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/teams", Map.of("name", "测试团队"), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createTeam_validRequest_returnsTeamInfo() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/teams", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "写作团队"), authHeaders(tokenA)), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("name")).isEqualTo("写作团队");
        assertThat(((Number) data.get("memberCount")).intValue()).isEqualTo(1);
    }

    @Test
    void listMyTeams_afterCreate_returnsOne() {
        // 创建团队
        restTemplate.exchange("/api/v1/teams", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "我的团队"), authHeaders(tokenA)), Map.class);

        // 查询列表
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/teams", HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenA)), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> teams = (List<?>) resp.getBody().get("data");
        assertThat(teams).hasSize(1);
    }

    @Test
    void inviteAndAccept_fullFlow_memberCountTwo() {
        // 创建团队
        ResponseEntity<Map> createResp = restTemplate.exchange(
                "/api/v1/teams", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "全流程团队"), authHeaders(tokenA)), Map.class);
        String teamId = ((Map<String, Object>) createResp.getBody().get("data")).get("id").toString();

        // 邀请用户 B
        ResponseEntity<Map> inviteResp = restTemplate.exchange(
                "/api/v1/teams/" + teamId + "/invites", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", userBId), authHeaders(tokenA)), Map.class);
        assertThat(inviteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String inviteCode = ((Map<String, Object>) inviteResp.getBody().get("data")).get("inviteCode").toString();
        assertThat(inviteCode).startsWith("INV");

        // 用户 B 接受邀请
        ResponseEntity<Map> acceptResp = restTemplate.exchange(
                "/api/v1/teams/invites/" + inviteCode + "/accept", HttpMethod.POST,
                new HttpEntity<>(authHeaders(tokenB)), Map.class);
        assertThat(acceptResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<String, Object>) acceptResp.getBody().get("data")).get("status")).isEqualTo("active");

        // 查询成员列表（应有 2 人 active）
        ResponseEntity<Map> membersResp = restTemplate.exchange(
                "/api/v1/teams/" + teamId + "/members", HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenA)), Map.class);
        List<?> members = (List<?>) membersResp.getBody().get("data");
        long activeCount = members.stream()
                .filter(m -> "active".equals(((Map<?, ?>) m).get("status")))
                .count();
        assertThat(activeCount).isEqualTo(2);
    }

    // ── 工具方法 ─────────────────────────────────────────────────────────────

    private String register(String email, String password, String nickname) {
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/auth/register",
                Map.of("email", email, "password", password, "nickname", nickname),
                Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        return (String) data.get("accessToken");
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
