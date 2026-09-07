package com.proofolio.company.repository;

import com.proofolio.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByIdAndOwnerId(UUID id, UUID ownerId);
    Optional<Company> findByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);
    List<Company> findByOwnerIdAndNameContainingIgnoreCaseOrderByNameAsc(UUID ownerId, String q);
    List<Company> findByOwnerIdOrderByNameAsc(UUID ownerId);
}
