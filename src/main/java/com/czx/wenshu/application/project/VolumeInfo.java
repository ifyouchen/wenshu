package com.czx.wenshu.application.project;

import com.czx.wenshu.domain.project.Volume;

public record VolumeInfo(String id, String projectId, String title, String conflict, int sortOrder, String createdAt) {

    public static VolumeInfo from(Volume v) {
        return new VolumeInfo(v.id().toString(), v.projectId().toString(), v.title(), v.conflict(),
                v.sortOrder(), v.createdAt().toString());
    }
}