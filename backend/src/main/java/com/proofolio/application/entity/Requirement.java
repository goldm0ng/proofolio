package com.proofolio.application.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "requirements")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Requirement {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(nullable = false, length = 300)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequirementKind kind;

    @Column(nullable = false)
    private boolean done;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    Requirement(Application application, String title, RequirementKind kind, String note, int sortOrder) {
        this.application = application;
        this.title = title;
        this.kind = kind == null ? RequirementKind.OTHER : kind;
        this.note = note;
        this.sortOrder = sortOrder;
    }
}
