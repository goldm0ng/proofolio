package com.proofolio.project.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "project_metrics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMetric {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(nullable = false, length = 200)
    private String value;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public ProjectMetric(Project project, String label, String value, int sortOrder) {
        this.project = project;
        this.label = label;
        this.value = value;
        this.sortOrder = sortOrder;
    }
}
