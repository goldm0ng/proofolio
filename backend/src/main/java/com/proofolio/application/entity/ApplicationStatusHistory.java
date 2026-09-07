package com.proofolio.application.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationStatusHistory {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    ApplicationStatusHistory(Application application, ApplicationStatus status, String note) {
        this.application = application;
        this.status = status;
        this.note = note;
    }
}
