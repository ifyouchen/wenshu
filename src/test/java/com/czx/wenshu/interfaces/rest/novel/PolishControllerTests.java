package com.czx.wenshu.interfaces.rest.novel;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.application.user.SecurityAlertEmailSender;
import com.czx.wenshu.domain.user.EmailAddress;
import java.time.Instant;
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

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/users-test-schema.sql")
class PolishControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;
    private String chapterId;

    @BeforeEach
    void setUp() {
        Map<String, String> registerReq = Map.of("email", "polish-user@example.com",
                "password", "password123", "nickname", "润色者");
        ResponseEntity<Map> regResp = restTemplate.postForEntity("/api/v1/auth/register", registerReq, Map.class);
        Map<String, Object> data = (Map<String, Object>) regResp.getBody().get("data");
        accessToken = (String) data.get("accessToken");

        // 创建作品/卷/章节用于 P5-07/P5-08 测试
        ResponseEntity<Map> projectResp = restTemplate.exchange("/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "润色测试"), authHeaders()), Map.class);
        String projectId = ((Map<String, Object>) projectResp.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> volumeResp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/volumes", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "第一卷", "sortOrder", 0), authHeaders()), Map.class);
        String volumeId = ((Map<String, Object>) volumeResp.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> chapterResp = restTemplate.exchange(
                "/api/v1/volumes/" + volumeId + "/chapters", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "第一章", "sortOrder", 0), authHeaders()), Map.class);
        chapterId = ((Map<String, Object>) chapterResp.getBody().get("data")).get("id").toString();
        restTemplate.exchange("/api/v1/chapters/" + chapterId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("title", "第一章", "content", "张三走进了客栈，环顾四周。", "status", "draft"),
                        authHeaders()), Map.class);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ── P5-09 Polish endpoints ─────────────────────────────────────────────

    @Test
    void basicCorrectionRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/polish/basic",
                Map.of("text", "这是一段文本"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void advancedPolishRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/polish/advanced",
                Map.of("text", "这是一段文本"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void styleRewriteRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/polish/style",
                Map.of("text", "这是一段文本", "styleDescription", "武侠风格"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void basicCorrectionWithNoApiKeyReturns400() {
        // UnconfiguredLlmClient 抛出 ApiException(BAD_REQUEST)
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/polish/basic", HttpMethod.POST,
                new HttpEntity<>(Map.of("text", "这是一段需要校正的文本"), authHeaders()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> body = response.getBody();
        assertThat(body.get("message").toString()).contains("API Key 未配置");
    }

    @Test
    void advancedPolishWithNoApiKeyReturns400() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/polish/advanced", HttpMethod.POST,
                new HttpEntity<>(Map.of("text", "这是一段需要润色的文本"), authHeaders()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void styleRewriteWithNoApiKeyReturns400() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/polish/style", HttpMethod.POST,
                new HttpEntity<>(Map.of("text", "这是一段文本", "styleDescription", "武侠风格"), authHeaders()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── P5-08 Branch endpoint ──────────────────────────────────────────────

    @Test
    void branchSuggestionsRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/novel/branch",
                Map.of("chapterId", chapterId, "branchCount", 3), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void branchSuggestionsWithNoApiKeyReturns400() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/novel/branch", HttpMethod.POST,
                new HttpEntity<>(Map.of("chapterId", chapterId, "branchCount", 3), authHeaders()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString()).contains("API Key 未配置");
    }

    // ── P5-07 SSE continue endpoint ────────────────────────────────────────

    @Test
    void sseEndpointRequiresAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/novel/continue?chapterId=" + chapterId, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void sseEndpointWithNoApiKeyReturnsErrorEvent() {
        // 无 API Key 时 UnconfiguredStreamingLlmClient 立即触发 onError
        // SSE 响应会包含 error 事件
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        // 不设置 Accept 让 SSE content-type 原样返回
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/novel/continue?chapterId=" + chapterId,
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // 连接应该正常完成（emitter.complete() 被调用）
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 响应体包含 error 事件
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).contains("error");
    }

    @TestConfiguration
    static class EmailSenderTestConfig {

        @Bean
        @Primary
        CapturingVerificationEmailSender capturingVerificationEmailSender() {
            return new CapturingVerificationEmailSender();
        }

        @Bean
        @Primary
        CapturingPasswordResetEmailSender capturingPasswordResetEmailSender() {
            return new CapturingPasswordResetEmailSender();
        }

        @Bean
        @Primary
        CapturingSecurityAlertEmailSender capturingSecurityAlertEmailSender() {
            return new CapturingSecurityAlertEmailSender();
        }
    }

    static class CapturingVerificationEmailSender implements VerificationEmailSender {
        @Override
        public void sendVerificationEmail(EmailAddress email, String rawToken, Instant expiresAt) {}
    }

    static class CapturingPasswordResetEmailSender implements PasswordResetEmailSender {
        @Override
        public void sendPasswordResetEmail(EmailAddress email, String rawToken, Instant expiresAt) {}
    }

    static class CapturingSecurityAlertEmailSender implements SecurityAlertEmailSender {
        @Override
        public void sendSecurityAlertEmail(EmailAddress email, String alertType, String alertDetail, String alertTime) {}
    }
}
