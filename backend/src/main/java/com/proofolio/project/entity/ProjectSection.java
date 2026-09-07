package com.proofolio.project.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "project_sections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectSection {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SectionType type;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public ProjectSection(Project project, SectionType type, String body, int sortOrder) {
        this.project = project;
        this.type = type;
        this.body = body == null ? "" : body;
        this.sortOrder = sortOrder;
    }
}
