package com.czx.wenshu.interfaces.rest.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.application.user.SecurityAlertEmailSender;
import com.czx.wenshu.domain.user.EmailAddress;
import com.czx.wenshu.domain.user.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CapturingVerificationEmailSender verificationEmailSender;

    @Autowired
    private CapturingPasswordResetEmailSender passwordResetEmailSender;

    @Test
    void registerReturnsTokenPairAndUserInfo() {
        verificationEmailSender.clear();
        Map<String, String> request = Map.of(
                "email", "Author@Example.com",
                "password", "password123",
                "nickname", "新作者",
                "verificationCode", sendRegisterCode("Author@Example.com")
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
        assertThat(user.get("isEmailVerified")).isEqualTo(true);
        assertThat(user.get("aiTrainConsent")).isEqualTo(true);
        assertThat(verificationEmailSender.sentTokens()).hasSize(1);
        assertThat(verificationEmailSender.sentTokens().getFirst().email().value()).isEqualTo("author@example.com");
        assertThat(verificationEmailSender.sentTokens().getFirst().expiresAt()).isAfter(Instant.now());
    }

    @Test
    void registerRejectsDuplicatedEmail() {
        verificationEmailSender.clear();
        Map<String, String> request = Map.of(
                "email", "duplicate@example.com",
                "password", "password123",
                "nickname", "作者",
                "verificationCode", sendRegisterCode("duplicate@example.com")
        );

        restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);
        ResponseEntity<Map> duplicateResponse = restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);

        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(duplicateResponse.getBody()).isNotNull();
        assertThat(duplicateResponse.getBody().get("code")).isEqualTo(40000);
        assertThat(duplicateResponse.getBody().get("message")).isEqualTo("该邮箱已注册");
    }

    @Test
    void registerMarksUserAsVerified() {
        verificationEmailSender.clear();
        Map<String, String> request = Map.of(
                "email", "verify@example.com",
                "password", "password123",
                "nickname", "待验证",
                "verificationCode", sendRegisterCode("verify@example.com")
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/register", request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        Map<String, Object> user = (Map<String, Object>) data.get("user");
        assertThat(user.get("email")).isEqualTo("verify@example.com");
        assertThat(user.get("isEmailVerified")).isEqualTo(true);
        assertThat(userRepository.findByEmail(new EmailAddress("verify@example.com")).orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    void sendRegisterCodeIsRateLimitedWithinSixtySeconds() {
        verificationEmailSender.clear();
        sendRegisterCode("limited@example.com");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/auth/register/code",
                Map.of("email", "limited@example.com"),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(42900);
        assertThat(response.getBody().get("message")).isEqualTo("请 60 秒后再试");
    }

    @Test
    void loginReturnsTokenPairAndResetsFailureState() {
        verificationEmailSender.clear();
        register("login@example.com", "password123", "登录用户");

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/login", Map.of(
                "email", "login@example.com",
                "password", "password123"
        ), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat((String) data.get("accessToken")).startsWith("wat_");
        assertThat((String) data.get("refreshToken")).startsWith("wrt_");
        Map<String, Object> user = (Map<String, Object>) data.get("user");
        assertThat(user.get("email")).isEqualTo("login@example.com");
        assertThat(userRepository.findByEmail(new EmailAddress("login@example.com")).orElseThrow().loginFailCount()).isZero();
    }

    @Test
    void loginLocksAccountAfterFiveFailures() {
        verificationEmailSender.clear();
        register("locked@example.com", "password123", "锁定用户");

        for (int i = 0; i < 5; i++) {
            ResponseEntity<Map> failed = restTemplate.postForEntity("/api/v1/auth/login", Map.of(
                    "email", "locked@example.com",
                    "password", "wrong-pass"
            ), Map.class);
            assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        ResponseEntity<Map> locked = restTemplate.postForEntity("/api/v1/auth/login", Map.of(
                "email", "locked@example.com",
                "password", "password123"
        ), Map.class);

        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(locked.getBody()).isNotNull();
        assertThat(locked.getBody().get("code")).isEqualTo(40300);
        assertThat(locked.getBody().get("message")).isEqualTo("账号已锁定，请 15 分钟后再试");
        assertThat(userRepository.findByEmail(new EmailAddress("locked@example.com")).orElseThrow().lockedUntil()).isNotNull();
    }

    @Test
    void logoutReturnsSuccess() {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/logout", null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo(0);
    }

    @Test
    void refreshTokenRotatesAndRevokesOldToken() {
        ResponseEntity<Map> registerResponse = register("refresh@example.com", "password123", "刷新用户");
        Map<String, Object> registerData = (Map<String, Object>) registerResponse.getBody().get("data");
        String oldRefreshToken = (String) registerData.get("refreshToken");

        ResponseEntity<Map> refreshResponse = restTemplate.postForEntity("/api/v1/auth/refresh", Map.of(
                "refreshToken", oldRefreshToken
        ), Map.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> refreshData = (Map<String, Object>) refreshResponse.getBody().get("data");
        String newRefreshToken = (String) refreshData.get("refreshToken");
        assertThat(newRefreshToken).startsWith("wrt_").isNotEqualTo(oldRefreshToken);

        ResponseEntity<Map> reusedOldTokenResponse = restTemplate.postForEntity("/api/v1/auth/refresh", Map.of(
                "refreshToken", oldRefreshToken
        ), Map.class);
        assertThat(reusedOldTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(reusedOldTokenResponse.getBody()).isNotNull();
        assertThat(reusedOldTokenResponse.getBody().get("code")).isEqualTo(40100);
    }

    @Test
    void forgotAndResetPasswordRevokesAllRefreshTokens() {
        passwordResetEmailSender.clear();
        ResponseEntity<Map> registerResponse = register("reset@example.com", "old-password", "重置用户");
        Map<String, Object> registerData = (Map<String, Object>) registerResponse.getBody().get("data");
        String oldRefreshToken = (String) registerData.get("refreshToken");

        ResponseEntity<Map> forgotResponse = restTemplate.postForEntity("/api/v1/auth/password/forgot", Map.of(
                "email", "reset@example.com"
        ), Map.class);

        assertThat(forgotResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> forgotData = (Map<String, Object>) forgotResponse.getBody().get("data");
        assertThat(forgotData.get("sent")).isEqualTo(true);
        assertThat(passwordResetEmailSender.sentTokens()).hasSize(1);
        String resetToken = passwordResetEmailSender.sentTokens().getFirst().rawToken();

        ResponseEntity<Map> resetResponse = restTemplate.postForEntity("/api/v1/auth/password/reset", Map.of(
                "token", resetToken,
                "newPassword", "new-password"
        ), Map.class);

        assertThat(resetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<Map> oldRefreshResponse = restTemplate.postForEntity("/api/v1/auth/refresh", Map.of(
                "refreshToken", oldRefreshToken
        ), Map.class);
        assertThat(oldRefreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<Map> oldPasswordLogin = restTemplate.postForEntity("/api/v1/auth/login", Map.of(
                "email", "reset@example.com",
                "password", "old-password"
        ), Map.class);
        assertThat(oldPasswordLogin.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map> newPasswordLogin = restTemplate.postForEntity("/api/v1/auth/login", Map.of(
                "email", "reset@example.com",
                "password", "new-password"
        ), Map.class);
        assertThat(newPasswordLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void forgotPasswordDoesNotFailForUnknownEmail() {
        passwordResetEmailSender.clear();

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/password/forgot", Map.of(
                "email", "missing@example.com"
        ), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("sent")).isEqualTo(false);
        assertThat(passwordResetEmailSender.sentTokens()).isEmpty();
    }

    private ResponseEntity<Map> register(String email, String password, String nickname) {
        return restTemplate.postForEntity("/api/v1/auth/register", Map.of(
                "email", email,
                "password", password,
                "nickname", nickname,
                "verificationCode", sendRegisterCode(email)
        ), Map.class);
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

    static class CapturingSecurityAlertEmailSender implements SecurityAlertEmailSender {

        @Override
        public void sendSecurityAlertEmail(EmailAddress email, String alertType, String alertDetail, String alertTime) {
        }
    }
}
