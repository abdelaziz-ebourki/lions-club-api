package com.lionsclub.api.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @JsonProperty("remember_me") @JsonAlias("rememberMe") Boolean rememberMe
) {}
