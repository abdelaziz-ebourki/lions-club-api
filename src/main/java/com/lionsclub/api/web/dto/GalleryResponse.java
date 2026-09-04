package com.lionsclub.api.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GalleryResponse(
        UUID id,
        String title,
        String description,
        String imageUrl,
        String thumbnailUrl,
        String category,
        UUID eventId,
        List<String> tags,
        LocalDateTime uploadedAt,
        String uploadedBy
) {
    public record GalleryPage(
            List<GalleryResponse> data,
            long total,
            int page,
            int limit,
            int totalPages
    ) {}

    public record GalleryItemRequest(
            String title,
            String description,
            String category,
            UUID eventId,
            List<String> tags,
            String imageUrl,
            String thumbnailUrl
    ) {}

    public record UploadResponse(
            String imageUrl,
            String thumbnailUrl
    ) {}
}
