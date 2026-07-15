package com.lionsclub.api.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RsvpRequest(
        @NotBlank String status,
        @Min(0) int plusOne,
        @Size(max = 500) String notes
) {}
