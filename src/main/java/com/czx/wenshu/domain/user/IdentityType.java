package com.czx.wenshu.domain.user;

public enum IdentityType {

    WEB_NOVEL_AUTHOR("web_novel_author"),
    SHORT_DRAMA_WRITER("short_drama_writer"),
    NEW_AUTHOR("new_author");

    private final String value;

    IdentityType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static IdentityType fromValue(String value) {
        for (IdentityType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return NEW_AUTHOR;
    }
}
