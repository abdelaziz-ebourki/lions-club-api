package com.lionsclub.api.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        String name,
        String role,
        String avatar,
        String bio,
        LocalDate joinedAt,
        String email,
        String phone,
        MemberSocials socials
) {
    public record MemberSocials(
            String linkedin,
            String facebook,
            String instagram
    ) {}
}
