package com.czx.wenshu.interfaces.rest.auth;

import com.czx.wenshu.application.auth.ResendVerifyEmailResult;
import java.time.Instant;

public record ResendVerifyEmailResponse(boolean sent, Instant expiresAt) {

    public static ResendVerifyEmailResponse from(ResendVerifyEmailResult result) {
        return new ResendVerifyEmailResponse(result.sent(), result.expiresAt());
    }
}
