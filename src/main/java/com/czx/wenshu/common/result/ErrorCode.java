package com.czx.wenshu.common.result;

public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    FORBIDDEN(40300, "无权访问该资源"),
    NOT_FOUND(40400, "资源不存在"),
    VERSION_CONFLICT(40901, "数据已被其他设备修改"),
    RATE_LIMITED(42900, "请求过于频繁"),
    INTERNAL_ERROR(50000, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
