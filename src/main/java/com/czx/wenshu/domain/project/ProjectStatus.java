package com.czx.wenshu.domain.project;

public enum ProjectStatus {

    DRAFT("draft"),
    DELETED("deleted");

    private final String value;

    ProjectStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ProjectStatus fromValue(String value) {
        for (ProjectStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return DRAFT;
    }
}