package com.czx.wenshu.interfaces.rest.auth;

import com.czx.wenshu.application.auth.SendRegisterCodeResult;
import java.time.Instant;

public record SendRegisterCodeResponse(boolean sent, Instant expiresAt) {

    static SendRegisterCodeResponse from(SendRegisterCodeResult result) {
        return new SendRegisterCodeResponse(result.sent(), result.expiresAt());
    }
}
