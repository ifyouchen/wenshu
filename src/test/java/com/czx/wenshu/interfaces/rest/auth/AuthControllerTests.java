package com.czx.wenshu.interfaces.rest.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/users-test-schema.sql")
class AuthControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registerReturnsTokenPairAndUserInfo() {
        Map<String, String> request = Map.of(
                "email", "Author@Example.com",
                "password", "password123",
                "nickname", "新作者"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("code")).isEqualTo(0);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat((String) data.get("accessToken")).startsWith("wat_");
        assertThat((String) data.get("refreshToken")).startsWith("wrt_");
        assertThat(data.get("tokenType")).isEqualTo("Bearer");
        Map<String, Object> user = (Map<String, Object>) data.get("user");
        assertThat(user.get("email")).isEqualTo("author@example.com");
        assertThat(user.get("nickname")).isEqualTo("新作者");
        assertThat(user.get("identityType")).isEqualTo("new_author");
        assertThat(user.get("isEmailVerified")).isEqualTo(false);
        assertThat(user.get("aiTrainConsent")).isEqualTo(true);
    }

    @Test
    void registerRejectsDuplicatedEmail() {
        Map<String, String> request = Map.of(
                "email", "duplicate@example.com",
                "password", "password123",
                "nickname", "作者"
        );

        restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);
        ResponseEntity<Map> duplicateResponse = restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);

        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(duplicateResponse.getBody()).isNotNull();
        assertThat(duplicateResponse.getBody().get("code")).isEqualTo(40000);
        assertThat(duplicateResponse.getBody().get("message")).isEqualTo("该邮箱已注册");
    }
}
