package com.proofolio.project.repository;

import com.proofolio.project.entity.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @EntityGraph(attributePaths = {"techStack"})
    List<Project> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    @EntityGraph(attributePaths = {"techStack"})
    Optional<Project> findDetailByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<Project> findByIdAndOwnerId(UUID id, UUID ownerId);
}
