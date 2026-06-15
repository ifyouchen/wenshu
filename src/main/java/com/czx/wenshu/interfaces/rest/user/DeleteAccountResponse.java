package com.czx.wenshu.interfaces.rest.user;

public record DeleteAccountResponse(
        String restoreToken,
        String restoreTokenExpiresAt
) {
}