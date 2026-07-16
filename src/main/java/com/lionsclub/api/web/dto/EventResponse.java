package com.lionsclub.api.web.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        String date,
        String time,
        String location,
        String category,
        String status,
        int rsvpCount,
        Map<String, Integer> rsvpBreakdown,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
