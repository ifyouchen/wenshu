package com.czx.wenshu.application.auth;

import java.time.Instant;

public record SendRegisterCodeResult(boolean sent, Instant expiresAt) {
}
