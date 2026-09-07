package com.proofolio.application.repository;

import com.proofolio.application.entity.Application;
import com.proofolio.application.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByCompanyId(UUID companyId);

    @EntityGraph(attributePaths = {"company"})
    @Query("""
            select a from Application a
            where a.ownerId = :ownerId
              and (:status is null or a.status = :status)
              and (:companyId is null or a.company.id = :companyId)
            order by case when a.deadlineAt is null then 1 else 0 end, a.deadlineAt asc, a.updatedAt desc
            """)
    List<Application> search(@Param("ownerId") UUID ownerId, @Param("status") ApplicationStatus status,
                             @Param("companyId") UUID companyId);

    @EntityGraph(attributePaths = {"company"})
    @Query("""
            select a from Application a
            where a.ownerId = :ownerId and a.deadlineAt >= :from and a.deadlineAt < :to and a.status not in :closed
            order by a.deadlineAt asc
            """)
    List<Application> upcoming(@Param("ownerId") UUID ownerId, @Param("from") Instant from, @Param("to") Instant to,
                               @Param("closed") Collection<ApplicationStatus> closed);

    @EntityGraph(attributePaths = {"company"})
    Optional<Application> findDetailByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<Application> findFirstByCompanyIdAndPositionTitleIgnoreCase(UUID companyId, String positionTitle);
}
