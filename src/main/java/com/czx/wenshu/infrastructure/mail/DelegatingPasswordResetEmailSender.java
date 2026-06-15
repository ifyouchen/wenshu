package com.czx.wenshu.infrastructure.mail;

import com.czx.wenshu.application.auth.PasswordResetEmailSender;
import com.czx.wenshu.domain.user.EmailAddress;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelegatingPasswordResetEmailSender implements PasswordResetEmailSender {

    private static final Logger log = LoggerFactory.getLogger(DelegatingPasswordResetEmailSender.class);

    private final EmailService emailService;

    public DelegatingPasswordResetEmailSender(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void sendPasswordResetEmail(EmailAddress email, String rawToken, Instant expiresAt) {
        log.info("Sending password reset email to: {}", email.value());
        emailService.sendPasswordResetEmail(email, rawToken, expiresAt);
    }
}