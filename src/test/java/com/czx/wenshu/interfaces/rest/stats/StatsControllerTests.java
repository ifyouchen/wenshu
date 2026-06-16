package com.czx.wenshu.interfaces.rest.stats;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.application.user.SecurityAlertEmailSender;
import com.czx.wenshu.domain.user.EmailAddress;
import java.time.Instant;
import java.util.ArrayList;
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
class StatsControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CapturingVerificationEmailSender verificationEmailSender;

    private String accessToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        Map<String, String> registerRequest = Map.of(
                "email", "writer-stats@example.com",
                "password", "password123",
                "nickname", "统计员",
                "verificationCode", sendRegisterCode("writer-stats@example.com")
        );
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", registerRequest, Map.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) registerResponse.getBody().get("data");
        accessToken = (String) data.get("accessToken");

        // 创建作品 + 卷 + 章节并写入内容（触发写作统计）
        ResponseEntity<Map> projectResp = restTemplate.exchange(
                "/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "统计测试作品"), authHeaders()), Map.class);
        projectId = ((Map<String, Object>) projectResp.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> volumeResp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/volumes", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "第一卷", "sortOrder", 0), authHeaders()), Map.class);
        String volumeId = ((Map<String, Object>) volumeResp.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> chapterResp = restTemplate.exchange(
                "/api/v1/volumes/" + volumeId + "/chapters", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "第一章", "sortOrder", 0), authHeaders()), Map.class);
        String chapterId = ((Map<String, Object>) chapterResp.getBody().get("data")).get("id").toString();

        // 写入内容，产生写作统计数据（100 个汉字）
        String content = "这是测试内容".repeat(20);  // ~120 chars
        restTemplate.exchange("/api/v1/chapters/" + chapterId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("title", "第一章", "content", content, "status", "draft"),
                        authHeaders()), Map.class);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String sendRegisterCode(String email) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register/code",
                Map.of("email", email),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return verificationEmailSender.sentTokens().getLast().rawToken();
    }

    // ── P4-07 写作统计总览 ─────────────────────────────────────────────────

    @Test
    void getOverviewReturnsTodayStats() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/stats/writing", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat((Integer) data.get("todayChars")).isGreaterThan(0);
        assertThat(data.get("todayGoal")).isEqualTo(2000);
        assertThat((List<?>) data.get("trend")).hasSize(7);
    }

    @Test
    void getOverviewCalculatesStreak() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/stats/writing", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        // 今天有写作，streak >= 1
        assertThat((Integer) data.get("streak")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void statsWithoutAuthReturns401() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/stats/writing", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── P4-08 热力图 ───────────────────────────────────────────────────────

    @Test
    void getHeatmapReturns365Days() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/stats/writing/heatmap", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(data).hasSize(365);
        // 今天的条目字数 > 0
        Map<String, Object> todayEntry = data.get(data.size() - 1);
        assertThat((Integer) todayEntry.get("chars")).isGreaterThan(0);
    }

    @Test
    void getProjectProgressIncludesProject() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/stats/writing/projects", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(data).hasSize(1);
        assertThat(data.get(0).get("title")).isEqualTo("统计测试作品");
        assertThat((Integer) data.get(0).get("todayChars")).isGreaterThan(0);
    }

    @Test
    void getMonthlySummaryReturnsCurrentMonth() {
        // 获取当前年月（格式 yyyy-MM）
        String today = java.time.LocalDate.now().toString();
        String yearMonth = today.substring(0, 7);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/stats/writing/monthly/" + yearMonth, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("yearMonth")).isEqualTo(yearMonth);
        assertThat((Integer) data.get("totalChars")).isGreaterThan(0);
        assertThat((Integer) data.get("activeDays")).isGreaterThanOrEqualTo(1);
    }

    // ── P4-09 每日目标设置 ─────────────────────────────────────────────────

    @Test
    void updateGlobalWritingGoalChangesGoal() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user/writing-goal", HttpMethod.PUT,
                new HttpEntity<>(Map.of("dailyCharGoal", 3000), authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("dailyCharGoal")).isEqualTo(3000);
    }

    @Test
    void updatedGoalReflectsInOverview() {
        // 先改目标
        restTemplate.exchange("/api/v1/user/writing-goal", HttpMethod.PUT,
                new HttpEntity<>(Map.of("dailyCharGoal", 5000), authHeaders()), Map.class);

        // 再查统计总览
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/stats/writing", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("todayGoal")).isEqualTo(5000);
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

        private final List<SentToken> sentTokens = new ArrayList<>();

        @Override
        public void sendVerificationEmail(EmailAddress email, String rawToken, Instant expiresAt) {
            sentTokens.add(new SentToken(email, rawToken, expiresAt));
        }

        List<SentToken> sentTokens() {
            return sentTokens;
        }
    }

    record SentToken(EmailAddress email, String rawToken, Instant expiresAt) {
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
