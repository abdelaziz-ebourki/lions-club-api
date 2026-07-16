package com.lionsclub.api.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionsclub.api.infrastructure.persistence.UserRepository;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ControllerBoilerplateTest {

    @Test
    void eventControllerShouldNotHaveGetCurrentUserMethod() {
        var methods = Arrays.stream(EventController.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("getCurrentUser"))
                .toList();
        assertThat(methods).isEmpty();
    }

    @Test
    void rsvpControllerShouldNotHaveGetCurrentUserMethod() {
        var methods = Arrays.stream(RsvpController.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("getCurrentUser"))
                .toList();
        assertThat(methods).isEmpty();
    }

    @Test
    void authControllerShouldNotHaveUserRepositoryField() {
        var fields = Arrays.stream(AuthController.class.getDeclaredFields())
                .filter(f -> f.getType().equals(UserRepository.class))
                .toList();
        assertThat(fields).isEmpty();
    }
}
