package com.lionsclub.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

public record ForumResponse(
        UUID id,
        UUID categoryId,
        String title,
        String author,
        String content,
        LocalDateTime createdAt,
        String status,
        long replyCount,
        long viewCount,
        LocalDateTime lastActivity
) {
    public record ForumCategoryResponse(
            UUID id,
            String name,
            String description,
            long threadCount,
            long postCount,
            String icon
    ) {}

    public record ForumReplyResponse(
            UUID id,
            UUID threadId,
            String author,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            UUID parentReplyId
    ) {}

    public record CreateThreadRequest(
            @NotBlank @Size(min = 5, max = 200) String title,
            @NotBlank @Size(min = 10, max = 5000) String content
    ) {}

    public record CreateReplyRequest(
            UUID threadId,
            @NotBlank @Size(min = 5, max = 5000) String content,
            UUID parentReplyId
    ) {}

    public record UpdateThreadStatusRequest(
            @NotBlank String status
    ) {}
}
