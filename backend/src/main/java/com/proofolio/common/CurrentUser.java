package com.proofolio.common;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Holds the owner id for the current request. Set by {@link ApiTokenFilter}, read by services.
 * Every owned query is scoped by {@link #id()}; a row belonging to someone else is simply "not found".
 */
@Component
public class CurrentUser {

    private static final ThreadLocal<UUID> HOLDER = new ThreadLocal<>();

    public UUID id() {
        UUID id = HOLDER.get();
        if (id == null) {
            throw new IllegalStateException("No current user bound to this thread");
        }
        return id;
    }

    void bind(UUID id) {
        HOLDER.set(id);
    }

    void clear() {
        HOLDER.remove();
    }
}
