package com.czx.wenshu.domain.task;

public enum AsyncTaskStatus {
    PENDING("pending"),
    RUNNING("running"),
    COMPLETED("completed"),
    FAILED("failed");

    private final String value;

    AsyncTaskStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static AsyncTaskStatus fromValue(String value) {
        for (AsyncTaskStatus s : values()) {
            if (s.value.equals(value)) return s;
        }
        return PENDING;
    }
}
