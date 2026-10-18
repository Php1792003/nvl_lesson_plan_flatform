// src/main/java/com/school/lessonplan/ai/dto/ChecklistItem.java
package com.school.lessonplan.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChecklistItem {

    private ChecklistLevel level;
    private String issue;
    private String fix;

    public ChecklistItem() {}

    public ChecklistItem(ChecklistLevel level, String issue, String fix) {
        this.level = level;
        this.issue = issue;
        this.fix = fix;
    }

    public ChecklistItem(String level, String issue, String fix) {
        this.level = ChecklistLevel.fromString(level);
        this.issue = issue;
        this.fix = fix;
    }

    public ChecklistLevel getLevel() {
        return level;
    }

    public void setLevel(ChecklistLevel level) {
        this.level = level;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getFix() {
        return fix;
    }

    public void setFix(String fix) {
        this.fix = fix;
    }
}
