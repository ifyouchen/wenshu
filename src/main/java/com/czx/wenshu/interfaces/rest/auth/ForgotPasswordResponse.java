package com.czx.wenshu.interfaces.rest.auth;

import com.czx.wenshu.application.auth.ForgotPasswordResult;
import java.time.Instant;

public record ForgotPasswordResponse(boolean sent, Instant expiresAt) {

    public static ForgotPasswordResponse from(ForgotPasswordResult result) {
        return new ForgotPasswordResponse(result.sent(), result.expiresAt());
    }
}
