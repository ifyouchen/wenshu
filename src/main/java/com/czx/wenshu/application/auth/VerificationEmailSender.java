package com.czx.wenshu.application.auth;

import com.czx.wenshu.domain.user.EmailAddress;
import java.time.Instant;

public interface VerificationEmailSender {

    void sendVerificationEmail(EmailAddress email, String rawToken, Instant expiresAt);
}
