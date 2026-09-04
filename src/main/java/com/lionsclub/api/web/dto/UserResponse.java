package com.lionsclub.api.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String role,
        String avatar,
        boolean emailVerified,
        LocalDateTime createdAt
) {}
