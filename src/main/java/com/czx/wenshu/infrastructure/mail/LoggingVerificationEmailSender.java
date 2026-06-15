package com.czx.wenshu.infrastructure.mail;

import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.domain.user.EmailAddress;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingVerificationEmailSender implements VerificationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingVerificationEmailSender.class);

    @Override
    public void sendVerificationEmail(EmailAddress email, String rawToken, Instant expiresAt) {
        log.info("Verification email queued: email={}, expiresAt={}", email.value(), expiresAt);
    }
}
