package com.proofolio.experience.entity;

import com.proofolio.common.BaseEntity;
import com.proofolio.common.Visibility;
import com.proofolio.project.entity.Project;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "experiences")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Experience extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String situation = "";

    @Column(nullable = false, columnDefinition = "text")
    private String task = "";

    @Column(nullable = false, columnDefinition = "text")
    private String action = "";

    @Column(nullable = false, columnDefinition = "text")
    private String result = "";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> tags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility = Visibility.PRIVATE;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "experience", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ExperienceEvidence> evidence = new ArrayList<>();

    public Experience(UUID ownerId, String title) {
        super(ownerId);
        this.title = title;
    }

    public void replaceEvidence(List<ExperienceEvidence> fresh) {
        evidence.clear();
        evidence.addAll(fresh);
    }
}
