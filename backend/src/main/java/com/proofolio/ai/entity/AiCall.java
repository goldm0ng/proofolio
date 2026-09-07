package com.proofolio.ai.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_calls")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiCall {

    public enum Status { OK, ERROR }

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 100)
    private String task;

    @Column(nullable = false, length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(name = "input_tokens", nullable = false)
    private long inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private long outputTokens;

    @Column(name = "cache_read_tokens", nullable = false)
    private long cacheReadTokens;

    @Column(name = "cost_usd", nullable = false, precision = 12, scale = 6)
    private BigDecimal costUsd = BigDecimal.ZERO;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public AiCall(UUID ownerId, String task, String model, Status status, long inputTokens, long outputTokens, long cacheReadTokens,
                  BigDecimal costUsd, long durationMs, String errorMessage) {
        this.ownerId = ownerId;
        this.task = task;
        this.model = model;
        this.status = status;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.cacheReadTokens = cacheReadTokens;
        this.costUsd = costUsd;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
    }
}
