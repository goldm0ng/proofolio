package com.proofolio.ai.service;

import com.proofolio.ai.entity.AiCall;

/** Thin boundary around the Claude API so services and tests can swap it. */
public interface ClaudeGateway {

    boolean isConfigured();

    String model();

    /**
     * Runs a structured-output call: stable system prompt (cached) + user content, returns the typed result
     * together with the persisted call log.
     */
    <T> Result<T> structured(String task, String systemPrompt, String userContent, Class<T> schema);

    record Result<T>(T value, AiCall call) {}
}
