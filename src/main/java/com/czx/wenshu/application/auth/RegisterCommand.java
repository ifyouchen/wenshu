package com.czx.wenshu.application.auth;

public record RegisterCommand(String email, String password, String nickname) {
}
