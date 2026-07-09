package com.lionsclub.api.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void shouldHaveExactlyTwoValues() {
        Role[] values = Role.values();
        assertThat(values).hasSize(2);
    }

    @Test
    void shouldContainAdmin() {
        assertThat(Role.valueOf("ADMIN")).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldContainMember() {
        assertThat(Role.valueOf("MEMBER")).isEqualTo(Role.MEMBER);
    }
}