package com.czx.wenshu.interfaces.rest.project;

public record CreateCharacterRequest(String name, String role, String appearance, String personality,
                                     String abilities, String speechStyle, String status) {
}
