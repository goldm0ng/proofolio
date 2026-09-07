package com.proofolio.experience.dto;

import com.proofolio.common.Visibility;
import com.proofolio.experience.entity.EvidenceType;
import com.proofolio.experience.entity.Experience;
import com.proofolio.experience.entity.ExperienceEvidence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ExperienceDtos {
    private ExperienceDtos() {}

    public record EvidenceInput(EvidenceType type, @NotBlank @Size(max = 1000) String url, @Size(max = 300) String label) {}
    public record EvidenceResponse(EvidenceType type, String url, String label) {
        public static EvidenceResponse from(ExperienceEvidence e) {
            return new EvidenceResponse(e.getType(), e.getUrl(), e.getLabel());
        }
    }

    public record ExperienceUpsert(
            UUID projectId,
            @NotBlank @Size(max = 300) String title,
            String situation, String task, String action, String result,
            List<String> tags,
            List<@Valid EvidenceInput> evidence,
            Visibility visibility
    ) {}

    public record ExperienceSummary(UUID id, UUID projectId, String projectName, String title, List<String> tags,
                                    Visibility visibility, Instant updatedAt) {
        public static ExperienceSummary from(Experience e) {
            return new ExperienceSummary(e.getId(),
                    e.getProject() == null ? null : e.getProject().getId(),
                    e.getProject() == null ? null : e.getProject().getName(),
                    e.getTitle(), List.copyOf(e.getTags()), e.getVisibility(), e.getUpdatedAt());
        }
    }

    public record ExperienceResponse(UUID id, UUID projectId, String projectName, String title, List<String> tags,
                                     Visibility visibility, Instant updatedAt,
                                     String situation, String task, String action, String result,
                                     List<EvidenceResponse> evidence, Instant createdAt) {
        public static ExperienceResponse from(Experience e) {
            ExperienceSummary s = ExperienceSummary.from(e);
            return new ExperienceResponse(s.id(), s.projectId(), s.projectName(), s.title(), s.tags(), s.visibility(),
                    s.updatedAt(), e.getSituation(), e.getTask(), e.getAction(), e.getResult(),
                    e.getEvidence().stream().map(EvidenceResponse::from).toList(), e.getCreatedAt());
        }
    }

    public record TagCountResponse(String tag, long count) {}
}
