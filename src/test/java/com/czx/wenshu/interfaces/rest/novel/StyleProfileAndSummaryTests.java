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
class StyleProfileAndSummaryTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;
    private String projectId;
    private String chapterId;

    @BeforeEach
    void setUp() {
        Map<String, String> registerReq = Map.of("email", "style-test@example.com",
                "password", "password123", "nickname", "风格测试者");
        ResponseEntity<Map> regResp = restTemplate.postForEntity("/api/v1/auth/register", registerReq, Map.class);
        Map<String, Object> data = (Map<String, Object>) regResp.getBody().get("data");
        accessToken = (String) data.get("accessToken");

        ResponseEntity<Map> projectResp = restTemplate.exchange("/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "风格测试作品"), authHeaders()), Map.class);
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

    // ── P5-10 文风档案 ─────────────────────────────────────────────────────

    @Test
    void getStyleProfileRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/user/style-profile", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getStyleProfileReturnsEmptyForNewUser() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user/style-profile", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("analysisCompleted")).isEqualTo(false);
    }

    @Test
    void saveStyleProfileCreatesTaskId() {
        // 无 LLM Key → 任务创建成功（任务状态会很快 FAILED，但 taskId 存在）
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user/style-profile", HttpMethod.PUT,
                new HttpEntity<>(Map.of("sampleText", "这是我的写作风格样本，节奏明快，词汇简洁。"), authHeaders()),
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("analysisTaskId")).isNotNull();
    }

    @Test
    void deleteStyleProfileRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user/style-profile", HttpMethod.DELETE,
                new HttpEntity<>(new HttpHeaders()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deleteStyleProfileWorks() {
        // 先保存
        restTemplate.exchange("/api/v1/user/style-profile", HttpMethod.PUT,
                new HttpEntity<>(Map.of("sampleText", "写作样本"), authHeaders()), Map.class);

        // 再删除
        ResponseEntity<Map> delResp = restTemplate.exchange(
                "/api/v1/user/style-profile", HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()), Map.class);
        assertThat(delResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 再次 GET，应返回空档案
        ResponseEntity<Map> getResp = restTemplate.exchange(
                "/api/v1/user/style-profile", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        Map<String, Object> data = (Map<String, Object>) getResp.getBody().get("data");
        assertThat(data.get("analysisCompleted")).isEqualTo(false);
    }

    // ── P6-01 章节摘要 ─────────────────────────────────────────────────────

    @Test
    void summarizeRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/chapters/" + chapterId + "/summarize", null, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void summarizeEmptyChapterReturns400() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/chapters/" + chapterId + "/summarize", HttpMethod.POST,
                new HttpEntity<>(authHeaders()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString()).contains("内容为空");
    }

    @Test
    void summarizeChapterWithContentCreatesTask() {
        // 先写入内容
        restTemplate.exchange("/api/v1/chapters/" + chapterId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("title", "第一章", "content", "张三走进了客栈，环顾四周，忽然发现了一个可疑的人影。", "status", "draft"),
                        authHeaders()), Map.class);

        // 提交摘要任务
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/chapters/" + chapterId + "/summarize", HttpMethod.POST,
                new HttpEntity<>(authHeaders()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("taskId")).isNotNull();
    }

    // ── P6-02 角色锚点 ─────────────────────────────────────────────────────

    @Test
    void characterAnchorUpdatedAfterChapterSave() {
        // 创建角色
        ResponseEntity<Map> charResp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/characters", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "张三", "role", "主角"), authHeaders()), Map.class);
        String charId = ((Map<String, Object>) charResp.getBody().get("data")).get("id").toString();

        // 初始 lastActiveChapterId 为 null
        Map<String, Object> initialChar = (Map<String, Object>) restTemplate.exchange(
                "/api/v1/characters/" + charId, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class).getBody().get("data");
        assertThat(initialChar.get("lastActiveChapterId")).isNull();

        // 更新章节内容，包含角色名"张三"
        restTemplate.exchange("/api/v1/chapters/" + chapterId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("title", "第一章", "content", "张三走进了客栈，环顾四周。", "status", "draft"),
                        authHeaders()), Map.class);

        // 验证角色锚点已更新
        Map<String, Object> updatedChar = (Map<String, Object>) restTemplate.exchange(
                "/api/v1/characters/" + charId, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class).getBody().get("data");
        assertThat(updatedChar.get("lastActiveChapterId")).isNotNull();
        assertThat(updatedChar.get("lastActiveChapterId").toString()).isEqualTo(chapterId);
        // firstChapterId 也应被设置（首次出现）
        assertThat(updatedChar.get("firstChapterId")).isNotNull();
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
