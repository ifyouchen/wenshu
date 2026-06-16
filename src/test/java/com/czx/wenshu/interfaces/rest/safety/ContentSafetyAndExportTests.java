package com.czx.wenshu.interfaces.rest.safety;

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
 * 内容安全（P9-05）、数据导出（P9-04）、合规策略（P9-06）集成测试。
 *
 * <p>覆盖场景：</p>
 * <ol>
 *   <li>GET /content/policy — 无需鉴权，返回策略内容</li>
 *   <li>GET /content/policy — 包含 copyright 和 aiAssisted 字段（P9-06）</li>
 *   <li>POST /content/appeals — 未鉴权返回 401</li>
 *   <li>POST /content/appeals — 已登录用户提交申诉成功</li>
 *   <li>GET /content/appeals — 查询申诉列表</li>
 *   <li>POST /user/data/export — 未鉴权返回 401（P9-04）</li>
 *   <li>POST /user/data/export — 已登录用户提交导出任务返回 taskId（P9-04）</li>
 *   <li>POST /content/appeals — 内容为空返回 400 校验错误</li>
 * </ol>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/users-test-schema.sql")
class ContentSafetyAndExportTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;

    /** 邮件 Bean 替换，避免 BeanDefinitionOverrideException。 */
    @TestConfiguration
    static class MailConfig {
        @Bean @Primary
        VerificationEmailSender capturingVerif() { return (e, t, x) -> {}; }
        @Bean @Primary
        PasswordResetEmailSender capturingReset() { return (e, t, x) -> {}; }
        @Bean @Primary
        SecurityAlertEmailSender capturingSecurity() { return (e, a, d, t) -> {}; }
    }

    @BeforeEach
    void setUp() {
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/v1/auth/register",
                Map.of("email", "safety-test@example.com", "password", "password123", "nickname", "安全测试"),
                Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        accessToken = (String) data.get("accessToken");
    }

    // ── P9-06 / P9-05：内容策略 ─────────────────────────────────────────

    @Test
    void getPolicy_noAuthRequired() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/v1/content/policy", Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data).containsKey("safetyLevels");
        assertThat(data).containsKey("copyright");
        assertThat(data).containsKey("privacy");
        assertThat(data).containsKey("appealProcess");
    }

    @Test
    void getPolicy_containsAiAssistAnnotation_P9_06() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/v1/content/policy", Map.class);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        Map<String, Object> copyright = (Map<String, Object>) data.get("copyright");
        assertThat(copyright.get("exportAnnotation").toString()).contains("AI 辅助生成");
    }

    @Test
    void getPolicy_safetyLevels_containsFourLevels() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/v1/content/policy", Map.class);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        List<?> levels = (List<?>) data.get("safetyLevels");
        assertThat(levels).hasSize(4);
    }

    // ── P9-05：申诉接口 ──────────────────────────────────────────────────

    @Test
    void submitAppeal_withoutToken_returns401() {
        Map<String, String> body = Map.of("content", "被过滤的内容", "reason", "我认为没有问题");
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/v1/content/appeals", body, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void submitAppeal_validRequest_returns200() {
        Map<String, String> body = Map.of("content", "被过滤的 AI 输出内容", "reason", "此内容完全合规，属于文学创作");
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/content/appeals", HttpMethod.POST,
                new HttpEntity<>(body, authHeaders()), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data).containsKey("id");
        assertThat(data.get("status")).isEqualTo("pending");
    }

    @Test
    void submitAppeal_emptyContent_returns400() {
        Map<String, String> body = Map.of("content", "", "reason", "某理由");
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/content/appeals", HttpMethod.POST,
                new HttpEntity<>(body, authHeaders()), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listAppeals_afterSubmission_returnsOne() {
        // 先提交一条申诉
        Map<String, String> body = Map.of("content", "申诉列表测试内容", "reason", "合法内容被误判");
        restTemplate.exchange("/api/v1/content/appeals", HttpMethod.POST,
                new HttpEntity<>(body, authHeaders()), Map.class);

        // 查询列表
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/content/appeals", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> appeals = (List<?>) resp.getBody().get("data");
        assertThat(appeals).hasSize(1);
    }

    // ── P9-04：数据导出 ──────────────────────────────────────────────────

    @Test
    void submitDataExport_withoutToken_returns401() {
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/v1/user/data/export", null, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void submitDataExport_loggedIn_returnsTaskId() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/user/data/export", HttpMethod.POST,
                new HttpEntity<>(authHeaders()), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data).containsKey("taskId");
        assertThat((String) data.get("taskId")).isNotBlank();
    }

    // ── 工具方法 ─────────────────────────────────────────────────────────

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
