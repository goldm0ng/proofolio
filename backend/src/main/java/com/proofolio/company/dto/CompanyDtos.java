package com.proofolio.company.dto;

import com.proofolio.application.dto.ApplicationCard;
import com.proofolio.company.entity.Company;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CompanyDtos {
    private CompanyDtos() {}

    public record CompanyUpsert(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String industry,
            @Size(max = 500) String website,
            String talentProfile,
            List<String> coreValues,
            List<String> techStack,
            String hiringProcess,
            String notes
    ) {}

    public record CompanyResponse(
            UUID id, String name, String industry, String website, String talentProfile,
            List<String> coreValues, List<String> techStack, String hiringProcess, String notes,
            Instant createdAt, Instant updatedAt
    ) {
        public static CompanyResponse from(Company c) {
            return new CompanyResponse(c.getId(), c.getName(), c.getIndustry(), c.getWebsite(), c.getTalentProfile(),
                    List.copyOf(c.getCoreValues()), List.copyOf(c.getTechStack()), c.getHiringProcess(), c.getNotes(),
                    c.getCreatedAt(), c.getUpdatedAt());
        }
    }

    public record CompanyDetail(
            UUID id, String name, String industry, String website, String talentProfile,
            List<String> coreValues, List<String> techStack, String hiringProcess, String notes,
            Instant createdAt, Instant updatedAt,
            List<ApplicationCard> applications
    ) {
        public static CompanyDetail of(Company c, List<ApplicationCard> applications) {
            return new CompanyDetail(c.getId(), c.getName(), c.getIndustry(), c.getWebsite(), c.getTalentProfile(),
                    List.copyOf(c.getCoreValues()), List.copyOf(c.getTechStack()), c.getHiringProcess(), c.getNotes(),
                    c.getCreatedAt(), c.getUpdatedAt(), applications);
        }
    }
}
