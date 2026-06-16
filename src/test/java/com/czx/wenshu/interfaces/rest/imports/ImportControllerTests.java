package com.czx.wenshu.interfaces.rest.imports;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.application.imports.ImportApplicationService;
import com.czx.wenshu.application.user.SecurityAlertEmailSender;
import com.czx.wenshu.domain.user.EmailAddress;
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
class ImportControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ImportApplicationService importApplicationService;

    private String accessToken;
    private String projectId;
    private String volumeId;

    @BeforeEach
    void setUp() {
        Map<String, String> registerRequest = Map.of(
                "email", "importer@example.com",
                "password", "password123",
                "nickname", "导入员"
        );
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity("/api/v1/auth/register", registerRequest, Map.class);
        Map<String, Object> data = (Map<String, Object>) registerResponse.getBody().get("data");
        accessToken = (String) data.get("accessToken");

        ResponseEntity<Map> projectResponse = restTemplate.exchange(
                "/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "导入测试作品"), authHeaders()), Map.class);
        projectId = ((Map<String, Object>) projectResponse.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> volumeResponse = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/volumes", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "第一卷", "sortOrder", 0), authHeaders()), Map.class);
        volumeId = ((Map<String, Object>) volumeResponse.getBody().get("data")).get("id").toString();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ── P4-03 粘贴导入（依赖最少，优先验证）─────────────────────────────────

    @Test
    void pasteImportWithChapterHeadingsCreatesMultipleChapters() {
        String text = """
                第一章 初入江湖
                这是第一章的内容，描述了主角初次踏入江湖的故事。

                第二章 初遇
                这是第二章的内容，主角遇到了重要角色。

                第三章 转折
                剧情在此发生了重大转折。
                """;

        Map<String, Object> request = Map.of(
                "projectId", projectId,
                "volumeId", volumeId,
                "text", text
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/import/paste",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> chapters = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(chapters).hasSize(3);
        assertThat(chapters.get(0).get("title")).isEqualTo("第一章 初入江湖");
        assertThat(chapters.get(1).get("title")).isEqualTo("第二章 初遇");
        assertThat(chapters.get(2).get("title")).isEqualTo("第三章 转折");
    }

    @Test
    void pasteImportWithNoHeadingCreatesOneChapter() {
        String text = "这是没有章节标题的纯文本内容，应该作为一章整体导入。";
        Map<String, Object> request = Map.of(
                "projectId", projectId,
                "volumeId", volumeId,
                "text", text
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/import/paste",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> chapters = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(chapters).hasSize(1);
        assertThat(chapters.get(0).get("title")).isEqualTo("第一章");
    }

    @Test
    void pasteImportWithoutAuthReturns401() {
        Map<String, Object> request = Map.of("projectId", projectId, "volumeId", volumeId, "text", "内容");
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/import/paste", request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── P4-01/P4-02 解析会话（通过 service 直接测试核心逻辑）──────────────

    @Test
    void splitIntoChaptersDetectsChineseHeadings() {
        String text = "第一章 起源\n内容一\n\n第二章 发展\n内容二\n\n第三章 高潮\n内容三";
        var chapters = importApplicationService.splitIntoChapters(text);
        assertThat(chapters).hasSize(3);
        assertThat(chapters.get(0).title()).isEqualTo("第一章 起源");
        assertThat(chapters.get(0).content()).isEqualTo("内容一");
        assertThat(chapters.get(1).title()).isEqualTo("第二章 发展");
        assertThat(chapters.get(2).title()).isEqualTo("第三章 高潮");
    }

    @Test
    void splitIntoChaptersHandlesPrologueBeforeFirstChapter() {
        String text = "这是前言内容，描述了世界观背景。\n\n第一章 正式开始\n这是正文内容，描述主角的初次登场。";
        var chapters = importApplicationService.splitIntoChapters(text);
        assertThat(chapters).hasSize(2);
        assertThat(chapters.get(0).title()).isEqualTo("序章");
        assertThat(chapters.get(1).title()).isEqualTo("第一章 正式开始");
        assertThat(chapters.get(1).content()).isEqualTo("这是正文内容，描述主角的初次登场。");
    }

    @Test
    void adjustSplitPointsThenApply() {
        String text = "第一章 初稿\n初稿内容";
        Map<String, Object> pasteReq = Map.of(
                "projectId", projectId, "volumeId", volumeId, "text", text);
        ResponseEntity<Map> pasteResp = restTemplate.exchange(
                "/api/v1/import/paste", HttpMethod.POST,
                new HttpEntity<>(pasteReq, authHeaders()), Map.class);
        List<Map<String, Object>> created = (List<Map<String, Object>>) pasteResp.getBody().get("data");
        assertThat(created).hasSize(1);
        assertThat(created.get(0).get("title")).isEqualTo("第一章 初稿");
        assertThat((Integer) created.get(0).get("wordCount")).isGreaterThan(0);
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
