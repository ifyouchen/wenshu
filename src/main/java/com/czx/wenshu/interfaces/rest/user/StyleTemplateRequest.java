package com.czx.wenshu.interfaces.rest.user;

import java.util.List;

public record StyleTemplateRequest(String name, String templateType, List<String> genres, String prompt) {
}
