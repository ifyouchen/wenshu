package com.czx.wenshu.interfaces.rest.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.application.user.SecurityAlertEmailSender;
import com.czx.wenshu.domain.task.AsyncTask;
import com.czx.wenshu.domain.task.AsyncTaskRepository;
import com.czx.wenshu.domain.user.EmailAddress;
import java.time.Instant;
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
class TaskControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AsyncTaskService asyncTaskService;

    private String accessToken;
    private UUID userId;

    @BeforeEach
    void setUp() {
        Map<String, String> registerRequest = Map.of(
                "email", "taskuser@example.com",
                "password", "password123",
                "nickname", "任务用户"
        );
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", registerRequest, Map.class);
        Map<String, Object> data = (Map<String, Object>) registerResponse.getBody().get("data");
        accessToken = (String) data.get("accessToken");
        // userId will be resolved through the service in tests
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ── P5-03 任务进度查询 ─────────────────────────────────────────────────

    @Test
    void getProgressReturnsNotFoundForUnknownTask() {
        UUID unknownId = UUID.randomUUID();
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/tasks/" + unknownId + "/progress",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getProgressWithoutAuthReturns401() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/tasks/" + UUID.randomUUID() + "/progress", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createdTaskStartsAsPending() {
        // 获取当前用户 ID（通过 /user/me）
        ResponseEntity<Map> meResp = restTemplate.exchange(
                "/api/v1/user/me", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        String userIdStr = ((Map<String, Object>) meResp.getBody().get("data")).get("id").toString();
        UUID uid = UUID.fromString(userIdStr);

        AsyncTask task = asyncTaskService.createTask(uid, null, "novel_skeleton");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/tasks/" + task.id() + "/progress",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("pending");
        assertThat(data.get("progressPct")).isEqualTo(0);
        assertThat(data.get("taskType")).isEqualTo("novel_skeleton");
    }

    @Test
    void taskProgressUpdatesCorrectly() {
        ResponseEntity<Map> meResp = restTemplate.exchange(
                "/api/v1/user/me", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        String userIdStr = ((Map<String, Object>) meResp.getBody().get("data")).get("id").toString();
        UUID uid = UUID.fromString(userIdStr);

        AsyncTask task = asyncTaskService.createTask(uid, null, "novel_skeleton");
        asyncTaskService.markRunning(task.id(), 5, "初始化");
        asyncTaskService.updateProgress(task.id(), 2, "生成卷结构", 40);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/tasks/" + task.id() + "/progress",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("running");
        assertThat(data.get("progressPct")).isEqualTo(40);
        assertThat(data.get("stepLabel")).isEqualTo("生成卷结构");
        assertThat(data.get("currentStep")).isEqualTo(2);
        assertThat(data.get("totalSteps")).isEqualTo(5);
    }

    @Test
    void completedTaskShowsCompletedStatus() {
        ResponseEntity<Map> meResp = restTemplate.exchange(
                "/api/v1/user/me", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        String userIdStr = ((Map<String, Object>) meResp.getBody().get("data")).get("id").toString();
        UUID uid = UUID.fromString(userIdStr);

        AsyncTask task = asyncTaskService.createTask(uid, null, "novel_skeleton");
        UUID resultId = UUID.randomUUID();
        asyncTaskService.complete(task.id(), resultId);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/tasks/" + task.id() + "/progress",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("completed");
        assertThat(data.get("progressPct")).isEqualTo(100);
        assertThat(data.get("resultId")).isEqualTo(resultId.toString());
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
