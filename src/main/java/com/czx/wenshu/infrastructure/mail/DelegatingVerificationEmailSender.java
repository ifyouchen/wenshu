package com.czx.wenshu.infrastructure.mail;

import com.czx.wenshu.application.auth.VerificationEmailSender;
import com.czx.wenshu.domain.user.EmailAddress;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelegatingVerificationEmailSender implements VerificationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(DelegatingVerificationEmailSender.class);

    private final EmailService emailService;

    public DelegatingVerificationEmailSender(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void sendVerificationEmail(EmailAddress email, String rawToken, Instant expiresAt) {
        log.info("Sending verification email to: {}", email.value());
        emailService.sendVerificationEmail(email, rawToken, expiresAt);
    }
}