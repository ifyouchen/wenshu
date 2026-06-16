package com.czx.wenshu.interfaces.rest.script;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.application.user.SecurityAlertEmailSender;
import com.czx.wenshu.domain.script.ScriptDraft;
import com.czx.wenshu.domain.script.ScriptDraftRepository;
import com.czx.wenshu.domain.script.ScriptScene;
import com.czx.wenshu.domain.script.ScriptSceneRepository;
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
class ScriptConversionTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ScriptDraftRepository scriptDraftRepository;

    @Autowired
    private ScriptSceneRepository scriptSceneRepository;

    private String accessToken;
    private String projectId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        Map<String, String> regReq = Map.of("email", "script-conv@example.com",
                "password", "password123", "nickname", "剧本测试");
        ResponseEntity<Map> regResp = restTemplate.postForEntity("/api/v1/auth/register", regReq, Map.class);
        Map<String, Object> data = (Map<String, Object>) regResp.getBody().get("data");
        accessToken = (String) data.get("accessToken");

        ResponseEntity<Map> meResp = restTemplate.exchange(
                "/api/v1/user/me", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        userId = UUID.fromString(((Map<String, Object>) meResp.getBody().get("data")).get("id").toString());

        ResponseEntity<Map> projResp = restTemplate.exchange("/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "剧本测试作品"), authHeaders()), Map.class);
        projectId = ((Map<String, Object>) projResp.getBody().get("data")).get("id").toString();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + accessToken);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    // ── P7-04 异步剧本改编 ──────────────────────────────────────────────

    @Test
    void convertRequiresAuth() {
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/v1/script/convert",
                Map.of("projectId", projectId), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void convertReturnsTaskIdAndDraftId() {
        Map<String, Object> req = Map.of(
                "projectId", projectId,
                "title", "测试草稿",
                "psychologyStrategy", "action");
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/script/convert", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders()), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("taskId")).isNotNull();
        assertThat(data.get("draftId")).isNotNull();

        // 草稿应已创建
        String draftId = data.get("draftId").toString();
        ResponseEntity<Map> draftResp = restTemplate.exchange(
                "/api/v1/script/drafts/" + draftId,
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        assertThat(draftResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> draft = (Map<String, Object>) draftResp.getBody().get("data");
        assertThat(draft.get("title")).isEqualTo("测试草稿");
        assertThat(draft.get("strategy")).isEqualTo("action");
    }

    @Test
    void convertConsumesAdaptationQuota() {
        Map<String, Object> req = Map.of("projectId", projectId, "psychologyStrategy", "dialogue");
        restTemplate.exchange("/api/v1/script/convert", HttpMethod.POST,
                new HttpEntity<>(req, authHeaders()), Map.class);

        ResponseEntity<Map> quotaResp = restTemplate.exchange(
                "/api/v1/user/quota", HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        Map<String, Object> quota = (Map<String, Object>) quotaResp.getBody().get("data");
        assertThat((Integer) quota.get("usedAdaptations")).isEqualTo(1);
    }

    // ── P7-05 剧本工作台数据（已在P7-01实现，此处验证完整数据）──────────

    @Test
    void getDraftReturnsCorrectFields() {
        java.time.Clock clock = java.time.Clock.systemUTC();
        ScriptDraft draft = ScriptDraft.create(
                UUID.fromString(projectId), userId, "工作台草稿", "voiceover", clock);
        scriptDraftRepository.save(draft);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/script/drafts/" + draft.id(),
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("id")).isEqualTo(draft.id().toString());
        assertThat(data.get("strategy")).isEqualTo("voiceover");
        assertThat(data.get("status")).isEqualTo("processing");
    }

    @Test
    void getScenesPaginationWorks() {
        java.time.Clock clock = java.time.Clock.systemUTC();
        ScriptDraft draft = ScriptDraft.create(
                UUID.fromString(projectId), userId, "场景草稿", "action", clock);
        scriptDraftRepository.save(draft);

        // 创建 3 个场景
        for (int i = 0; i < 3; i++) {
            ScriptScene scene = ScriptScene.create(draft.id(), i, "原文片段" + i, clock);
            scriptSceneRepository.save(scene);
        }

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/script/drafts/" + draft.id() + "/scenes?page=0&size=2",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("total")).isEqualTo(3);
        assertThat(data.get("size")).isEqualTo(2);
        List<?> scenes = (List<?>) data.get("scenes");
        assertThat(scenes).hasSize(2);
    }

    // ── P7-06 乐观锁 ──────────────────────────────────────────────────────

    @Test
    void sceneUpdateWithWrongVersionReturns409() {
        java.time.Clock clock = java.time.Clock.systemUTC();
        ScriptDraft draft = ScriptDraft.create(
                UUID.fromString(projectId), userId, "乐观锁草稿", "action", clock);
        scriptDraftRepository.save(draft);
        ScriptScene scene = ScriptScene.create(draft.id(), 0, "原文", clock);
        scriptSceneRepository.save(scene);

        // 使用错误的版本号更新
        Map<String, Object> updateReq = Map.of("content", "新剧本内容", "version", 99);
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/script/scenes/" + scene.id(),
                HttpMethod.PUT, new HttpEntity<>(updateReq, authHeaders()), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void sceneUpdateWithCorrectVersionSucceeds() {
        java.time.Clock clock = java.time.Clock.systemUTC();
        ScriptDraft draft = ScriptDraft.create(
                UUID.fromString(projectId), userId, "乐观锁草稿2", "action", clock);
        scriptDraftRepository.save(draft);
        ScriptScene scene = ScriptScene.create(draft.id(), 0, "原文", clock);
        scriptSceneRepository.save(scene);

        Map<String, Object> updateReq = Map.of("content", "新剧本内容", "version", 0);
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/script/scenes/" + scene.id(),
                HttpMethod.PUT, new HttpEntity<>(updateReq, authHeaders()), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("version")).isEqualTo(1);
        assertThat(data.get("content")).isEqualTo("新剧本内容");
    }

    // ── P7-07 分集管理 ────────────────────────────────────────────────────

    @Test
    void createEpisodeAndListIt() {
        java.time.Clock clock = java.time.Clock.systemUTC();
        ScriptDraft draft = ScriptDraft.create(
                UUID.fromString(projectId), userId, "分集草稿", "action", clock);
        scriptDraftRepository.save(draft);

        // 创建集数
        Map<String, Object> episodeReq = Map.of("episodeNo", 1, "title", "第一集");
        ResponseEntity<Map> createResp = restTemplate.exchange(
                "/api/v1/script/drafts/" + draft.id() + "/episodes",
                HttpMethod.POST, new HttpEntity<>(episodeReq, authHeaders()), Map.class);

        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> episode = (Map<String, Object>) createResp.getBody().get("data");
        assertThat(episode.get("title")).isEqualTo("第一集");
        assertThat(episode.get("episodeNo")).isEqualTo(1);

        // 查询集数列表
        ResponseEntity<Map> listResp = restTemplate.exchange(
                "/api/v1/script/drafts/" + draft.id() + "/episodes",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        List<?> episodes = (List<?>) listResp.getBody().get("data");
        assertThat(episodes).hasSize(1);
    }

    @Test
    void deleteEpisodeRemovesIt() {
        java.time.Clock clock = java.time.Clock.systemUTC();
        ScriptDraft draft = ScriptDraft.create(
                UUID.fromString(projectId), userId, "删集草稿", "action", clock);
        scriptDraftRepository.save(draft);

        Map<String, Object> episodeReq = Map.of("episodeNo", 1, "title", "待删集");
        ResponseEntity<Map> createResp = restTemplate.exchange(
                "/api/v1/script/drafts/" + draft.id() + "/episodes",
                HttpMethod.POST, new HttpEntity<>(episodeReq, authHeaders()), Map.class);
        String episodeId = ((Map<String, Object>) createResp.getBody().get("data")).get("id").toString();

        ResponseEntity<Map> deleteResp = restTemplate.exchange(
                "/api/v1/script/drafts/" + draft.id() + "/episodes/" + episodeId,
                HttpMethod.DELETE, new HttpEntity<>(authHeaders()), Map.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> listResp = restTemplate.exchange(
                "/api/v1/script/drafts/" + draft.id() + "/episodes",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        assertThat((List<?>) listResp.getBody().get("data")).isEmpty();
    }

    // ── P7-08 导出任务 ─────────────────────────────────────────────────────

    @Test
    void exportCreatesTask() {
        java.time.Clock clock = java.time.Clock.systemUTC();
        ScriptDraft draft = ScriptDraft.create(
                UUID.fromString(projectId), userId, "导出草稿", "action", clock);
        scriptDraftRepository.save(draft);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/script/drafts/" + draft.id() + "/export?format=docx",
                HttpMethod.POST, new HttpEntity<>(authHeaders()), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("taskId")).isNotNull();
        assertThat(data.get("draftId")).isEqualTo(draft.id().toString());
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
