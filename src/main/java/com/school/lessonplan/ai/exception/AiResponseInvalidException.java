// src/main/java/com/school/lessonplan/ai/exception/AiResponseInvalidException.java
package com.school.lessonplan.ai.exception;

public class AiResponseInvalidException extends RuntimeException {

    private final String code = "AI_RESPONSE_INVALID";

    public AiResponseInvalidException(String message) {
        super(message);
    }

    public AiResponseInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getCode() {
        return code;
    }
}
