package com.czx.wenshu.common.result;

import java.time.Instant;

public class Result<T> {

    private final int code;
    private final String message;
    private final T data;
    private final Instant timestamp;

    public Result(int code, String message, T data, Instant timestamp) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data, Instant.now());
    }

    public static Result<Void> ok() {
        return ok(null);
    }

    public static Result<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.message());
    }

    public static Result<Void> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.code(), message, null, Instant.now());
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
