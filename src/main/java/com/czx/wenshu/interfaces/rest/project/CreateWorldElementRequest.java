package com.czx.wenshu.interfaces.rest.project;

import java.util.List;

public record CreateWorldElementRequest(String type, String name, String description, List<String> aliases) {
}