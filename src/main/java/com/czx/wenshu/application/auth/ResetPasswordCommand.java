package com.czx.wenshu.application.auth;

public record ResetPasswordCommand(String token, String newPassword) {
}
