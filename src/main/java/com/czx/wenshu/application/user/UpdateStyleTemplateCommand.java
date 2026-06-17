package com.czx.wenshu.application.user;

import java.util.List;

public record UpdateStyleTemplateCommand(String name, String templateType, List<String> genres, String prompt) {
}
