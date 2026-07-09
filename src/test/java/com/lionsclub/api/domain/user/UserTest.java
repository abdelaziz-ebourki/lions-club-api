package com.lionsclub.api.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void shouldHaveAllExpectedFields() {
        Set<String> expectedFields = Set.of(
                "id", "email", "passwordHash", "firstName", "lastName",
                "role", "enabled", "createdAt", "updatedAt"
        );
        Set<String> actualFields = Stream.of(User.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertThat(actualFields).containsAll(expectedFields);
    }

    @Test
    void shouldHaveValidFieldTypes() throws NoSuchFieldException {
        assertThat(User.class.getDeclaredField("id").getType()).isEqualTo(UUID.class);
        assertThat(User.class.getDeclaredField("email").getType()).isEqualTo(String.class);
        assertThat(User.class.getDeclaredField("passwordHash").getType()).isEqualTo(String.class);
        assertThat(User.class.getDeclaredField("firstName").getType()).isEqualTo(String.class);
        assertThat(User.class.getDeclaredField("lastName").getType()).isEqualTo(String.class);
        assertThat(User.class.getDeclaredField("role").getType()).isEqualTo(Role.class);
        assertThat(User.class.getDeclaredField("enabled").getType()).isEqualTo(boolean.class);
        assertThat(User.class.getDeclaredField("createdAt").getType()).isEqualTo(LocalDateTime.class);
        assertThat(User.class.getDeclaredField("updatedAt").getType()).isEqualTo(LocalDateTime.class);
    }
}