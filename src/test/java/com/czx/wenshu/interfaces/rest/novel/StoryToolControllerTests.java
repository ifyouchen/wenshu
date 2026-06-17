package com.czx.wenshu.interfaces.rest.novel;

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
class StoryToolControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CapturingVerificationEmailSender verificationEmailSender;

    private String accessToken;
    private String projectId;
    private String chapterId;

    @BeforeEach
    void setUp() {
        verificationEmailSender.clear();
        String email = "story-tools@example.com";
        Map<String, String> registerReq = Map.of(
                "email", email,
                "password", "password123",
                "nickname", "工具用户",
                "verificationCode", sendRegisterCode(email));
        ResponseEntity<Map> regResp = restTemplate.postForEntity("/api/v1/auth/register", registerReq, Map.class);
        Map<String, Object> data = (Map<String, Object>) regResp.getBody().get("data");
        accessToken = (String) data.get("accessToken");

        ResponseEntity<Map> projectResp = restTemplate.exchange("/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "title", "工具测试作品",
                        "genre", "玄幻",
                        "synopsis", "少年背负旧案寻找真相",
                        "worldview", "宗门与王朝并立"), authHeaders()), Map.class);
        projectId = ((Map<String, Object>) projectResp.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> volumeResp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/volumes", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "第一卷", "sortOrder", 0), authHeaders()), Map.class);
        String volumeId = ((Map<String, Object>) volumeResp.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> chapterResp = restTemplate.exchange(
                "/api/v1/volumes/" + volumeId + "/chapters", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "第一章", "sortOrder", 0), authHeaders()), Map.class);
        chapterId = ((Map<String, Object>) chapterResp.getBody().get("data")).get("id").toString();

        restTemplate.exchange("/api/v1/chapters/" + chapterId, HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "title", "第一章",
                        "content", "夜雨落在青石阶上。林澈握着半枚玉佩，听见祠堂后传来脚步声。",
                        "status", "draft"), authHeaders()), Map.class);
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

    @Test
    void listToolsRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/story-tools", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listToolsReturnsCatalog() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/story-tools", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> tools = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(tools).extracting(tool -> tool.get("id"))
                .contains("story", "story-long-write", "story-short-write",
                        "story-long-analyze", "story-short-analyze", "story-long-scan", "story-short-scan",
                        "story-import", "story-cover", "story-deslop", "story-review",
                        "story-architect", "character-designer", "narrative-writer",
                        "consistency-checker", "chapter-extractor");
    }

    @Test
    void runToolRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/story-tools/story-review/run",
                Map.of("projectId", projectId, "input", "一段正文"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void runUnknownToolReturns400() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/story-tools/not-a-tool/run", HttpMethod.POST,
                new HttpEntity<>(Map.of("projectId", projectId, "input", "一段正文"), authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString()).contains("未知故事工具");
    }

    @Test
    void runWithoutProjectOrChapterReturns400() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/story-tools/story-architect/run", HttpMethod.POST,
                new HttpEntity<>(Map.of("instruction", "设计一个开篇"), authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString()).contains("projectId 和 chapterId 至少提供一个");
    }

    @Test
    void runCreativeToolWithNoApiKeyReturns400() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/story-tools/story-deslop/run", HttpMethod.POST,
                new HttpEntity<>(Map.of("chapterId", chapterId), authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message").toString()).contains("API Key 未配置");
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

        void clear() {
            sentTokens.clear();
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
