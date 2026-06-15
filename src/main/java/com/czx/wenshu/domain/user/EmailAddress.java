package com.czx.wenshu.domain.user;

import java.util.Locale;
import java.util.regex.Pattern;

public record EmailAddress(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > 255 || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
    }
}
