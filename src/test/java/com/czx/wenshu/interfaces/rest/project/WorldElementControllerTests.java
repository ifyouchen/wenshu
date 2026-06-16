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
class WorldElementControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;
    private String projectId;

    @BeforeEach
    void setUp() {
        Map<String, String> registerRequest = Map.of(
                "email", "worldbuilder@example.com",
                "password", "password123",
                "nickname", "世界观设计师"
        );
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity("/api/v1/auth/register", registerRequest, Map.class);
        Map<String, Object> data = (Map<String, Object>) registerResponse.getBody().get("data");
        accessToken = (String) data.get("accessToken");

        ResponseEntity<Map> projectResponse = restTemplate.exchange(
                "/api/v1/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "世界观测试作品"), authHeaders()), Map.class);
        projectId = ((Map<String, Object>) projectResponse.getBody().get("data")).get("id").toString();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // P3-03 tests

    @Test
    void createWorldElementReturnsElementInfo() {
        Map<String, String> request = Map.of(
                "type", "location",
                "name", "神秘森林",
                "description", "一片充满魔法的古老森林"
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/world-dict",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> element = (Map<String, Object>) response.getBody().get("data");
        assertThat(element.get("type")).isEqualTo("location");
        assertThat(element.get("name")).isEqualTo("神秘森林");
        assertThat(element.get("description")).isEqualTo("一片充满魔法的古老森林");
        assertThat(element.get("locked")).isEqualTo(false);
        assertThat(element.get("id")).isNotNull();
    }

    @Test
    void listWorldElementsReturnsAll() {
        restTemplate.exchange("/api/v1/projects/" + projectId + "/world-dict",
                HttpMethod.POST, new HttpEntity<>(Map.of("type", "location", "name", "地点A"), authHeaders()), Map.class);
        restTemplate.exchange("/api/v1/projects/" + projectId + "/world-dict",
                HttpMethod.POST, new HttpEntity<>(Map.of("type", "faction", "name", "势力B"), authHeaders()), Map.class);
        restTemplate.exchange("/api/v1/projects/" + projectId + "/world-dict",
                HttpMethod.POST, new HttpEntity<>(Map.of("type", "item", "name", "道具C"), authHeaders()), Map.class);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/world-dict",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> elements = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(elements).hasSize(3);
    }

    @Test
    void updateWorldElementChangesFields() {
        String elementId = createElementAndGetId("location", "旧地名", "旧描述");

        Map<String, String> updateRequest = Map.of(
                "type", "rule",
                "name", "新规则名",
                "description", "更新后的描述"
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/world-dict/" + elementId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> element = (Map<String, Object>) response.getBody().get("data");
        assertThat(element.get("type")).isEqualTo("rule");
        assertThat(element.get("name")).isEqualTo("新规则名");
        assertThat(element.get("description")).isEqualTo("更新后的描述");
    }

    @Test
    void deleteWorldElementRemovesIt() {
        String elementId = createElementAndGetId("item", "待删除道具", null);

        ResponseEntity<Map> deleteResponse = restTemplate.exchange(
                "/api/v1/world-dict/" + elementId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 列表中应已不包含该元素
        ResponseEntity<Map> listResponse = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/world-dict",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        List<Map<String, Object>> elements = (List<Map<String, Object>>) listResponse.getBody().get("data");
        assertThat(elements).isEmpty();
    }

    @Test
    void worldElementTypesIncludeAllCategories() {
        // 验证可管理地点、势力、道具、规则四类
        String[] types = {"location", "faction", "item", "rule"};
        for (String type : types) {
            restTemplate.exchange("/api/v1/projects/" + projectId + "/world-dict",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("type", type, "name", type + "-名称"), authHeaders()),
                    Map.class);
        }

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/world-dict",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        List<Map<String, Object>> elements = (List<Map<String, Object>>) response.getBody().get("data");
        assertThat(elements).hasSize(4);
        List<String> returnedTypes = elements.stream()
                .map(e -> (String) e.get("type")).toList();
        assertThat(returnedTypes).containsExactlyInAnyOrder("location", "faction", "item", "rule");
    }

    @Test
    void worldDictWithoutAuthReturns401() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/projects/" + projectId + "/world-dict", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // P3-04 tests: aliases 支持

    @Test
    void createWorldElementWithAliasesReturnsAliases() {
        Map<String, Object> request = Map.of(
                "type", "character",
                "name", "张三",
                "description", "主角",
                "aliases", List.of("小三", "三郎")
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/world-dict",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> element = (Map<String, Object>) response.getBody().get("data");
        assertThat(element.get("name")).isEqualTo("张三");
        List<String> aliases = (List<String>) element.get("aliases");
        assertThat(aliases).containsExactly("小三", "三郎");
    }

    @Test
    void updateWorldElementAliasesChangesAliases() {
        String elementId = createElementAndGetId("character", "李白", null);

        Map<String, Object> updateRequest = Map.of(
                "name", "李白",
                "aliases", List.of("诗仙", "翰林")
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/world-dict/" + elementId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> element = (Map<String, Object>) response.getBody().get("data");
        List<String> aliases = (List<String>) element.get("aliases");
        assertThat(aliases).containsExactly("诗仙", "翰林");
    }

    @Test
    void createWorldElementWithoutAliasesDefaultsToEmpty() {
        Map<String, String> request = Map.of("type", "item", "name", "魔法剑");
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/world-dict",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> element = (Map<String, Object>) response.getBody().get("data");
        List<String> aliases = (List<String>) element.get("aliases");
        assertThat(aliases).isEmpty();
    }

    private String createElementAndGetId(String type, String name, String description) {
        Map<String, Object> payload = description != null
                ? Map.of("type", type, "name", name, "description", description)
                : Map.of("type", type, "name", name);
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + projectId + "/world-dict",
                HttpMethod.POST,
                new HttpEntity<>(payload, authHeaders()),
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
