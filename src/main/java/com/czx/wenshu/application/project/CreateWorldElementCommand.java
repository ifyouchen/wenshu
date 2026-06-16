package com.czx.wenshu.application.project;

import java.util.List;

public record CreateWorldElementCommand(String type, String name, String description, List<String> aliases) {
}