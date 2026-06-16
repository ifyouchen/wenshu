package com.czx.wenshu.interfaces.rest.novel;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.application.novel.ContextAssemblyService;
import com.czx.wenshu.application.novel.ContextBundle;
import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.application.user.SecurityAlertEmailSender;
import com.czx.wenshu.domain.task.AsyncTask;
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
class NovelControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AsyncTaskService asyncTaskService;

    @Autowired
    private ContextAssemblyService contextAssemblyService;

    private String accessToken;
    private String projectId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        Map<String, String> registerReq = Map.of("email", "novel-ai@example.com",
                "password", "password123", "nickname", "AI写作者");
        ResponseEntity<Map> regResp = restTemplate.postForEntity(
                "/api/v1/auth/register", registerReq, Map.class);
        Map<String, Object> data = (Map<String, Object>) regResp.getBody().get("data");
        accessToken = (String) data.get("accessToken");

        ResponseEntity<Map> meResp = restTemplate.exchange(
                "/api/v1/user/me", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        userId = UUID.fromString(((Map<String, Object>) meResp.getBody().get("data")).get("id").toString());

        ResponseEntity<Map> projectResp = restTemplate.exchange(
                "/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "AI测试作品", "genre", "玄幻",
                        "synopsis", "一个热血少年的成长故事"), authHeaders()), Map.class);
        projectId = ((Map<String, Object>) projectResp.getBody().get("data")).get("id").toString();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ── P5-04 骨架生成任务提交 ─────────────────────────────────────────────

    @Test
    void submitSkeletonReturnsTaskId() {
        Map<String, Object> request = Map.of(
                "projectId", projectId,
                "genre", "玄幻",
                "synopsis", "天才少年横空出世",
                "worldview", "修炼为主，法宝为辅",
                "targetWords", 100000
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/novel/skeleton", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data).containsKey("taskId");
        String taskId = (String) data.get("taskId");
        assertThat(taskId).isNotBlank();

        // 任务可被查询（状态为 pending/running/failed，因为没有 API key）
        ResponseEntity<Map> progressResp = restTemplate.exchange(
                "/api/v1/tasks/" + taskId + "/progress", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        assertThat(progressResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> progress = (Map<String, Object>) progressResp.getBody().get("data");
        assertThat(progress.get("taskType")).isEqualTo("novel_skeleton");
    }

    @Test
    void submitSkeletonRequiresAuth() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/novel/skeleton",
                Map.of("projectId", projectId, "genre", "奇幻"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── P5-05 骨架应用入库 ─────────────────────────────────────────────────

    @Test
    void applySkeletonCreatesVolumesChaptersCharacters() throws Exception {
        // 准备：手动创建一个 COMPLETED 任务，带有 skeleton JSON 结果
        String skeletonJson = """
                {
                  "title": "天道至尊",
                  "theme": "逆境成长",
                  "volumes": [
                    {
                      "title": "第一卷 初入江湖",
                      "conflict": "主角初次踏入修炼世界，面临重重考验",
                      "chapters": [
                        {"title": "第一章 天才觉醒", "outline": "主角在困境中觉醒特殊天赋"},
                        {"title": "第二章 初出茅庐", "outline": "主角踏入修炼门派"}
                      ]
                    }
                  ],
                  "characters": [
                    {"name": "叶辰", "role": "主角", "description": "天才少年，命途多舛"},
                    {"name": "陈明", "role": "反派", "description": "嫉妒叶辰天赋的师兄"}
                  ]
                }
                """;

        AsyncTask task = asyncTaskService.createTask(userId, UUID.fromString(projectId), "novel_skeleton");
        asyncTaskService.completeWithJson(task.id(), skeletonJson);

        // 等待事务提交（小延迟确保异步任务不干扰）
        Thread.sleep(50);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/skeleton/" + task.id() + "/apply", HttpMethod.POST,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> result = (Map<String, Object>) response.getBody().get("data");
        assertThat(result.get("createdVolumes")).isEqualTo(1);
        assertThat(result.get("createdChapters")).isEqualTo(2);
        assertThat(result.get("createdCharacters")).isEqualTo(2);
        assertThat(result.get("title")).isEqualTo("天道至尊");

        // 验证大纲树中有新卷
        ResponseEntity<Map> outlineResp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/outline", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        Map<String, Object> outline = (Map<String, Object>) outlineResp.getBody().get("data");
        List<Map<String, Object>> volumes = (List<Map<String, Object>>) outline.get("volumes");
        assertThat(volumes).hasSize(1);
        assertThat(volumes.get(0).get("title")).isEqualTo("第一卷 初入江湖");

        // 验证角色列表
        ResponseEntity<Map> charsResp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/characters", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        List<Map<String, Object>> chars = (List<Map<String, Object>>) charsResp.getBody().get("data");
        assertThat(chars).hasSize(2);
    }

    // ── P5-06 动态上下文组装（单元层验证）────────────────────────────────

    @Test
    void contextAssemblyReturnsEmptyBundleForNewProject() {
        ContextBundle bundle = contextAssemblyService.assemble(
                UUID.fromString(projectId), null, 4000);
        // 新作品没有锁定角色或世界观
        assertThat(bundle).isNotNull();
        assertThat(bundle.lockedCharacterCount()).isEqualTo(0);
        assertThat(bundle.lockedWorldElementCount()).isEqualTo(0);
        assertThat(bundle.estimatedTokens()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void contextAssemblyIncludesLockedCharacters() {
        // 创建并锁定一个角色
        ResponseEntity<Map> charResp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/characters", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "叶辰", "role", "主角"), authHeaders()), Map.class);
        String charId = ((Map<String, Object>) charResp.getBody().get("data")).get("id").toString();
        restTemplate.exchange("/api/v1/characters/" + charId + "/lock", HttpMethod.PUT,
                new HttpEntity<>(authHeaders()), Map.class);

        ContextBundle bundle = contextAssemblyService.assemble(
                UUID.fromString(projectId), null, 4000);
        assertThat(bundle.lockedCharacterCount()).isEqualTo(1);
        assertThat(bundle.systemContext()).contains("叶辰");
        assertThat(bundle.systemContext()).contains("主角");
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
