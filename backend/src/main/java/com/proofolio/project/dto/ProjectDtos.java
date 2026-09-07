package com.proofolio.project.dto;

import com.proofolio.common.Visibility;
import com.proofolio.experience.dto.ExperienceDtos.ExperienceSummary;
import com.proofolio.project.entity.Project;
import com.proofolio.project.entity.SectionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProjectDtos {
    private ProjectDtos() {}

    public record ProjectUpsert(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String tagline,
            LocalDate startedAt, LocalDate endedAt, Integer teamSize,
            @Size(max = 200) String role,
            @Size(max = 500) String repoUrl,
            @Size(max = 500) String deployUrl,
            Visibility visibility
    ) {}

    public record SectionInput(@NotNull SectionType type, String body) {}
    public record SectionsReplace(@NotNull List<@Valid SectionInput> sections) {}
    public record SectionResponse(SectionType type, String body) {}

    public record TechStackInput(@NotBlank @Size(max = 50) String category, @NotBlank @Size(max = 100) String name) {}
    public record TechStackReplace(@NotNull List<@Valid TechStackInput> items) {}
    public record TechStackResponse(String category, String name) {}

    public record MetricInput(@NotBlank @Size(max = 200) String label, @NotBlank @Size(max = 200) String value) {}
    public record MetricsReplace(@NotNull List<@Valid MetricInput> items) {}
    public record MetricResponse(String label, String value) {}

    public record ProjectSummary(UUID id, String name, String tagline, LocalDate startedAt, LocalDate endedAt,
                                 Integer teamSize, String role, String repoUrl, String deployUrl, Visibility visibility,
                                 long experienceCount, List<String> techStack, Instant updatedAt) {
        public static ProjectSummary from(Project p, long experienceCount) {
            return new ProjectSummary(p.getId(), p.getName(), p.getTagline(), p.getStartedAt(), p.getEndedAt(),
                    p.getTeamSize(), p.getRole(), p.getRepoUrl(), p.getDeployUrl(), p.getVisibility(), experienceCount,
                    p.getTechStack().stream().map(t -> t.getName()).toList(), p.getUpdatedAt());
        }
    }

    public record ProjectDetail(UUID id, String name, String tagline, LocalDate startedAt, LocalDate endedAt,
                                Integer teamSize, String role, String repoUrl, String deployUrl, Visibility visibility,
                                long experienceCount, List<String> techStack, Instant updatedAt,
                                List<SectionResponse> sections, List<TechStackResponse> techStackItems,
                                List<MetricResponse> metrics, List<ExperienceSummary> experiences, Instant createdAt) {
        public static ProjectDetail from(Project p, List<ExperienceSummary> experiences) {
            ProjectSummary s = ProjectSummary.from(p, experiences.size());
            return new ProjectDetail(s.id(), s.name(), s.tagline(), s.startedAt(), s.endedAt(), s.teamSize(), s.role(),
                    s.repoUrl(), s.deployUrl(), s.visibility(), s.experienceCount(), s.techStack(), s.updatedAt(),
                    p.getSections().stream().map(x -> new SectionResponse(x.getType(), x.getBody())).toList(),
                    p.getTechStack().stream().map(x -> new TechStackResponse(x.getCategory(), x.getName())).toList(),
                    p.getMetrics().stream().map(x -> new MetricResponse(x.getLabel(), x.getValue())).toList(),
                    experiences, p.getCreatedAt());
        }
    }
}
