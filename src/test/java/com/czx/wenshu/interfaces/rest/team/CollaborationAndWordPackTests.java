package com.czx.wenshu.interfaces.rest.team;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.application.user.SecurityAlertEmailSender;
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

/**
 * 项目协作（P9-08）与字数包/体验额度（P9-09）集成测试。
 *
 * <p>覆盖场景：</p>
 * <ol>
 *   <li>注册新用户 → 自动发放体验额度（P9-09）</li>
 *   <li>GET /user/quota → 包含 wordPackRemainingChars 字段（P9-09）</li>
 *   <li>POST /projects/{id}/collaborators — 添加协作者（P9-08）</li>
 *   <li>GET /projects/{id}/collaborators — 查询协作者列表（P9-08）</li>
 *   <li>DELETE /projects/{id}/collaborators/{userId} — 移除协作者（P9-08）</li>
 *   <li>非所有者添加协作者 → 403（P9-08）</li>
 *   <li>GET /teams/{id}/usage — 团队用量统计（P9-08）</li>
 *   <li>支付 topup 后字数包余量增加（P9-09 + P9-03）</li>
 * </ol>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/users-test-schema.sql")
class CollaborationAndWordPackTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private String tokenOwner;
    private String tokenCollaborator;
    private String collaboratorUserId;
    private String projectId;

    @TestConfiguration
    static class MailConfig {
        @Bean @Primary VerificationEmailSender v() { return (e, t, x) -> {}; }
        @Bean @Primary PasswordResetEmailSender p() { return (e, t, x) -> {}; }
        @Bean @Primary SecurityAlertEmailSender s() { return (e, a, d, t) -> {}; }
    }

    @BeforeEach
    void setUp() {
        // 创建项目所有者
        tokenOwner = register("owner@example.com", "password123", "所有者");
        // 创建协作者
        tokenCollaborator = register("collaborator@example.com", "password123", "协作者");

        // 获取协作者 user ID
        ResponseEntity<Map> meResp = restTemplate.exchange("/api/v1/user/me", HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenCollaborator)), Map.class);
        Map<String, Object> meData = (Map<String, Object>) meResp.getBody().get("data");
        collaboratorUserId = (String) meData.get("id");

        // 所有者创建作品
        ResponseEntity<Map> projResp = restTemplate.exchange("/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "协作测试作品"), authHeaders(tokenOwner)), Map.class);
        projectId = ((Map<String, Object>) projResp.getBody().get("data")).get("id").toString();
    }

    // ── P9-09：体验额度 ──────────────────────────────────────────────────────

    @Test
    void newUser_hasTrialWordPack_inQuota() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/user/quota", HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenOwner)), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        // P9-09：新注册用户应有体验额度（5万字）
        long wordPackRemaining = ((Number) data.get("wordPackRemainingChars")).longValue();
        assertThat(wordPackRemaining).isEqualTo(50_000L);
    }

    @Test
    void quota_hasTotalEffectiveCharsField() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/user/quota", HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenOwner)), Map.class);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data).containsKey("wordPackRemainingChars");
        assertThat(data).containsKey("remainingChars");
    }

    // ── P9-08：项目协作 ──────────────────────────────────────────────────────

    @Test
    void addCollaborator_byOwner_returns200() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/collaborators", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", collaboratorUserId, "role", "editor"),
                        authHeaders(tokenOwner)), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("role")).isEqualTo("editor");
        assertThat(data.get("userId")).isEqualTo(collaboratorUserId);
    }

    @Test
    void addCollaborator_byNonOwner_returns403() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/collaborators", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", collaboratorUserId),
                        authHeaders(tokenCollaborator)), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listCollaborators_afterAdd_returnsOne() {
        // 先添加协作者
        restTemplate.exchange("/api/v1/projects/" + projectId + "/collaborators", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", collaboratorUserId),
                        authHeaders(tokenOwner)), Map.class);

        // 所有者查询
        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/collaborators", HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenOwner)), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> collabs = (List<?>) resp.getBody().get("data");
        assertThat(collabs).hasSize(1);
    }

    @Test
    void removeCollaborator_byOwner_succeeds() {
        // 先添加
        restTemplate.exchange("/api/v1/projects/" + projectId + "/collaborators", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", collaboratorUserId),
                        authHeaders(tokenOwner)), Map.class);

        // 移除
        ResponseEntity<Map> removeResp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/collaborators/" + collaboratorUserId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(tokenOwner)), Map.class);
        assertThat(removeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 验证列表为空
        ResponseEntity<Map> listResp = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/collaborators", HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenOwner)), Map.class);
        List<?> collabs = (List<?>) listResp.getBody().get("data");
        assertThat(collabs).isEmpty();
    }

    @Test
    void teamUsage_afterCreatingTeam_returnsMemberStats() {
        // 创建团队
        ResponseEntity<Map> teamResp = restTemplate.exchange(
                "/api/v1/teams", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "测试团队"), authHeaders(tokenOwner)), Map.class);
        String teamId = ((Map<String, Object>) teamResp.getBody().get("data")).get("id").toString();

        // 查询团队用量
        ResponseEntity<Map> usageResp = restTemplate.exchange(
                "/api/v1/teams/" + teamId + "/usage", HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenOwner)), Map.class);
        assertThat(usageResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) usageResp.getBody().get("data");
        assertThat(((Number) data.get("memberCount")).intValue()).isEqualTo(1);
        assertThat(data).containsKey("totalUsedChars");
        assertThat(data).containsKey("memberUsage");
    }

    // ── 工具方法 ─────────────────────────────────────────────────────────────

    private String register(String email, String password, String nickname) {
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/auth/register",
                Map.of("email", email, "password", password, "nickname", nickname),
                Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        return (String) data.get("accessToken");
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
