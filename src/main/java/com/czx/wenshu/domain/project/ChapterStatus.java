package com.czx.wenshu.domain.project;

public enum ChapterStatus {

    PENDING("pending"),
    DRAFT("draft"),
    COMPLETED("completed");

    private final String value;

    ChapterStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ChapterStatus fromValue(String value) {
        for (ChapterStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return PENDING;
    }
}