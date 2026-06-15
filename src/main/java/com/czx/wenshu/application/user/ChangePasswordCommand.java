package com.czx.wenshu.application.user;

import java.util.UUID;

public record ChangePasswordCommand(UUID id, String currentPassword, String newPassword) {
}