package com.lionsclub.api.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record NewsResponse(
        UUID id,
        String title,
        String slug,
        String content,
        String excerpt,
        String featuredImage,
        String category,
        String status,
        UUID authorId,
        String authorName,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record NewsSummary(
            UUID id,
            String title,
            String slug,
            String excerpt,
            String featuredImage,
            String category,
            String status,
            UUID authorId,
            String authorName,
            LocalDateTime publishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record NewsPage(
            List<NewsSummary> data,
            long total,
            int page,
            int limit,
            int totalPages
    ) {}
}
