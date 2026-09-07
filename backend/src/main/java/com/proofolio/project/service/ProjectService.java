package com.proofolio.project.service;

import com.proofolio.common.ApiException;
import com.proofolio.common.CurrentUser;
import com.proofolio.common.Visibility;
import com.proofolio.experience.dto.ExperienceDtos.ExperienceSummary;
import com.proofolio.experience.repository.ExperienceRepository;
import com.proofolio.project.dto.ProjectDtos.*;
import com.proofolio.project.entity.*;
import com.proofolio.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ExperienceRepository experienceRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<ProjectSummary> list() {
        return projectRepository.findByOwnerIdOrderByUpdatedAtDesc(currentUser.id()).stream()
                .map(p -> ProjectSummary.from(p, experienceRepository.countByProjectId(p.getId())))
                .toList();
    }

    public ProjectDetail create(ProjectUpsert req) {
        Project p = new Project(currentUser.id(), req.name().trim());
        apply(p, req);
        projectRepository.save(p);
        return detail(p);
    }

    @Transactional(readOnly = true)
    public ProjectDetail get(UUID id) {
        return detail(require(id));
    }

    public ProjectDetail update(UUID id, ProjectUpsert req) {
        Project p = require(id);
        p.setName(req.name().trim());
        apply(p, req);
        return detail(p);
    }

    public ProjectDetail replaceSections(UUID id, SectionsReplace req) {
        Project p = require(id);
        // last one wins per type, keep first-seen order
        Map<SectionType, String> byType = new LinkedHashMap<>();
        req.sections().forEach(s -> byType.put(s.type(), s.body() == null ? "" : s.body()));
        List<ProjectSection> fresh = new ArrayList<>();
        int i = 0;
        for (Map.Entry<SectionType, String> e : byType.entrySet()) {
            fresh.add(new ProjectSection(p, e.getKey(), e.getValue(), i++));
        }
        // delete old rows before inserting: (project_id, type) is unique
        p.getSections().clear();
        projectRepository.flush();
        p.replaceSections(fresh);
        projectRepository.flush();
        return detail(p);
    }

    public ProjectDetail replaceTechStack(UUID id, TechStackReplace req) {
        Project p = require(id);
        List<ProjectTechStack> fresh = new ArrayList<>();
        for (int i = 0; i < req.items().size(); i++) {
            TechStackInput t = req.items().get(i);
            fresh.add(new ProjectTechStack(p, t.category().trim().toUpperCase(), t.name().trim(), i));
        }
        p.replaceTechStack(fresh);
        projectRepository.flush();
        return detail(p);
    }

    public ProjectDetail replaceMetrics(UUID id, MetricsReplace req) {
        Project p = require(id);
        List<ProjectMetric> fresh = new ArrayList<>();
        for (int i = 0; i < req.items().size(); i++) {
            MetricInput m = req.items().get(i);
            fresh.add(new ProjectMetric(p, m.label().trim(), m.value().trim(), i));
        }
        p.replaceMetrics(fresh);
        projectRepository.flush();
        return detail(p);
    }

    public void delete(UUID id) {
        Project p = require(id);
        experienceRepository.findByProjectIdOrderByUpdatedAtDesc(id).forEach(e -> e.setProject(null));
        projectRepository.delete(p);
    }

    public Project require(UUID id) {
        return projectRepository.findDetailByIdAndOwnerId(id, currentUser.id()).orElseThrow(() -> ApiException.notFound("Project", id));
    }

    private ProjectDetail detail(Project p) {
        List<ExperienceSummary> experiences = experienceRepository.findByProjectIdOrderByUpdatedAtDesc(p.getId())
                .stream().map(ExperienceSummary::from).toList();
        return ProjectDetail.from(p, experiences);
    }

    private static void apply(Project p, ProjectUpsert req) {
        p.setTagline(req.tagline());
        p.setStartedAt(req.startedAt());
        p.setEndedAt(req.endedAt());
        p.setTeamSize(req.teamSize());
        p.setRole(req.role());
        p.setRepoUrl(req.repoUrl());
        p.setDeployUrl(req.deployUrl());
        p.setVisibility(req.visibility() == null ? Visibility.PRIVATE : req.visibility());
    }
}
