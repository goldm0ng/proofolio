package com.proofolio.company.controller;

import com.proofolio.company.dto.CompanyDtos.CompanyDetail;
import com.proofolio.company.dto.CompanyDtos.CompanyResponse;
import com.proofolio.company.dto.CompanyDtos.CompanyUpsert;
import com.proofolio.company.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public List<CompanyResponse> list(@RequestParam(required = false) String q) {
        return companyService.list(q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(@Valid @RequestBody CompanyUpsert req) {
        return companyService.create(req);
    }

    @GetMapping("/{id}")
    public CompanyDetail get(@PathVariable UUID id) {
        return companyService.get(id);
    }

    @PutMapping("/{id}")
    public CompanyResponse update(@PathVariable UUID id, @Valid @RequestBody CompanyUpsert req) {
        return companyService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        companyService.delete(id);
    }
}
