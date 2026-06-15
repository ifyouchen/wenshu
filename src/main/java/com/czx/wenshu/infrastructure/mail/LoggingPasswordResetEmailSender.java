package com.czx.wenshu.infrastructure.mail;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.domain.user.EmailAddress;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingPasswordResetEmailSender implements PasswordResetEmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetEmailSender.class);

    @Override
    public void sendPasswordResetEmail(EmailAddress email, String rawToken, Instant expiresAt) {
        log.info("Password reset email queued: email={}, expiresAt={}", email.value(), expiresAt);
    }
}
