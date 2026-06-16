package com.czx.wenshu.application.novel;

import java.util.List;

/**
 * 润色结果（P5-09）。
 * basicAnnotations 在基础校正时非空；rewritten 在进阶润色和风格重塑时非空。
 */
public record PolishResult(String mode,
                            List<PolishAnnotation> basicAnnotations,
                            String rewritten) {

    public static PolishResult ofAnnotations(List<PolishAnnotation> annotations) {
        return new PolishResult("basic", annotations, null);
    }

    public static PolishResult ofRewritten(String mode, String text) {
        return new PolishResult(mode, null, text);
    }
}
