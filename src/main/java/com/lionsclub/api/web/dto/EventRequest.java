package com.lionsclub.api.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventRequest(
        @NotBlank @Size(min = 3) String title,
        @NotBlank @Size(min = 10) String description,
        @NotBlank String date,
        @NotBlank String time,
        @NotBlank @Size(min = 3) String location,
        @NotBlank String category,
        String status
) {}
