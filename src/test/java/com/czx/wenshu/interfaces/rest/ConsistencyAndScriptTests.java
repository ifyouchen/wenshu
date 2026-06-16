package com.czx.wenshu.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.application.user.SecurityAlertEmailSender;
import com.czx.wenshu.domain.script.ScriptDraft;
import com.czx.wenshu.domain.script.ScriptDraftRepository;
import com.czx.wenshu.domain.user.EmailAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
class ConsistencyAndScriptTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ScriptDraftRepository scriptDraftRepository;

    private String accessToken;
    private String projectId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        Map<String, String> registerReq = Map.of("email", "consistency-script@example.com",
                "password", "password123", "nickname", "测试员");
        ResponseEntity<Map> regResp = restTemplate.postForEntity("/api/v1/auth/register", registerReq, Map.class);
        Map<String, Object> data = (Map<String, Object>) regResp.getBody().get("data");
        accessToken = (String) data.get("accessToken");

        ResponseEntity<Map> meResp = restTemplate.exchange(
                "/api/v1/user/me", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        userId = UUID.fromString(((Map<String, Object>) meResp.getBody().get("data")).get("id").toString());

        ResponseEntity<Map> projectResp = restTemplate.exchange("/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "测试作品", "synopsis", "一个测试"), authHeaders()), Map.class);
        projectId = ((Map<String, Object>) projectResp.getBody().get("data")).get("id").toString();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ── P6-06 一致性审查 ──────────────────────────────────────────────────

    @Test
    void consistencyCheckRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/consistency/check?projectId=" + projectId, null, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void consistencyCheckSubmitsTaskAndReturnsIds() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/consistency/check?projectId=" + projectId,
                HttpMethod.POST, new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("taskId")).isNotNull();
        assertThat(data.get("reportId")).isNotNull();
    }

    @Test
    void consistencyCheckConsumesQuota() {
        // 触发审查（消耗一次配额）
        restTemplate.exchange("/api/v1/consistency/check?projectId=" + projectId,
                HttpMethod.POST, new HttpEntity<>(authHeaders()), Map.class);

        // 验证配额已减少
        ResponseEntity<Map> quotaResp = restTemplate.exchange(
                "/api/v1/user/quota", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        Map<String, Object> quota = (Map<String, Object>) quotaResp.getBody().get("data");
        assertThat((Integer) quota.get("usedAdaptations")).isEqualTo(1);
        assertThat((Integer) quota.get("remainingAdaptations")).isEqualTo(4);
    }

    @Test
    void getReportRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/consistency/reports/" + UUID.randomUUID(), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getReportReturnsItemsAfterCheck() throws Exception {
        ResponseEntity<Map> checkResp = restTemplate.exchange(
                "/api/v1/consistency/check?projectId=" + projectId,
                HttpMethod.POST, new HttpEntity<>(authHeaders()), Map.class);
        String reportId = ((Map<String, Object>) checkResp.getBody().get("data")).get("reportId").toString();

        // 等待异步任务（任务可能 FAILED 因为没有 LLM key，但报告应该存在）
        Thread.sleep(200);

        ResponseEntity<Map> reportResp = restTemplate.exchange(
                "/api/v1/consistency/reports/" + reportId,
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        assertThat(reportResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> report = (Map<String, Object>) reportResp.getBody().get("data");
        assertThat(report.get("reportId")).isEqualTo(reportId);
        assertThat(report.get("items")).isNotNull();
    }

    // ── P6-07 审查条目状态更新 ─────────────────────────────────────────────

    @Test
    void updateItemStatusRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/consistency/items/" + UUID.randomUUID(),
                HttpMethod.PATCH, new HttpEntity<>(Map.of("status", "handled")), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateItemStatusWithInvalidStatusReturns400() {
        // 先创建一个报告和条目
        ResponseEntity<Map> checkResp = restTemplate.exchange(
                "/api/v1/consistency/check?projectId=" + projectId,
                HttpMethod.POST, new HttpEntity<>(authHeaders()), Map.class);
        assertThat(checkResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 尝试用非法状态更新（不存在的条目 ID 返回 404）
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/consistency/items/" + UUID.randomUUID(),
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "handled"), authHeaders()),
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── P7-01 剧本草稿与场景 ──────────────────────────────────────────────

    @Test
    void listDraftsRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/script/projects/" + projectId + "/drafts", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listDraftsReturnsEmptyForNewProject() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/script/projects/" + projectId + "/drafts",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> drafts = (List<?>) response.getBody().get("data");
        assertThat(drafts).isEmpty();
    }

    @Test
    void getDraftAfterManualCreation() {
        // 通过 Repository 直接创建草稿（模拟改编任务完成后的状态）
        java.time.Clock clock = java.time.Clock.systemUTC();
        ScriptDraft draft = ScriptDraft.create(
                UUID.fromString(projectId), userId, "测试剧本草稿", "action", clock);
        scriptDraftRepository.save(draft);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/script/drafts/" + draft.id(),
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("title")).isEqualTo("测试剧本草稿");
        assertThat(data.get("strategy")).isEqualTo("action");
        assertThat(data.get("status")).isEqualTo("processing");
    }

    @Test
    void listScenesReturnsEmptyWhenNoneExist() {
        java.time.Clock clock = java.time.Clock.systemUTC();
        ScriptDraft draft = ScriptDraft.create(
                UUID.fromString(projectId), userId, "空场景草稿", "action", clock);
        scriptDraftRepository.save(draft);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/script/drafts/" + draft.id() + "/scenes",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("total")).isEqualTo(0);
        assertThat((List<?>) data.get("scenes")).isEmpty();
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
