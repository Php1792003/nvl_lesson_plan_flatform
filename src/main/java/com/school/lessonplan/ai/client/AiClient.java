// src/main/java/com/school/lessonplan/ai/client/AiClient.java
package com.school.lessonplan.ai.client;

public interface AiClient {

    /**
     * Completes a prompt by sending systemPrompt and userMessage to the AI provider.
     *
     * @param systemPrompt The expert role system instructions
     * @param userMessage The metadata, legal basis, and extracted plan text
     * @return Raw JSON text response from the AI
     */
    String complete(String systemPrompt, String userMessage);

    /**
     * Checks if the AI client is properly configured (e.g. API key present).
     */
    boolean isConfigured();
}
