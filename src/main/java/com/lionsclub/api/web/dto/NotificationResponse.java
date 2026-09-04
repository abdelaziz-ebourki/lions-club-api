package com.lionsclub.api.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String description,
        String targetUrl,
        boolean read,
        LocalDateTime createdAt
) {
    public record NotificationsResponse(
            List<NotificationResponse> notifications,
            long unreadCount
    ) {}
}
