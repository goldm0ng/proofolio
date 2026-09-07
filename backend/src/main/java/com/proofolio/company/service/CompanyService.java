package com.proofolio.company.service;

import com.proofolio.application.repository.ApplicationRepository;
import com.proofolio.application.service.ApplicationQueryService;
import com.proofolio.common.ApiException;
import com.proofolio.common.CurrentUser;
import com.proofolio.company.dto.CompanyDtos.CompanyDetail;
import com.proofolio.company.dto.CompanyDtos.CompanyResponse;
import com.proofolio.company.dto.CompanyDtos.CompanyUpsert;
import com.proofolio.company.entity.Company;
import com.proofolio.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationQueryService applicationQueryService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<CompanyResponse> list(String q) {
        List<Company> companies = (q == null || q.isBlank())
                ? companyRepository.findByOwnerIdOrderByNameAsc(currentUser.id())
                : companyRepository.findByOwnerIdAndNameContainingIgnoreCaseOrderByNameAsc(currentUser.id(), q.trim());
        return companies.stream().map(CompanyResponse::from).toList();
    }

    public CompanyResponse create(CompanyUpsert req) {
        companyRepository.findByOwnerIdAndNameIgnoreCase(currentUser.id(), req.name().trim()).ifPresent(c -> {
            throw ApiException.conflict("같은 이름의 회사가 이미 있습니다: " + c.getName());
        });
        Company company = new Company(currentUser.id(), req.name().trim());
        apply(company, req);
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public CompanyDetail get(UUID id) {
        Company company = require(id);
        return CompanyDetail.of(company, applicationQueryService.cardsForCompany(id));
    }

    public CompanyResponse update(UUID id, CompanyUpsert req) {
        Company company = require(id);
        companyRepository.findByOwnerIdAndNameIgnoreCase(currentUser.id(), req.name().trim())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw ApiException.conflict("같은 이름의 회사가 이미 있습니다: " + other.getName());
                });
        company.setName(req.name().trim());
        apply(company, req);
        return CompanyResponse.from(company);
    }

    public void delete(UUID id) {
        Company company = require(id);
        if (applicationRepository.existsByCompanyId(id)) {
            throw ApiException.conflict("이 회사에 연결된 지원이 있어 삭제할 수 없습니다");
        }
        companyRepository.delete(company);
    }

    /** Finds by name (case-insensitive) or creates a bare company. */
    public Company findOrCreateByName(String name) {
        String trimmed = name.trim();
        UUID owner = currentUser.id();
        return companyRepository.findByOwnerIdAndNameIgnoreCase(owner, trimmed)
                .orElseGet(() -> companyRepository.save(new Company(owner, trimmed)));
    }

    public Company require(UUID id) {
        return companyRepository.findByIdAndOwnerId(id, currentUser.id()).orElseThrow(() -> ApiException.notFound("Company", id));
    }

    private static void apply(Company c, CompanyUpsert req) {
        c.setIndustry(req.industry());
        c.setWebsite(req.website());
        c.setTalentProfile(req.talentProfile());
        c.setCoreValues(req.coreValues() == null ? new ArrayList<>() : new ArrayList<>(req.coreValues()));
        c.setTechStack(req.techStack() == null ? new ArrayList<>() : new ArrayList<>(req.techStack()));
        c.setHiringProcess(req.hiringProcess());
        c.setNotes(req.notes());
    }
}
