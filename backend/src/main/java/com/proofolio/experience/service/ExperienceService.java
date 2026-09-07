package com.proofolio.experience.service;

import com.proofolio.common.ApiException;
import com.proofolio.common.CurrentUser;
import com.proofolio.common.Visibility;
import com.proofolio.experience.dto.ExperienceDtos.*;
import com.proofolio.experience.entity.Experience;
import com.proofolio.experience.entity.ExperienceEvidence;
import com.proofolio.experience.repository.ExperienceRepository;
import com.proofolio.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<ExperienceSummary> list(UUID projectId, String tag, String q) {
        String query = (q == null || q.isBlank()) ? "" : q.trim();
        return experienceRepository.search(currentUser.id(), projectId, query).stream()
                .filter(e -> tag == null || tag.isBlank() || e.getTags().contains(tag.trim()))
                .map(ExperienceSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagCountResponse> tags() {
        return experienceRepository.countTags(currentUser.id()).stream()
                .map(t -> new TagCountResponse(t.getTag(), t.getCnt()))
                .toList();
    }

    public ExperienceResponse create(ExperienceUpsert req) {
        Experience e = new Experience(currentUser.id(), req.title().trim());
        apply(e, req);
        experienceRepository.save(e);
        return ExperienceResponse.from(e);
    }

    @Transactional(readOnly = true)
    public ExperienceResponse get(UUID id) {
        return ExperienceResponse.from(require(id));
    }

    public ExperienceResponse update(UUID id, ExperienceUpsert req) {
        Experience e = require(id);
        e.setTitle(req.title().trim());
        apply(e, req);
        return ExperienceResponse.from(e);
    }

    public void delete(UUID id) {
        experienceRepository.delete(require(id));
    }

    private Experience require(UUID id) {
        return experienceRepository.findDetailByIdAndOwnerId(id, currentUser.id()).orElseThrow(() -> ApiException.notFound("Experience", id));
    }

    private void apply(Experience e, ExperienceUpsert req) {
        e.setProject(req.projectId() == null ? null
                : projectRepository.findByIdAndOwnerId(req.projectId(), currentUser.id()).orElseThrow(() -> ApiException.notFound("Project", req.projectId())));
        e.setSituation(nz(req.situation()));
        e.setTask(nz(req.task()));
        e.setAction(nz(req.action()));
        e.setResult(nz(req.result()));
        e.setTags(req.tags() == null ? new ArrayList<>()
                : new ArrayList<>(req.tags().stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList()));
        e.setVisibility(req.visibility() == null ? Visibility.PRIVATE : req.visibility());
        List<ExperienceEvidence> fresh = new ArrayList<>();
        if (req.evidence() != null) {
            for (int i = 0; i < req.evidence().size(); i++) {
                EvidenceInput in = req.evidence().get(i);
                fresh.add(new ExperienceEvidence(e, in.type(), in.url().trim(), in.label(), i));
            }
        }
        e.replaceEvidence(fresh);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
