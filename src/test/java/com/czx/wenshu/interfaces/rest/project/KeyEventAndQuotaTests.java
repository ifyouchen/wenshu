package com.czx.wenshu.interfaces.rest.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.application.llm.EmbeddingClient;
import com.czx.wenshu.application.user.SecurityAlertEmailSender;
import com.czx.wenshu.domain.user.EmailAddress;
import com.czx.wenshu.infrastructure.llm.NoopEmbeddingClient;
import java.time.Instant;
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

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/users-test-schema.sql")
class KeyEventAndQuotaTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;
    private String projectId;
    private String chapterId;

    @BeforeEach
    void setUp() {
        Map<String, String> registerReq = Map.of("email", "kevevent-test@example.com",
                "password", "password123", "nickname", "测试员");
        ResponseEntity<Map> regResp = restTemplate.postForEntity("/api/v1/auth/register", registerReq, Map.class);
        Map<String, Object> data = (Map<String, Object>) regResp.getBody().get("data");
        accessToken = (String) data.get("accessToken");

        ResponseEntity<Map> projectResp = restTemplate.exchange("/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "关键事件测试作品"), authHeaders()), Map.class);
        projectId = ((Map<String, Object>) projectResp.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> volumeResp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/volumes", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "第一卷", "sortOrder", 0), authHeaders()), Map.class);
        String volumeId = ((Map<String, Object>) volumeResp.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> chapterResp = restTemplate.exchange(
                "/api/v1/volumes/" + volumeId + "/chapters", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "第一章", "sortOrder", 0), authHeaders()), Map.class);
        chapterId = ((Map<String, Object>) chapterResp.getBody().get("data")).get("id").toString();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ── P6-03 关键事件时间线 ───────────────────────────────────────────────

    @Test
    void extractKeyEventsRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/chapters/" + chapterId + "/key-events", null, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void extractKeyEventsOnEmptyChapterReturns400() {
        // 章节内容为空，应返回 400
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/chapters/" + chapterId + "/key-events", HttpMethod.POST,
                new HttpEntity<>(authHeaders()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString()).contains("内容为空");
    }

    @Test
    void extractKeyEventsWithContentCreatesTask() {
        // 写入章节内容
        restTemplate.exchange("/api/v1/chapters/" + chapterId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("title", "第一章", "content",
                        "张三与陈明发生了激烈冲突，最终张三做出了关键抉择，离开了师门。", "status", "draft"),
                        authHeaders()), Map.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/chapters/" + chapterId + "/key-events", HttpMethod.POST,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("taskId")).isNotNull();
    }

    @Test
    void getKeyEventsReturnsEmptyInitially() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/chapters/" + chapterId + "/key-events", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> events = (List<?>) response.getBody().get("data");
        assertThat(events).isEmpty();
    }

    @Test
    void getProjectKeyEventsReturnsEmptyInitially() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/key-events", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> events = (List<?>) response.getBody().get("data");
        assertThat(events).isEmpty();
    }

    // ── P6-04 向量嵌入降级验证（单元级，无 H2 VECTOR 支持）────────────────

    @Test
    void noopEmbeddingClientIsUnavailableAndReturnsNull() {
        // 验证 NoopEmbeddingClient 的降级行为
        EmbeddingClient client = new NoopEmbeddingClient();
        assertThat(client.isAvailable()).isFalse();
        assertThat(client.embed("任意文本")).isNull();
        assertThat(client.dimension()).isEqualTo(0);
    }

    // ── P6-05 配额检查 ────────────────────────────────────────────────────

    @Test
    void getQuotaRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/user/quota", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getQuotaReturnsFreeTierLimits() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user/quota", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("limitChars")).isEqualTo(100000);
        assertThat(data.get("limitAdaptations")).isEqualTo(5);
        assertThat(data.get("usedChars")).isEqualTo(0);
        assertThat(data.get("usedAdaptations")).isEqualTo(0);
        assertThat(data.get("remainingChars")).isEqualTo(100000);
        assertThat(data.get("remainingAdaptations")).isEqualTo(5);
    }

    @TestConfiguration
    static class TestConfig {

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
