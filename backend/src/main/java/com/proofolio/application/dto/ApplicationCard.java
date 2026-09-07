package com.proofolio.application.dto;

import com.proofolio.application.entity.Application;
import com.proofolio.application.entity.ApplicationStatus;
import com.proofolio.application.entity.EssayStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationCard(
        UUID id, UUID companyId, String companyName, String positionTitle, ApplicationStatus status,
        Instant deadlineAt, String postingUrl,
        int requirementsDone, int requirementsTotal, int essayDone, int essayTotal,
        Instant updatedAt
) {
    public static ApplicationCard from(Application a) {
        int reqDone = (int) a.getRequirements().stream().filter(r -> r.isDone()).count();
        int essayDone = (int) a.getEssayQuestions().stream().filter(q -> q.getStatus() == EssayStatus.DONE).count();
        return new ApplicationCard(a.getId(), a.getCompany().getId(), a.getCompany().getName(), a.getPositionTitle(),
                a.getStatus(), a.getDeadlineAt(), a.getPostingUrl(),
                reqDone, a.getRequirements().size(), essayDone, a.getEssayQuestions().size(), a.getUpdatedAt());
    }
}
