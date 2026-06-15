package com.czx.wenshu.application.auth;

import java.time.Instant;

public record ResendVerifyEmailResult(boolean sent, Instant expiresAt) {
}
