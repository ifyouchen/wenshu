package com.czx.wenshu.application.project;

public record UpdateChapterCommand(String title, String content, String outline, String status) {
}