package com.czx.wenshu.interfaces.rest.project;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/users-test-schema.sql")
class ProjectControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CapturingVerificationEmailSender verificationEmailSender;

    private String accessToken;

    @BeforeEach
    void setUp() {
        verificationEmailSender.clear();
        Map<String, String> registerRequest = Map.of(
                "email", "writer@example.com",
                "password", "password123",
                "nickname", "作家"
        );
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity("/api/v1/auth/register", registerRequest, Map.class);
        Map<String, Object> data = (Map<String, Object>) registerResponse.getBody().get("data");
        accessToken = (String) data.get("accessToken");
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void createProjectReturnsProjectInfo() {
        Map<String, String> request = Map.of(
                "title", "我的小说",
                "genre", "fantasy",
                "synopsis", "一个奇幻冒险故事"
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("title")).isEqualTo("我的小说");
        assertThat(data.get("genre")).isEqualTo("fantasy");
        assertThat(data.get("status")).isEqualTo("draft");
        assertThat(data.get("totalWords")).isEqualTo(0);
    }

    @Test
    void listProjectsReturnsCreatedProjects() {
        restTemplate.exchange("/api/v1/projects", org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "作品一"), authHeaders()), Map.class);
        restTemplate.exchange("/api/v1/projects", org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "作品二"), authHeaders()), Map.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> projects = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(projects).hasSize(2);
    }

    @Test
    void updateProjectChangesTitle() {
        ResponseEntity<Map> createResponse = restTemplate.exchange(
                "/api/v1/projects", org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "原标题"), authHeaders()), Map.class);
        String projectId = ((Map<String, Object>) createResponse.getBody().get("data")).get("id").toString();

        Map<String, String> updateRequest = Map.of("title", "新标题");
        ResponseEntity<Map> updateResponse = restTemplate.exchange(
                "/api/v1/projects/" + projectId,
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(updateRequest, authHeaders()),
                Map.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<String, Object>) updateResponse.getBody().get("data")).get("title")).isEqualTo("新标题");
    }

    @Test
    void deleteProjectRemovesProject() {
        ResponseEntity<Map> createResponse = restTemplate.exchange(
                "/api/v1/projects", org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "待删除"), authHeaders()), Map.class);
        String projectId = ((Map<String, Object>) createResponse.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> deleteResponse = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "?confirm=true",
                org.springframework.http.HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> getResponse = restTemplate.exchange(
                "/api/v1/projects/" + projectId,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createVolumeUnderProject() {
        String projectId = createProjectAndGetId();

        Map<String, Object> request = Map.of("title", "第一卷", "sortOrder", 0);
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/volumes",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("title")).isEqualTo("第一卷");
    }

    @Test
    void createChapterUnderVolume() {
        String projectId = createProjectAndGetId();
        String volumeId = createVolumeAndGetId(projectId);

        Map<String, Object> request = Map.of("title", "第一章", "sortOrder", 0);
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/volumes/" + volumeId + "/chapters",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("title")).isEqualTo("第一章");
        assertThat(data.get("wordCount")).isEqualTo(0);
    }

    @Test
    void updateChapterContentUpdatesWordCount() {
        String projectId = createProjectAndGetId();
        String volumeId = createVolumeAndGetId(projectId);
        String chapterId = createChapterAndGetId(volumeId);

        Map<String, String> request = Map.of(
                "title", "第一章 修改后",
                "content", "这是一个测试内容，包含一些文字。",
                "status", "draft"
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/chapters/" + chapterId,
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("title")).isEqualTo("第一章 修改后");
        assertThat(data.get("wordCount")).isInstanceOf(Integer.class);
        assertThat(data.get("status")).isEqualTo("draft");
    }

    @Test
    void deleteChapterRemovesChapter() {
        String projectId = createProjectAndGetId();
        String volumeId = createVolumeAndGetId(projectId);
        String chapterId = createChapterAndGetId(volumeId);

        ResponseEntity<Map> deleteResponse = restTemplate.exchange(
                "/api/v1/chapters/" + chapterId,
                org.springframework.http.HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> getResponse = restTemplate.exchange(
                "/api/v1/chapters/" + chapterId,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void projectWithoutAuthReturns401() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/projects", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @TestConfiguration
    static class VerificationEmailSenderTestConfig {

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

        private final List<ProjectControllerTests.SentToken> sentTokens = new ArrayList<>();

        @Override
        public void sendVerificationEmail(EmailAddress email, String rawToken, Instant expiresAt) {
            sentTokens.add(new ProjectControllerTests.SentToken(email, rawToken, expiresAt));
        }

        void clear() { sentTokens.clear(); }
        List<ProjectControllerTests.SentToken> sentTokens() { return sentTokens; }
    }

    static class CapturingPasswordResetEmailSender implements PasswordResetEmailSender {

        private final List<ProjectControllerTests.SentToken> sentTokens = new ArrayList<>();

        @Override
        public void sendPasswordResetEmail(EmailAddress email, String rawToken, Instant expiresAt) {
            sentTokens.add(new ProjectControllerTests.SentToken(email, rawToken, expiresAt));
        }

        void clear() { sentTokens.clear(); }
        List<ProjectControllerTests.SentToken> sentTokens() { return sentTokens; }
    }

    static class CapturingSecurityAlertEmailSender implements SecurityAlertEmailSender {

        @Override
        public void sendSecurityAlertEmail(EmailAddress email, String alertType, String alertDetail, String alertTime) {
        }
    }

    record SentToken(EmailAddress email, String rawToken, Instant expiresAt) {
    }

    private String createProjectAndGetId() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects", org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "测试作品"), authHeaders()), Map.class);
        return ((Map<String, Object>) response.getBody().get("data")).get("id").toString();
    }

    private String createVolumeAndGetId(String projectId) {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/volumes", org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "第一卷", "sortOrder", 0), authHeaders()), Map.class);
        return ((Map<String, Object>) response.getBody().get("data")).get("id").toString();
    }

    private String createChapterAndGetId(String volumeId) {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/volumes/" + volumeId + "/chapters", org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "第一章", "sortOrder", 0), authHeaders()), Map.class);
        return ((Map<String, Object>) response.getBody().get("data")).get("id").toString();
    }

    @Test
    void getOutlineReturnsVolumeChapterTree() {
        String projectId = createProjectAndGetId();
        String volumeId = createVolumeAndGetId(projectId);
        createChapterAndGetId(volumeId);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/outline",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        List<Map<String, Object>> volumes = (List<Map<String, Object>>) data.get("volumes");
        assertThat(volumes).hasSize(1);
        assertThat(volumes.get(0).get("title")).isEqualTo("第一卷");
        List<Map<String, Object>> chapters = (List<Map<String, Object>>) volumes.get(0).get("chapters");
        assertThat(chapters).hasSize(1);
        assertThat(chapters.get(0).get("title")).isEqualTo("第一章");
    }

    @Test
    void updateWritingGoalSetsDailyCharGoal() {
        String projectId = createProjectAndGetId();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/writing-goal",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(Map.of("dailyCharGoal", 3000), authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("dailyCharGoal")).isEqualTo(3000);
    }

    @Test
    void createSnapshotAndRestoreWorks() {
        String projectId = createProjectAndGetId();
        String volumeId = createVolumeAndGetId(projectId);
        String chapterId = createChapterAndGetId(volumeId);

        Map<String, String> updateReq = Map.of("content", "原始内容", "status", "draft");
        restTemplate.exchange("/api/v1/chapters/" + chapterId,
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(updateReq, authHeaders()), Map.class);

        ResponseEntity<Map> snapshotResponse = restTemplate.exchange(
                "/api/v1/chapters/" + chapterId + "/snapshots",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(Map.of("snapshotType", "manual", "label", "初始版本"), authHeaders()),
                Map.class
        );

        assertThat(snapshotResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> snapshotData = (Map<String, Object>) snapshotResponse.getBody().get("data");
        String snapshotId = snapshotData.get("id").toString();
        assertThat(snapshotData.get("snapshotType")).isEqualTo("manual");

        Map<String, String> updateReq2 = Map.of("content", "修改后内容", "status", "draft");
        restTemplate.exchange("/api/v1/chapters/" + chapterId,
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(updateReq2, authHeaders()), Map.class);

        ResponseEntity<Map> listResponse = restTemplate.exchange(
                "/api/v1/chapters/" + chapterId + "/snapshots",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> restoreResponse = restTemplate.exchange(
                "/api/v1/snapshots/" + snapshotId + "/restore",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertThat(restoreResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> restoredChapter = (Map<String, Object>) restoreResponse.getBody().get("data");
        assertThat(restoredChapter.get("content")).isEqualTo("原始内容");
    }
}