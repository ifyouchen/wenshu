package com.czx.wenshu.application.auth;

import java.time.Instant;

public record ForgotPasswordResult(boolean sent, Instant expiresAt) {
}
