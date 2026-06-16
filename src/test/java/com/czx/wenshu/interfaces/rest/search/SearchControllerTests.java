package com.czx.wenshu.interfaces.rest.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
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
class SearchControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;
    private String projectId;
    private String chapterId;

    @BeforeEach
    void setUp() {
        Map<String, String> registerRequest = Map.of(
                "email", "searcher@example.com",
                "password", "password123",
                "nickname", "搜索员"
        );
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", registerRequest, Map.class);
        Map<String, Object> data = (Map<String, Object>) registerResponse.getBody().get("data");
        accessToken = (String) data.get("accessToken");

        // 创建作品、卷、章节并写入内容
        ResponseEntity<Map> projectResp = restTemplate.exchange(
                "/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "搜索测试作品"), authHeaders()), Map.class);
        projectId = ((Map<String, Object>) projectResp.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> volumeResp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/volumes", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "第一卷", "sortOrder", 0), authHeaders()), Map.class);
        String volumeId = ((Map<String, Object>) volumeResp.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> chapterResp = restTemplate.exchange(
                "/api/v1/volumes/" + volumeId + "/chapters", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "第一章", "sortOrder", 0), authHeaders()), Map.class);
        chapterId = ((Map<String, Object>) chapterResp.getBody().get("data")).get("id").toString();

        // 写入章节内容，包含"张三"多次出现
        restTemplate.exchange("/api/v1/chapters/" + chapterId, HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "title", "第一章",
                        "content", "张三走进了客栈，张三环顾四周。这里没有张三认识的人。",
                        "status", "draft"), authHeaders()), Map.class);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ── P4-04 全书搜索 ─────────────────────────────────────────────────────

    @Test
    void searchFindsKeywordInChapters() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/search?keyword=张三",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> result = (Map<String, Object>) response.getBody().get("data");
        assertThat(result.get("total")).isEqualTo(3);
        List<Map<String, Object>> chapters = (List<Map<String, Object>>) result.get("chapters");
        assertThat(chapters).hasSize(1);
        assertThat(chapters.get(0).get("matchCount")).isEqualTo(3);
    }

    @Test
    void searchReturnsEmptyWhenNoMatch() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/search?keyword=李四",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> result = (Map<String, Object>) response.getBody().get("data");
        assertThat(result.get("total")).isEqualTo(0);
        assertThat((List<?>) result.get("chapters")).isEmpty();
    }

    @Test
    void searchIsCaseInsensitiveByDefault() {
        // 写入英文内容
        restTemplate.exchange("/api/v1/chapters/" + chapterId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("title", "第一章", "content", "Hello World hello", "status", "draft"),
                        authHeaders()), Map.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/search?keyword=hello",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        Map<String, Object> result = (Map<String, Object>) response.getBody().get("data");
        assertThat((Integer) result.get("total")).isEqualTo(2);
    }

    @Test
    void searchWithoutAuthReturns401() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/projects/" + projectId + "/search?keyword=张三", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── P4-05 全书替换（含快照保护）─────────────────────────────────────────

    @Test
    void replaceCreatesSnapshotAndUpdatesContent() {
        Map<String, Object> replaceReq = Map.of(
                "keyword", "张三",
                "replacement", "李四",
                "caseSensitive", false,
                "wholeWord", false,
                "syncCharacterName", false
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/search/replace",
                HttpMethod.POST, new HttpEntity<>(replaceReq, authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> result = (Map<String, Object>) response.getBody().get("data");
        assertThat(result.get("totalReplaced")).isEqualTo(3);

        List<Map<String, Object>> affected = (List<Map<String, Object>>) result.get("affectedChapters");
        assertThat(affected).hasSize(1);
        assertThat(affected.get(0).get("replacedCount")).isEqualTo(3);
        assertThat(affected.get(0).get("snapshotId")).isNotNull();

        // 验证章节内容已更新
        ResponseEntity<Map> chapterResp = restTemplate.exchange(
                "/api/v1/chapters/" + chapterId, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        String content = (String) ((Map<String, Object>) chapterResp.getBody().get("data")).get("content");
        assertThat(content).contains("李四");
        assertThat(content).doesNotContain("张三");
    }

    @Test
    void replaceWithNoMatchReturnsZero() {
        Map<String, Object> replaceReq = Map.of(
                "keyword", "不存在的词",
                "replacement", "新词",
                "caseSensitive", false,
                "wholeWord", false,
                "syncCharacterName", false
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/search/replace",
                HttpMethod.POST, new HttpEntity<>(replaceReq, authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> result = (Map<String, Object>) response.getBody().get("data");
        assertThat(result.get("totalReplaced")).isEqualTo(0);
        assertThat((List<?>) result.get("affectedChapters")).isEmpty();
    }

    // ── P4-06 角色名联动替换 ────────────────────────────────────────────────

    @Test
    void replaceWithSyncCharacterNameUpdatesCharacterArchive() {
        // 先创建一个名为"张三"的角色
        restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/characters", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "张三", "role", "hero"), authHeaders()), Map.class);

        // 替换时开启 syncCharacterName
        Map<String, Object> replaceReq = Map.of(
                "keyword", "张三",
                "replacement", "赵四",
                "caseSensitive", false,
                "wholeWord", false,
                "syncCharacterName", true
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/search/replace",
                HttpMethod.POST, new HttpEntity<>(replaceReq, authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> result = (Map<String, Object>) response.getBody().get("data");
        assertThat(result.get("characterNameSynced")).isEqualTo(true);

        // 验证角色档案已更新
        ResponseEntity<Map> charListResp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/characters", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        List<Map<String, Object>> characters = (List<Map<String, Object>>) charListResp.getBody().get("data");
        assertThat(characters).hasSize(1);
        assertThat(characters.get(0).get("name")).isEqualTo("赵四");
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
