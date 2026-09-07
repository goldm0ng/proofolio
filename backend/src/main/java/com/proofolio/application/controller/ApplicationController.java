package com.proofolio.application.controller;

import com.proofolio.application.dto.ApplicationCard;
import com.proofolio.application.dto.ApplicationDtos.*;
import com.proofolio.application.entity.ApplicationStatus;
import com.proofolio.application.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public List<ApplicationCard> list(@RequestParam(required = false) ApplicationStatus status,
                                      @RequestParam(required = false) UUID companyId) {
        return applicationService.list(status, companyId);
    }

    @GetMapping("/board")
    public Board board() {
        return applicationService.board();
    }

    @GetMapping("/upcoming")
    public List<ApplicationCard> upcoming(@RequestParam(defaultValue = "14") int days) {
        return applicationService.upcoming(days);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDetail create(@Valid @RequestBody ApplicationCreate req) {
        return applicationService.create(req);
    }

    @PostMapping("/from-extraction")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDetail fromExtraction(@Valid @RequestBody FromExtraction req) {
        return applicationService.createFromExtraction(req);
    }

    @GetMapping("/{id}")
    public ApplicationDetail get(@PathVariable UUID id) {
        return applicationService.get(id);
    }

    @PutMapping("/{id}")
    public ApplicationDetail update(@PathVariable UUID id, @Valid @RequestBody ApplicationUpdate req) {
        return applicationService.update(id, req);
    }

    @PatchMapping("/{id}/status")
    public ApplicationDetail changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusChange req) {
        return applicationService.changeStatus(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        applicationService.delete(id);
    }

    // requirements

    @PostMapping("/{id}/requirements")
    @ResponseStatus(HttpStatus.CREATED)
    public RequirementResponse addRequirement(@PathVariable UUID id, @Valid @RequestBody RequirementInput req) {
        return applicationService.addRequirement(id, req);
    }

    @PatchMapping("/{id}/requirements/{rid}")
    public RequirementResponse patchRequirement(@PathVariable UUID id, @PathVariable UUID rid,
                                                @Valid @RequestBody RequirementPatch req) {
        return applicationService.patchRequirement(id, rid, req);
    }

    @DeleteMapping("/{id}/requirements/{rid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRequirement(@PathVariable UUID id, @PathVariable UUID rid) {
        applicationService.deleteRequirement(id, rid);
    }

    // essay questions

    @PostMapping("/{id}/essay-questions")
    @ResponseStatus(HttpStatus.CREATED)
    public EssayQuestionResponse addEssay(@PathVariable UUID id, @Valid @RequestBody EssayQuestionInput req) {
        return applicationService.addEssayQuestion(id, req);
    }

    @PatchMapping("/{id}/essay-questions/{qid}")
    public EssayQuestionResponse patchEssay(@PathVariable UUID id, @PathVariable UUID qid,
                                            @Valid @RequestBody EssayQuestionPatch req) {
        return applicationService.patchEssayQuestion(id, qid, req);
    }

    @DeleteMapping("/{id}/essay-questions/{qid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEssay(@PathVariable UUID id, @PathVariable UUID qid) {
        applicationService.deleteEssayQuestion(id, qid);
    }
}
