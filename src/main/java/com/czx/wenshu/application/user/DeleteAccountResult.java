package com.czx.wenshu.application.user;

import java.time.Instant;

public record DeleteAccountResult(String restoreToken, Instant restoreTokenExpiresAt) {
}