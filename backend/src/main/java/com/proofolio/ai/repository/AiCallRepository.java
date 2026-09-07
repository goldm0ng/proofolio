package com.proofolio.ai.repository;

import com.proofolio.ai.entity.AiCall;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiCallRepository extends JpaRepository<AiCall, UUID> {
    List<AiCall> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable pageable);
}
