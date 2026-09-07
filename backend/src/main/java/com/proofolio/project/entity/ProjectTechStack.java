package com.proofolio.project.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "project_tech_stack")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectTechStack {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public ProjectTechStack(Project project, String category, String name, int sortOrder) {
        this.project = project;
        this.category = category;
        this.name = name;
        this.sortOrder = sortOrder;
    }
}
