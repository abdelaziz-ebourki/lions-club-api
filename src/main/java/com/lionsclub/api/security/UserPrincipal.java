package com.lionsclub.api.security;

import com.lionsclub.api.domain.user.Role;
import java.security.Principal;
import java.util.UUID;

public record UserPrincipal(
    UUID userId,
    String email,
    Role role,
    String firstName,
    String lastName
) implements Principal {

    @Override
    public String getName() {
        return email;
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public String authority() {
        return "ROLE_" + role.name();
    }
}