package com.lionsclub.api.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record RsvpResponse(
        UUID id,
        UUID eventId,
        UUID memberId,
        String status,
        int plusOne,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        MemberInfo member
) {
    public record MemberInfo(
            UUID id,
            String firstName,
            String lastName,
            String email
    ) {}
}