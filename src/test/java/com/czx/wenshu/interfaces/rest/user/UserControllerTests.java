package com.czx.wenshu.interfaces.rest.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.domain.user.EmailAddress;
import com.czx.wenshu.domain.user.UserRepository;
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
class UserControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CapturingVerificationEmailSender verificationEmailSender;

    private String accessToken;

    @BeforeEach
    void setUp() {
        verificationEmailSender.clear();
        Map<String, String> registerRequest = Map.of(
                "email", "user@example.com",
                "password", "password123",
                "nickname", "测试用户"
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
    void getCurrentUserReturnsUserInfo() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user/me",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("email")).isEqualTo("user@example.com");
        assertThat(data.get("nickname")).isEqualTo("测试用户");
        assertThat(data.get("identityType")).isEqualTo("new_author");
        assertThat(data.get("isEmailVerified")).isEqualTo(false);
        assertThat(data.get("aiTrainConsent")).isEqualTo(true);
    }

    @Test
    void getCurrentUserWithoutAuthReturns401() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/user/me", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateProfileChangesNicknameAndIdentityType() {
        Map<String, String> request = Map.of(
                "nickname", "新昵称",
                "identityType", "web_novel_author"
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user/profile",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("nickname")).isEqualTo("新昵称");
        assertThat(data.get("identityType")).isEqualTo("web_novel_author");

        assertThat(userRepository.findByEmail(new EmailAddress("user@example.com")).orElseThrow().nickname()).isEqualTo("新昵称");
    }

    @Test
    void updateProfileChangesAvatarUrl() {
        Map<String, String> request = Map.of(
                "avatarUrl", "https://example.com/avatar.png"
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user/profile",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("avatarUrl")).isEqualTo("https://example.com/avatar.png");
    }

    @Test
    void changePasswordSucceedsWithCorrectCurrentPassword() {
        Map<String, String> request = Map.of(
                "currentPassword", "password123",
                "newPassword", "newpassword456"
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user/password",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> oldPasswordLogin = restTemplate.postForEntity("/api/v1/auth/login", Map.of(
                "email", "user@example.com",
                "password", "password123"
        ), Map.class);
        assertThat(oldPasswordLogin.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map> newPasswordLogin = restTemplate.postForEntity("/api/v1/auth/login", Map.of(
                "email", "user@example.com",
                "password", "newpassword456"
        ), Map.class);
        assertThat(newPasswordLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void changePasswordFailsWithWrongCurrentPassword() {
        Map<String, String> request = Map.of(
                "currentPassword", "wrongpassword",
                "newPassword", "newpassword456"
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user/password",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).isEqualTo("当前密码错误");
    }

    @Test
    void changePasswordRevokesAllTokens() {
        Map<String, String> passwordRequest = Map.of(
                "currentPassword", "password123",
                "newPassword", "newpassword456"
        );
        restTemplate.exchange(
                "/api/v1/user/password",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(passwordRequest, authHeaders()),
                Map.class
        );

        ResponseEntity<Map> meResponse = restTemplate.exchange(
                "/api/v1/user/me",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateAiConsentTurnsOff() {
        Map<String, Object> request = Map.of(
                "aiTrainConsent", false
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user/ai-consent",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("aiTrainConsent")).isEqualTo(false);

        assertThat(userRepository.findByEmail(new EmailAddress("user@example.com")).orElseThrow().isAiTrainConsent()).isFalse();
    }

    @Test
    void updateAiConsentTurnsOn() {
        Map<String, Object> request = Map.of(
                "aiTrainConsent", true
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user/ai-consent",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("aiTrainConsent")).isEqualTo(true);
    }

    @Test
    void deleteAccountSoftDeletesAndReturnsRestoreToken() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user",
                org.springframework.http.HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("restoreToken")).isNotNull();
        assertThat(data.get("restoreTokenExpiresAt")).isNotNull();

        assertThat(userRepository.findByEmail(new EmailAddress("user@example.com")).orElseThrow().isDeleted()).isTrue();
    }

    @Test
    void deleteAccountRevokesAllTokens() {
        restTemplate.exchange(
                "/api/v1/user",
                org.springframework.http.HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        ResponseEntity<Map> meResponse = restTemplate.exchange(
                "/api/v1/user/me",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void restoreAccountRecoversDeletedAccount() {
        ResponseEntity<Map> deleteResponse = restTemplate.exchange(
                "/api/v1/user",
                org.springframework.http.HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        Map<String, Object> deleteData = (Map<String, Object>) deleteResponse.getBody().get("data");
        String restoreToken = (String) deleteData.get("restoreToken");

        ResponseEntity<Map> restoreResponse = restTemplate.postForEntity(
                "/api/v1/user/cancel-restore",
                Map.of("restoreToken", restoreToken),
                Map.class
        );

        assertThat(restoreResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> restoreData = (Map<String, Object>) restoreResponse.getBody().get("data");
        assertThat(restoreData.get("email")).isEqualTo("user@example.com");

        assertThat(userRepository.findByEmail(new EmailAddress("user@example.com")).orElseThrow().isDeleted()).isFalse();
    }

    @Test
    void restoreAccountFailsWithInvalidToken() {
        ResponseEntity<Map> restoreResponse = restTemplate.postForEntity(
                "/api/v1/user/cancel-restore",
                Map.of("restoreToken", "invalid_token"),
                Map.class
        );

        assertThat(restoreResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateIdentityTypeChangesIdentityType() {
        Map<String, String> request = Map.of(
                "identityType", "short_drama_writer"
        );
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/user/identity-type",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("identityType")).isEqualTo("short_drama_writer");

        assertThat(userRepository.findByEmail(new EmailAddress("user@example.com")).orElseThrow().identityType().value()).isEqualTo("short_drama_writer");
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

        private final List<SentToken> sentTokens = new ArrayList<>();

        @Override
        public void sendPasswordResetEmail(EmailAddress email, String rawToken, Instant expiresAt) {
            sentTokens.add(new SentToken(email, rawToken, expiresAt));
        }

        void clear() {
            sentTokens.clear();
        }

        List<SentToken> sentTokens() {
            return sentTokens;
        }
    }
}