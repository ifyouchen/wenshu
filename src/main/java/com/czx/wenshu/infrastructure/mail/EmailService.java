package com.czx.wenshu.infrastructure.mail;

import com.czx.wenshu.domain.user.EmailAddress;
import java.time.Instant;
import java.util.Map;

public interface EmailService {

    void sendVerificationEmail(EmailAddress email, String rawToken, Instant expiresAt);

    void sendPasswordResetEmail(EmailAddress email, String rawToken, Instant expiresAt);

    void sendSecurityAlertEmail(EmailAddress email, String alertType, String alertDetail, String alertTime);
}