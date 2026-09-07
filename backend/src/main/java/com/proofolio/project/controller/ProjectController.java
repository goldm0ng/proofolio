package com.proofolio.project.controller;

import com.proofolio.project.dto.ProjectDtos.*;
import com.proofolio.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectSummary> list() {
        return projectService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetail create(@Valid @RequestBody ProjectUpsert req) {
        return projectService.create(req);
    }

    @GetMapping("/{id}")
    public ProjectDetail get(@PathVariable UUID id) {
        return projectService.get(id);
    }

    @PutMapping("/{id}")
    public ProjectDetail update(@PathVariable UUID id, @Valid @RequestBody ProjectUpsert req) {
        return projectService.update(id, req);
    }

    @PutMapping("/{id}/sections")
    public ProjectDetail sections(@PathVariable UUID id, @Valid @RequestBody SectionsReplace req) {
        return projectService.replaceSections(id, req);
    }

    @PutMapping("/{id}/tech-stack")
    public ProjectDetail techStack(@PathVariable UUID id, @Valid @RequestBody TechStackReplace req) {
        return projectService.replaceTechStack(id, req);
    }

    @PutMapping("/{id}/metrics")
    public ProjectDetail metrics(@PathVariable UUID id, @Valid @RequestBody MetricsReplace req) {
        return projectService.replaceMetrics(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        projectService.delete(id);
    }
}
