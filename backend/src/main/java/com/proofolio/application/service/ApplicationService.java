package com.proofolio.application.service;

import com.proofolio.ai.dto.JobPostingExtraction;
import com.proofolio.application.dto.ApplicationCard;
import com.proofolio.application.dto.ApplicationDtos.*;
import com.proofolio.application.entity.*;
import com.proofolio.application.repository.ApplicationRepository;
import com.proofolio.common.ApiException;
import com.proofolio.common.CurrentUser;
import com.proofolio.company.entity.Company;
import com.proofolio.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ApplicationRepository applicationRepository;
    private final CompanyService companyService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<ApplicationCard> list(ApplicationStatus status, UUID companyId) {
        return applicationRepository.search(currentUser.id(), status, companyId).stream().map(ApplicationCard::from).toList();
    }

    @Transactional(readOnly = true)
    public Board board() {
        Map<ApplicationStatus, List<ApplicationCard>> grouped = applicationRepository.search(currentUser.id(), null, null).stream()
                .map(ApplicationCard::from)
                .collect(Collectors.groupingBy(ApplicationCard::status));
        List<BoardColumn> columns = Arrays.stream(ApplicationStatus.values())
                .map(s -> new BoardColumn(s, grouped.getOrDefault(s, List.of())))
                .toList();
        return new Board(columns);
    }

    @Transactional(readOnly = true)
    public List<ApplicationCard> upcoming(int days) {
        Instant from = LocalDate.now(KST).atStartOfDay(KST).toInstant();
        Instant to = from.plus(Duration.ofDays(Math.max(days, 0) + 1L));
        return applicationRepository.upcoming(currentUser.id(), from, to, ApplicationStatus.CLOSED).stream()
                .map(ApplicationCard::from).toList();
    }

    public ApplicationDetail create(ApplicationCreate req) {
        Company company = resolveCompany(req.companyId(), req.companyName());
        Application app = new Application(currentUser.id(), company, req.positionTitle().trim());
        app.setPostingUrl(req.postingUrl());
        app.setDeadlineAt(req.deadlineAt());
        app.setEmploymentType(req.employmentType());
        app.setLocation(req.location());
        app.setNotes(req.notes());
        app.changeStatus(req.status() == null ? ApplicationStatus.INTERESTED : req.status(), "생성");
        if (req.requirements() != null) {
            req.requirements().forEach(r -> app.addRequirement(r.title(), r.kind(), r.note()));
        }
        if (req.essayQuestions() != null) {
            req.essayQuestions().forEach(q -> app.addEssayQuestion(q.question(), q.maxLength(), q.sortOrder()));
        }
        applicationRepository.save(app);
        return ApplicationDetail.from(app);
    }

    public ApplicationDetail createFromExtraction(FromExtraction req) {
        JobPostingExtraction x = req.extraction();
        Company company = req.companyId() != null
                ? companyService.require(req.companyId())
                : resolveCompany(null, x.companyName());
        Application app = new Application(currentUser.id(), company, blankTo(x.positionTitle(), "직무 미상"));
        app.setPostingUrl(x.sourceUrl());
        app.setDeadlineAt(parseDeadline(x.deadlineAt()));
        app.setEmploymentType(x.employmentType());
        app.setLocation(x.location());
        app.setNotes(buildNotes(x));
        app.changeStatus(ApplicationStatus.INTERESTED, "공고 추출로 생성");
        if (x.requiredDocuments() != null) {
            x.requiredDocuments().stream()
                    .filter(d -> d != null && d.title() != null && !d.title().isBlank())
                    .forEach(d -> app.addRequirement(d.title().trim(), d.kind(), null));
        }
        if (x.essayQuestions() != null) {
            x.essayQuestions().stream()
                    .filter(q -> q != null && q.question() != null && !q.question().isBlank())
                    .forEach(q -> app.addEssayQuestion(q.question().trim(), q.maxLength(), null));
        }
        applicationRepository.save(app);
        return ApplicationDetail.from(app);
    }

    @Transactional(readOnly = true)
    public ApplicationDetail get(UUID id) {
        return ApplicationDetail.from(require(id));
    }

    public ApplicationDetail update(UUID id, ApplicationUpdate req) {
        Application app = require(id);
        if (req.positionTitle() != null && !req.positionTitle().isBlank()) app.setPositionTitle(req.positionTitle().trim());
        if (req.postingUrl() != null) app.setPostingUrl(req.postingUrl());
        if (req.deadlineAt() != null) app.setDeadlineAt(req.deadlineAt());
        if (req.employmentType() != null) app.setEmploymentType(req.employmentType());
        if (req.location() != null) app.setLocation(req.location());
        if (req.notes() != null) app.setNotes(req.notes());
        if (req.result() != null) app.setResult(req.result());
        if (req.retrospective() != null) app.setRetrospective(req.retrospective());
        return ApplicationDetail.from(app);
    }

    public ApplicationDetail changeStatus(UUID id, StatusChange req) {
        Application app = require(id);
        app.changeStatus(req.status(), req.note());
        return ApplicationDetail.from(app);
    }

    public void delete(UUID id) {
        applicationRepository.delete(require(id));
    }

    // ---- requirements ----

    public RequirementResponse addRequirement(UUID appId, RequirementInput req) {
        Application app = require(appId);
        Requirement r = app.addRequirement(req.title().trim(), req.kind(), req.note());
        applicationRepository.flush();
        return RequirementResponse.from(r);
    }

    public RequirementResponse patchRequirement(UUID appId, UUID rid, RequirementPatch req) {
        Requirement r = findRequirement(require(appId), rid);
        if (req.title() != null && !req.title().isBlank()) r.setTitle(req.title().trim());
        if (req.kind() != null) r.setKind(req.kind());
        if (req.done() != null) r.setDone(req.done());
        if (req.note() != null) r.setNote(req.note());
        return RequirementResponse.from(r);
    }

    public void deleteRequirement(UUID appId, UUID rid) {
        Application app = require(appId);
        app.getRequirements().remove(findRequirement(app, rid));
    }

    // ---- essay questions ----

    public EssayQuestionResponse addEssayQuestion(UUID appId, EssayQuestionInput req) {
        Application app = require(appId);
        EssayQuestion q = app.addEssayQuestion(req.question().trim(), req.maxLength(), req.sortOrder());
        applicationRepository.flush();
        return EssayQuestionResponse.from(q);
    }

    public EssayQuestionResponse patchEssayQuestion(UUID appId, UUID qid, EssayQuestionPatch req) {
        EssayQuestion q = findEssay(require(appId), qid);
        if (req.question() != null && !req.question().isBlank()) q.setQuestion(req.question().trim());
        if (req.maxLength() != null) q.setMaxLength(req.maxLength());
        if (req.draft() != null) {
            q.setDraft(req.draft());
            if (req.status() == null && q.getStatus() == EssayStatus.EMPTY && !req.draft().isBlank()) {
                q.setStatus(EssayStatus.DRAFT);
            }
        }
        if (req.status() != null) q.setStatus(req.status());
        if (req.sortOrder() != null) q.setSortOrder(req.sortOrder());
        q.setUpdatedAt(Instant.now());
        return EssayQuestionResponse.from(q);
    }

    public void deleteEssayQuestion(UUID appId, UUID qid) {
        Application app = require(appId);
        app.getEssayQuestions().remove(findEssay(app, qid));
    }

    // ---- helpers ----

    public Application require(UUID id) {
        return applicationRepository.findDetailByIdAndOwnerId(id, currentUser.id()).orElseThrow(() -> ApiException.notFound("Application", id));
    }

    private Company resolveCompany(UUID companyId, String companyName) {
        if (companyId != null) return companyService.require(companyId);
        if (companyName == null || companyName.isBlank()) {
            throw ApiException.validation("companyId 또는 companyName 중 하나는 필요합니다");
        }
        return companyService.findOrCreateByName(companyName);
    }

    private static Requirement findRequirement(Application app, UUID rid) {
        return app.getRequirements().stream().filter(r -> r.getId().equals(rid)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Requirement", rid));
    }

    private static EssayQuestion findEssay(Application app, UUID qid) {
        return app.getEssayQuestions().stream().filter(q -> q.getId().equals(qid)).findFirst()
                .orElseThrow(() -> ApiException.notFound("EssayQuestion", qid));
    }

    private static String blankTo(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s.trim();
    }

    private static String buildNotes(JobPostingExtraction x) {
        StringBuilder sb = new StringBuilder();
        if (x.summary() != null && !x.summary().isBlank()) sb.append(x.summary().trim()).append("\n\n");
        if (x.hiringStages() != null && !x.hiringStages().isEmpty()) {
            sb.append("전형 절차\n");
            x.hiringStages().forEach(s -> sb.append("- ").append(s).append('\n'));
            sb.append('\n');
        }
        if (x.requiredSkills() != null && !x.requiredSkills().isEmpty()) {
            sb.append("자격 요건: ").append(String.join(", ", x.requiredSkills())).append('\n');
        }
        if (x.preferredSkills() != null && !x.preferredSkills().isEmpty()) {
            sb.append("우대 사항: ").append(String.join(", ", x.preferredSkills())).append('\n');
        }
        String notes = sb.toString().trim();
        return notes.isEmpty() ? null : notes;
    }

    /** Accepts ISO-8601 instant/offset datetime, or a plain date (treated as 23:59 KST). */
    static Instant parseDeadline(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        try { return Instant.parse(s); } catch (DateTimeParseException ignored) {}
        try { return OffsetDateTime.parse(s).toInstant(); } catch (DateTimeParseException ignored) {}
        try { return java.time.LocalDateTime.parse(s).atZone(KST).toInstant(); } catch (DateTimeParseException ignored) {}
        try { return LocalDate.parse(s).atTime(23, 59).atZone(KST).toInstant(); } catch (DateTimeParseException ignored) {}
        return null;
    }
}
