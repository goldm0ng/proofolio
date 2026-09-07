package com.proofolio.application.service;

import com.proofolio.application.dto.ApplicationCard;
import com.proofolio.application.repository.ApplicationRepository;
import com.proofolio.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Read-side helper shared with the company module (avoids a service cycle). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationQueryService {

    private final ApplicationRepository applicationRepository;
    private final CurrentUser currentUser;

    public List<ApplicationCard> cardsForCompany(UUID companyId) {
        return applicationRepository.search(currentUser.id(), null, companyId).stream().map(ApplicationCard::from).toList();
    }
}
