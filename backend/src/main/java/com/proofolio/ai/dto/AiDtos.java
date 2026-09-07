package com.proofolio.ai.dto;

import com.proofolio.ai.entity.AiCall;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class AiDtos {
    private AiDtos() {}

    public record ExtractPostingRequest(String url, String text) {}

    public record ExtractPostingResponse(JobPostingExtraction extraction, String sourceText, UUID callId) {}

    public record AiCallResponse(UUID id, String task, String model, AiCall.Status status, long inputTokens,
                                 long outputTokens, long cacheReadTokens, BigDecimal costUsd, long durationMs,
                                 String errorMessage, Instant createdAt) {
        public static AiCallResponse from(AiCall c) {
            return new AiCallResponse(c.getId(), c.getTask(), c.getModel(), c.getStatus(), c.getInputTokens(),
                    c.getOutputTokens(), c.getCacheReadTokens(), c.getCostUsd(), c.getDurationMs(), c.getErrorMessage(),
                    c.getCreatedAt());
        }
    }
}
