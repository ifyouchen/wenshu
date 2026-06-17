package com.czx.wenshu.application.project;

import java.util.List;

public record ChapterContextInfo(ChapterInfo chapter, List<CharacterInfo> characters,
                                 List<WorldElementInfo> worldElements,
                                 List<KeyEventInfo> keyEvents) {
}
