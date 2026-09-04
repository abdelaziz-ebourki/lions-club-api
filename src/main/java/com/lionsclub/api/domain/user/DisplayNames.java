package com.lionsclub.api.domain.user;

import java.util.Locale;

public final class DisplayNames {

    private DisplayNames() {
    }

    public static String displayName(String firstName, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    public static String firstNameOf(String name) {
        String[] parts = name.strip().split("\\s+", 2);
        return parts[0];
    }

    public static String lastNameOf(String name) {
        String[] parts = name.strip().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : parts[0];
    }

    public static String apiRole(Role role) {
        return role.name().toLowerCase(Locale.ROOT);
    }
}
