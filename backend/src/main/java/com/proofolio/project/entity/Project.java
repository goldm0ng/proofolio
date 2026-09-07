package com.proofolio.project.entity;

import com.proofolio.common.BaseEntity;
import com.proofolio.common.Visibility;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String tagline;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "ended_at")
    private LocalDate endedAt;

    @Column(name = "team_size")
    private Integer teamSize;

    @Column(length = 200)
    private String role;

    @Column(name = "repo_url", length = 500)
    private String repoUrl;

    @Column(name = "deploy_url", length = 500)
    private String deployUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility = Visibility.PRIVATE;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProjectSection> sections = new ArrayList<>();

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProjectTechStack> techStack = new ArrayList<>();

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProjectMetric> metrics = new ArrayList<>();

    public Project(UUID ownerId, String name) {
        super(ownerId);
        this.name = name;
    }

    public void replaceSections(List<ProjectSection> fresh) {
        sections.clear();
        sections.addAll(fresh);
    }

    public void replaceTechStack(List<ProjectTechStack> fresh) {
        techStack.clear();
        techStack.addAll(fresh);
    }

    public void replaceMetrics(List<ProjectMetric> fresh) {
        metrics.clear();
        metrics.addAll(fresh);
    }
}
