package com.proofolio.experience.controller;

import com.proofolio.experience.dto.ExperienceDtos.*;
import com.proofolio.experience.service.ExperienceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/experiences")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService experienceService;

    @GetMapping
    public List<ExperienceSummary> list(@RequestParam(required = false) UUID projectId,
                                        @RequestParam(required = false) String tag,
                                        @RequestParam(required = false) String q) {
        return experienceService.list(projectId, tag, q);
    }

    @GetMapping("/tags")
    public List<TagCountResponse> tags() {
        return experienceService.tags();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExperienceResponse create(@Valid @RequestBody ExperienceUpsert req) {
        return experienceService.create(req);
    }

    @GetMapping("/{id}")
    public ExperienceResponse get(@PathVariable UUID id) {
        return experienceService.get(id);
    }

    @PutMapping("/{id}")
    public ExperienceResponse update(@PathVariable UUID id, @Valid @RequestBody ExperienceUpsert req) {
        return experienceService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        experienceService.delete(id);
    }
}
