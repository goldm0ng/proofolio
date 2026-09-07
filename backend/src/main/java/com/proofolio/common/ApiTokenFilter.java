package com.proofolio.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Single-token auth for /api/**. The one configured token resolves to the seeded owner user
 * (app.owner-username, default "me"). Health endpoint stays public.
 */
@Component
public class ApiTokenFilter extends OncePerRequestFilter {

    private final String token;
    private final String ownerUsername;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private volatile UUID ownerId;

    public ApiTokenFilter(@Value("${app.api-token}") String token,
                          @Value("${app.owner-username:me}") String ownerUsername,
                          ObjectMapper objectMapper, UserRepository userRepository, CurrentUser currentUser) {
        this.token = token;
        this.ownerUsername = ownerUsername;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && constantTimeEquals(header.substring(7).trim(), token)) {
            currentUser.bind(resolveOwnerId());
            try {
                chain.doFilter(request, response);
            } finally {
                currentUser.clear();
            }
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of("UNAUTHORIZED", "API 토큰이 없거나 올바르지 않습니다"));
    }

    private UUID resolveOwnerId() {
        UUID id = ownerId;
        if (id == null) {
            id = userRepository.findByUsername(ownerUsername)
                    .map(User::getId)
                    .orElseThrow(() -> new IllegalStateException("Owner user not found: " + ownerUsername));
            ownerId = id;
        }
        return id;
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
