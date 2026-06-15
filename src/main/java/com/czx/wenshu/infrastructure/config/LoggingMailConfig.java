package com.czx.wenshu.infrastructure.config;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.application.user.SecurityAlertEmailSender;
import com.czx.wenshu.domain.user.EmailAddress;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "wenshu.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingMailConfig {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailConfig.class);

    @Bean
    public VerificationEmailSender verificationEmailSender() {
        return (email, rawToken, expiresAt) -> log.info("Verification email queued: email={}, expiresAt={}", email.value(), expiresAt);
    }

    @Bean
    public PasswordResetEmailSender passwordResetEmailSender() {
        return (email, rawToken, expiresAt) -> log.info("Password reset email queued: email={}, expiresAt={}", email.value(), expiresAt);
    }

    @Bean
    public SecurityAlertEmailSender securityAlertEmailSender() {
        return (email, alertType, alertDetail, alertTime) -> log.info("Security alert email queued: email={}, alertType={}", email.value(), alertType);
    }
}