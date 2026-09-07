package com.proofolio.application.entity;

import java.util.EnumSet;
import java.util.Set;

public enum ApplicationStatus {
    INTERESTED, PLANNED, WRITING, SUBMITTED, DOCUMENT_PASSED, TEST, INTERVIEW, ACCEPTED, REJECTED, WITHDRAWN;

    public static final Set<ApplicationStatus> CLOSED = EnumSet.of(ACCEPTED, REJECTED, WITHDRAWN);

    public boolean isClosed() {
        return CLOSED.contains(this);
    }
}
