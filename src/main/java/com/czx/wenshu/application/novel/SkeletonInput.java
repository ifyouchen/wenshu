package com.czx.wenshu.application.novel;

import java.util.UUID;

/** 骨架生成请求参数（P5-04）。 */
public record SkeletonInput(
        UUID projectId,
        String genre,
        String synopsis,
        String worldview,
        Integer targetWords) {
}
