package com.proofolio.application.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "essay_questions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EssayQuestion {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(columnDefinition = "text")
    private String draft;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EssayStatus status = EssayStatus.EMPTY;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    EssayQuestion(Application application, String question, Integer maxLength, int sortOrder) {
        this.application = application;
        this.question = question;
        this.maxLength = maxLength;
        this.sortOrder = sortOrder;
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
