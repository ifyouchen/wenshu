package com.czx.wenshu.application.user;

import java.util.List;

public record CreateStyleTemplateCommand(String name, String templateType, List<String> genres, String prompt) {
}
