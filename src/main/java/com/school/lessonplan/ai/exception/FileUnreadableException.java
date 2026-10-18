// src/main/java/com/school/lessonplan/ai/exception/FileUnreadableException.java
package com.school.lessonplan.ai.exception;

public class FileUnreadableException extends RuntimeException {

    private final String code = "FILE_UNREADABLE";

    public FileUnreadableException(String message) {
        super(message);
    }

    public FileUnreadableException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getCode() {
        return code;
    }
}
