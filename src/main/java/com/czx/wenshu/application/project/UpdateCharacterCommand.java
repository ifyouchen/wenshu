package com.czx.wenshu.application.project;

public record UpdateCharacterCommand(String name, String role, String appearance, String personality,
                                      String abilities, String speechStyle, String status) {
}