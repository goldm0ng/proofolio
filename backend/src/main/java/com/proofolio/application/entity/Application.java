package com.proofolio.application.entity;

import com.proofolio.common.BaseEntity;
import com.proofolio.company.entity.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "position_title", nullable = false, length = 300)
    private String positionTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.INTERESTED;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "posting_url", length = 1000)
    private String postingUrl;

    @Column(name = "employment_type", length = 100)
    private String employmentType;

    @Column(length = 200)
    private String location;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(columnDefinition = "text")
    private String result;

    @Column(columnDefinition = "text")
    private String retrospective;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    private List<ApplicationStatusHistory> statusHistory = new ArrayList<>();

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Requirement> requirements = new ArrayList<>();

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<EssayQuestion> essayQuestions = new ArrayList<>();

    public Application(UUID ownerId, Company company, String positionTitle) {
        super(ownerId);
        this.company = company;
        this.positionTitle = positionTitle;
    }

    public void changeStatus(ApplicationStatus newStatus, String note) {
        this.status = newStatus;
        statusHistory.add(new ApplicationStatusHistory(this, newStatus, note));
    }

    public Requirement addRequirement(String title, RequirementKind kind, String note) {
        Requirement r = new Requirement(this, title, kind, note, requirements.size());
        requirements.add(r);
        return r;
    }

    public EssayQuestion addEssayQuestion(String question, Integer maxLength, Integer sortOrder) {
        EssayQuestion q = new EssayQuestion(this, question, maxLength, sortOrder == null ? essayQuestions.size() : sortOrder);
        essayQuestions.add(q);
        return q;
    }
}
