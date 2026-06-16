package com.czx.wenshu.interfaces.rest.project;

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
class CharacterControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        Map<String, String> registerRequest = Map.of(
                "email", "novelist@example.com",
                "password", "password123",
                "nickname", "小说家"
        );
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity("/api/v1/auth/register", registerRequest, Map.class);
        Map<String, Object> data = (Map<String, Object>) registerResponse.getBody().get("data");
        accessToken = (String) data.get("accessToken");

        ResponseEntity<Map> projectResponse = restTemplate.exchange(
                "/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "测试作品"), authHeaders()), Map.class);
        projectId = ((Map<String, Object>) projectResponse.getBody().get("data")).get("id").toString();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // P3-01 tests

    @Test
    void createCharacterReturnsCharacterInfo() {
        Map<String, String> request = Map.of("name", "主角", "role", "protagonist");
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/characters",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> character = (Map<String, Object>) response.getBody().get("data");
        assertThat(character.get("name")).isEqualTo("主角");
        assertThat(character.get("role")).isEqualTo("protagonist");
        assertThat(character.get("locked")).isEqualTo(false);
        assertThat(character.get("id")).isNotNull();
    }

    @Test
    void listCharactersReturnsAllCharacters() {
        restTemplate.exchange("/api/v1/projects/" + projectId + "/characters",
                HttpMethod.POST, new HttpEntity<>(Map.of("name", "角色一", "role", "hero"), authHeaders()), Map.class);
        restTemplate.exchange("/api/v1/projects/" + projectId + "/characters",
                HttpMethod.POST, new HttpEntity<>(Map.of("name", "角色二", "role", "villain"), authHeaders()), Map.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/characters",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> characters = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(characters).hasSize(2);
    }

    @Test
    void getCharacterReturnsDetail() {
        String characterId = createCharacterAndGetId("风云", "hero");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/characters/" + characterId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> character = (Map<String, Object>) response.getBody().get("data");
        assertThat(character.get("name")).isEqualTo("风云");
        assertThat(character.get("role")).isEqualTo("hero");
    }

    @Test
    void updateCharacterChangesFields() {
        String characterId = createCharacterAndGetId("旧名字", "hero");

        Map<String, String> updateRequest = Map.of(
                "name", "新名字",
                "role", "villain",
                "appearance", "高挑，黑发"
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/characters/" + characterId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> character = (Map<String, Object>) response.getBody().get("data");
        assertThat(character.get("name")).isEqualTo("新名字");
        assertThat(character.get("role")).isEqualTo("villain");
        assertThat(character.get("appearance")).isEqualTo("高挑，黑发");
    }

    @Test
    void deleteCharacterRemovesIt() {
        String characterId = createCharacterAndGetId("待删除角色", "extra");

        ResponseEntity<Map> deleteResponse = restTemplate.exchange(
                "/api/v1/characters/" + characterId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> getResponse = restTemplate.exchange(
                "/api/v1/characters/" + characterId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // P3-02 tests

    @Test
    void toggleLockChangesLockedState() {
        String characterId = createCharacterAndGetId("锁定角色", "lead");

        // 初始为未锁定
        ResponseEntity<Map> beforeResponse = restTemplate.exchange(
                "/api/v1/characters/" + characterId, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        assertThat(((Map<String, Object>) beforeResponse.getBody().get("data")).get("locked")).isEqualTo(false);

        // 锁定
        ResponseEntity<Map> lockResponse = restTemplate.exchange(
                "/api/v1/characters/" + characterId + "/lock",
                HttpMethod.PUT,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertThat(lockResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<String, Object>) lockResponse.getBody().get("data")).get("locked")).isEqualTo(true);

        // 解锁
        ResponseEntity<Map> unlockResponse = restTemplate.exchange(
                "/api/v1/characters/" + characterId + "/lock",
                HttpMethod.PUT,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertThat(unlockResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<String, Object>) unlockResponse.getBody().get("data")).get("locked")).isEqualTo(false);
    }

    @Test
    void characterWithoutAuthReturns401() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/projects/" + projectId + "/characters", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // P3-05 test: 角色名更新时同步词典条目

    @Test
    void updateCharacterNameSyncsMatchingWorldElement() {
        // 先创建一个与角色同名的词典条目
        restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/world-dict",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("type", "character", "name", "老名字"), authHeaders()),
                Map.class
        );

        // 创建角色，命名为"老名字"
        String characterId = createCharacterAndGetId("老名字", "hero");

        // 更新角色名为"新名字"
        ResponseEntity<Map> updateResponse = restTemplate.exchange(
                "/api/v1/characters/" + characterId,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", "新名字"), authHeaders()),
                Map.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<String, Object>) updateResponse.getBody().get("data")).get("name")).isEqualTo("新名字");

        // 验证词典中的同名条目已同步更新
        ResponseEntity<Map> listResponse = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/world-dict",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        List<Map<String, Object>> elements = (List<Map<String, Object>>) listResponse.getBody().get("data");
        assertThat(elements).hasSize(1);
        assertThat(elements.get(0).get("name")).isEqualTo("新名字");
    }

    @Test
    void updateCharacterNameNoMatchingWordDictEntryIsNoOp() {
        // 词典中没有同名条目，更新角色名不报错
        String characterId = createCharacterAndGetId("独立角色", "support");

        ResponseEntity<Map> updateResponse = restTemplate.exchange(
                "/api/v1/characters/" + characterId,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", "独立角色改名"), authHeaders()),
                Map.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<String, Object>) updateResponse.getBody().get("data")).get("name")).isEqualTo("独立角色改名");
    }

    private String createCharacterAndGetId(String name, String role) {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/characters",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("name", name, "role", role), authHeaders()),
                Map.class
        );
        return ((Map<String, Object>) response.getBody().get("data")).get("id").toString();
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
