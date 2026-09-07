package com.proofolio.experience.repository;

import com.proofolio.experience.entity.Experience;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperienceRepository extends JpaRepository<Experience, UUID> {

    @EntityGraph(attributePaths = {"project"})
    @Query("""
            select e from Experience e
            where e.ownerId = :ownerId
              and (:projectId is null or e.project.id = :projectId)
              and (:q = '' or lower(e.title) like lower(concat('%', :q, '%'))
                   or lower(e.situation) like lower(concat('%', :q, '%'))
                   or lower(e.action) like lower(concat('%', :q, '%'))
                   or lower(e.result) like lower(concat('%', :q, '%')))
            order by e.updatedAt desc
            """)
    List<Experience> search(@Param("ownerId") UUID ownerId, @Param("projectId") UUID projectId, @Param("q") String q);

    @EntityGraph(attributePaths = {"project"})
    List<Experience> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);

    long countByProjectId(UUID projectId);

    @EntityGraph(attributePaths = {"project"})
    Optional<Experience> findDetailByIdAndOwnerId(UUID id, UUID ownerId);

    @Query(value = "select t as tag, count(*) as cnt from experiences, unnest(tags) as t where owner_id = :ownerId group by t order by cnt desc, t asc",
            nativeQuery = true)
    List<TagCount> countTags(@Param("ownerId") UUID ownerId);

    interface TagCount {
        String getTag();
        long getCnt();
    }
}
