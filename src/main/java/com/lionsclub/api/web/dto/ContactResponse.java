package com.lionsclub.api.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        String name,
        String email,
        String subject,
        String message,
        LocalDateTime createdAt,
        String status
) {
    public record ContactRequest(
            @NotBlank @Size(min = 2, max = 100) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 5, max = 200) String subject,
            @NotBlank @Size(min = 10, max = 2000) String message
    ) {}

    public record ContactStatusRequest(
            @NotBlank String status
    ) {}
}
