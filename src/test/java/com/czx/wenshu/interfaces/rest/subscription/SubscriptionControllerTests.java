package com.czx.wenshu.interfaces.rest.subscription;

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
 * 订阅套餐接口集成测试（P9-01/P9-02）。
 *
 * <p>覆盖场景：</p>
 * <ol>
 *   <li>获取套餐列表（无需鉴权）</li>
 *   <li>套餐列表包含 free/pro/enterprise 三档</li>
 *   <li>获取当前订阅需要鉴权（401）</li>
 *   <li>已登录用户首次获取当前订阅 → 自动创建免费套餐</li>
 *   <li>当前订阅包含配额信息（limitChars = 100000 for free）</li>
 * </ol>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/users-test-schema.sql")
class SubscriptionControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;

    /** 邮件发送捕获（测试环境替换 SMTP，bean 方法名使用 capturing* 前缀避免与 LoggingMailConfig 的 bean 名称冲突）。 */
    @TestConfiguration
    static class MailTestConfig {
        @Bean @Primary
        VerificationEmailSender capturingSubVerificationEmailSender() { return (email, token, expiresAt) -> {}; }
        @Bean @Primary
        PasswordResetEmailSender capturingSubPasswordResetEmailSender() { return (email, token, expiresAt) -> {}; }
        @Bean @Primary
        SecurityAlertEmailSender capturingSubSecurityAlertEmailSender() { return (email, alertType, alertDetail, alertTime) -> {}; }
    }

    @BeforeEach
    void setUp() {
        // 注册并登录，获取访问令牌
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/v1/auth/register",
                Map.of("email", "sub-test@example.com", "password", "password123", "nickname", "订阅用户"),
                Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        accessToken = (String) data.get("accessToken");
    }

    // ── 1. 套餐列表（无需鉴权）──────────────────────────────────────────────

    @Test
    void getPlans_returnsAllActivePlans() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/v1/subscriptions/plans", Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> plans = (List<Map<String, Object>>) resp.getBody().get("data");
        assertThat(plans).hasSizeGreaterThanOrEqualTo(3);

        // 必须包含 free/pro/enterprise 三档
        List<String> planKeys = plans.stream()
                .map(p -> (String) p.get("planKey"))
                .toList();
        assertThat(planKeys).contains("free", "pro", "enterprise");
    }

    @Test
    void getPlans_freePlanHasCorrectLimits() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/v1/subscriptions/plans", Map.class);

        List<Map<String, Object>> plans = (List<Map<String, Object>>) resp.getBody().get("data");
        Map<String, Object> free = plans.stream()
                .filter(p -> "free".equals(p.get("planKey")))
                .findFirst()
                .orElseThrow();

        assertThat(((Number) free.get("monthlyCharLimit")).longValue()).isEqualTo(100_000L);
        assertThat(((Number) free.get("monthlyAdaptationLimit")).intValue()).isEqualTo(5);
        assertThat(((Number) free.get("pricePerMonth")).doubleValue()).isEqualTo(0.0);
    }

    @Test
    void getPlans_proPlanHasHigherLimits() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/v1/subscriptions/plans", Map.class);

        List<Map<String, Object>> plans = (List<Map<String, Object>>) resp.getBody().get("data");
        Map<String, Object> pro = plans.stream()
                .filter(p -> "pro".equals(p.get("planKey")))
                .findFirst()
                .orElseThrow();

        assertThat(((Number) pro.get("monthlyCharLimit")).longValue()).isGreaterThan(100_000L);
        assertThat(((Number) pro.get("monthlyAdaptationLimit")).intValue()).isGreaterThan(5);
    }

    // ── 2. 当前订阅（需鉴权）──────────────────────────────────────────────

    @Test
    void getCurrentSubscription_withoutToken_returns401() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/v1/subscriptions/current", Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCurrentSubscription_newUser_returnsFreePlan() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/subscriptions/current", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");

        assertThat(data.get("planKey")).isEqualTo("free");
        assertThat(data.get("status")).isEqualTo("active");
        assertThat(data.get("expiresAt")).isNull();  // 免费版永不过期
    }

    @Test
    void getCurrentSubscription_includesQuotaInfo() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/subscriptions/current", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        Map<String, Object> quota = (Map<String, Object>) data.get("quota");

        assertThat(quota).isNotNull();
        assertThat(((Number) quota.get("limitChars")).longValue()).isEqualTo(100_000L);
        assertThat(((Number) quota.get("limitAdaptations")).intValue()).isEqualTo(5);
        assertThat(((Number) quota.get("usedChars")).longValue()).isZero();
    }

    // ── 辅助方法 ─────────────────────────────────────────────────────────

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
