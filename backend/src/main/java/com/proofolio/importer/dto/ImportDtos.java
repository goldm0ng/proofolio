package com.proofolio.importer.dto;

import com.proofolio.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public final class ImportDtos {
    private ImportDtos() {}

    public record NotionMapping(
            @NotBlank String companyName,
            String positionTitle,
            String deadlineAt,
            String status,
            String postingUrl,
            List<String> notes,
            Map<String, ApplicationStatus> statusValues
    ) {}

    public record PreviewResponse(List<String> columns, List<List<String>> sampleRows, NotionMapping suggestedMapping, int rowCount) {}

    public record ImportError(int row, String message) {}

    public record ImportResult(int created, int updated, int skipped, List<ImportError> errors) {}
}
