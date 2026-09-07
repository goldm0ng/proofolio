package com.proofolio.application.dto;

import com.proofolio.ai.dto.JobPostingExtraction;
import com.proofolio.application.entity.*;
import com.proofolio.company.dto.CompanyDtos.CompanyResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApplicationDtos {
    private ApplicationDtos() {}

    public record RequirementInput(@NotBlank @Size(max = 300) String title, RequirementKind kind, String note) {}
    public record RequirementPatch(@Size(max = 300) String title, RequirementKind kind, Boolean done, String note) {}
    public record RequirementResponse(UUID id, String title, RequirementKind kind, boolean done, String note, int sortOrder) {
        public static RequirementResponse from(Requirement r) {
            return new RequirementResponse(r.getId(), r.getTitle(), r.getKind(), r.isDone(), r.getNote(), r.getSortOrder());
        }
    }

    public record EssayQuestionInput(@NotBlank String question, Integer maxLength, Integer sortOrder) {}
    public record EssayQuestionPatch(String question, Integer maxLength, String draft, EssayStatus status, Integer sortOrder) {}
    public record EssayQuestionResponse(UUID id, String question, Integer maxLength, String draft, EssayStatus status,
                                        int sortOrder, Instant updatedAt) {
        public static EssayQuestionResponse from(EssayQuestion q) {
            return new EssayQuestionResponse(q.getId(), q.getQuestion(), q.getMaxLength(), q.getDraft(), q.getStatus(),
                    q.getSortOrder(), q.getUpdatedAt());
        }
    }

    public record StatusHistoryEntry(ApplicationStatus status, String note, Instant changedAt) {
        public static StatusHistoryEntry from(ApplicationStatusHistory h) {
            return new StatusHistoryEntry(h.getStatus(), h.getNote(), h.getChangedAt());
        }
    }

    public record ApplicationCreate(
            UUID companyId,
            @Size(max = 200) String companyName,
            @NotBlank @Size(max = 300) String positionTitle,
            @Size(max = 1000) String postingUrl,
            Instant deadlineAt,
            @Size(max = 100) String employmentType,
            @Size(max = 200) String location,
            String notes,
            ApplicationStatus status,
            List<@Valid RequirementInput> requirements,
            List<@Valid EssayQuestionInput> essayQuestions
    ) {}

    public record ApplicationUpdate(
            @Size(max = 300) String positionTitle,
            @Size(max = 1000) String postingUrl,
            Instant deadlineAt,
            @Size(max = 100) String employmentType,
            @Size(max = 200) String location,
            String notes,
            String result,
            String retrospective
    ) {}

    public record StatusChange(@NotNull ApplicationStatus status, String note) {}

    public record FromExtraction(@NotNull @Valid JobPostingExtraction extraction, UUID companyId) {}

    public record BoardColumn(ApplicationStatus status, List<ApplicationCard> items) {}
    public record Board(List<BoardColumn> columns) {}

    public record ApplicationDetail(
            UUID id, UUID companyId, String companyName, String positionTitle, ApplicationStatus status,
            Instant deadlineAt, String postingUrl,
            int requirementsDone, int requirementsTotal, int essayDone, int essayTotal,
            Instant updatedAt,
            String employmentType, String location, String notes, String result, String retrospective,
            CompanyResponse company,
            List<RequirementResponse> requirements,
            List<EssayQuestionResponse> essayQuestions,
            List<StatusHistoryEntry> statusHistory,
            Instant createdAt
    ) {
        public static ApplicationDetail from(Application a) {
            ApplicationCard c = ApplicationCard.from(a);
            return new ApplicationDetail(c.id(), c.companyId(), c.companyName(), c.positionTitle(), c.status(),
                    c.deadlineAt(), c.postingUrl(), c.requirementsDone(), c.requirementsTotal(), c.essayDone(), c.essayTotal(),
                    c.updatedAt(),
                    a.getEmploymentType(), a.getLocation(), a.getNotes(), a.getResult(), a.getRetrospective(),
                    CompanyResponse.from(a.getCompany()),
                    a.getRequirements().stream().map(RequirementResponse::from).toList(),
                    a.getEssayQuestions().stream().map(EssayQuestionResponse::from).toList(),
                    a.getStatusHistory().stream().map(StatusHistoryEntry::from).toList(),
                    a.getCreatedAt());
        }
    }
}
