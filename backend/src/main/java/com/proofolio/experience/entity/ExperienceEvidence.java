package com.proofolio.experience.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "experience_evidence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExperienceEvidence {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experience_id", nullable = false)
    private Experience experience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvidenceType type;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(length = 300)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public ExperienceEvidence(Experience experience, EvidenceType type, String url, String label, int sortOrder) {
        this.experience = experience;
        this.type = type == null ? EvidenceType.URL : type;
        this.url = url;
        this.label = label;
        this.sortOrder = sortOrder;
    }
}
