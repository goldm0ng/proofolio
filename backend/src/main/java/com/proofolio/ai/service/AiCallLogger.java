package com.proofolio.ai.service;

import com.proofolio.ai.entity.AiCall;
import com.proofolio.ai.repository.AiCallRepository;
import com.proofolio.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Writes call logs in their own transaction so a failed request still leaves a record. */
@Component
@RequiredArgsConstructor
public class AiCallLogger {

    private final AiCallRepository repository;
    private final CurrentUser currentUser;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiCall ok(String task, String model, long in, long out, long cacheRead, long durationMs) {
        return repository.save(new AiCall(currentUser.id(), task, model, AiCall.Status.OK, in, out, cacheRead,
                ModelPricing.cost(model, in, out, cacheRead), durationMs, null));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiCall error(String task, String model, long durationMs, String message) {
        return repository.save(new AiCall(currentUser.id(), task, model, AiCall.Status.ERROR, 0, 0, 0, BigDecimal.ZERO, durationMs, message));
    }
}
