// src/main/java/com/school/lessonplan/ai/dto/ExtractedPlanText.java
package com.school.lessonplan.ai.dto;

public class ExtractedPlanText {

    private String text;
    private int textLength;
    private int pageOrSlideCount;
    private String fileName;
    private String extension;

    public ExtractedPlanText() {}

    public ExtractedPlanText(String text, int textLength, int pageOrSlideCount, String fileName, String extension) {
        this.text = text;
        this.textLength = textLength;
        this.pageOrSlideCount = pageOrSlideCount;
        this.fileName = fileName;
        this.extension = extension;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getTextLength() {
        return textLength;
    }

    public void setTextLength(int textLength) {
        this.textLength = textLength;
    }

    public int getPageOrSlideCount() {
        return pageOrSlideCount;
    }

    public void setPageOrSlideCount(int pageOrSlideCount) {
        this.pageOrSlideCount = pageOrSlideCount;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}
