package com.lionsclub.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionsclub.api.domain.user.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserPrincipalTest {

    @Test
    void shouldCreateUserPrincipalWithAllFields() {
        UUID userId = UUID.randomUUID();
        String email = "john.doe@lionsclub.org";
        Role role = Role.MEMBER;
        String firstName = "John";
        String lastName = "Doe";

        UserPrincipal principal = new UserPrincipal(userId, email, role, firstName, lastName);

        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo(email);
        assertThat(principal.role()).isEqualTo(role);
        assertThat(principal.firstName()).isEqualTo(firstName);
        assertThat(principal.lastName()).isEqualTo(lastName);
    }

    @Test
    void shouldReturnEmailAsPrincipalName() {
        UUID userId = UUID.randomUUID();
        String email = "john.doe@lionsclub.org";

        UserPrincipal principal = new UserPrincipal(userId, email, Role.ADMIN, "John", "Doe");

        assertThat(principal.getName()).isEqualTo(email);
    }

    @Test
    void shouldReturnFullName() {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "john@example.com", Role.MEMBER, "John", "Doe");

        assertThat(principal.fullName()).isEqualTo("John Doe");
    }

    @Test
    void shouldReturnAuthorityWithRolePrefix() {
        UserPrincipal admin = new UserPrincipal(UUID.randomUUID(), "admin@example.com", Role.ADMIN, "Admin", "User");
        UserPrincipal member = new UserPrincipal(UUID.randomUUID(), "member@example.com", Role.MEMBER, "Member", "User");

        assertThat(admin.authority()).isEqualTo("ROLE_ADMIN");
        assertThat(member.authority()).isEqualTo("ROLE_MEMBER");
    }

    @Test
    void shouldBeEqualWhenAllFieldsMatch() {
        UUID userId = UUID.randomUUID();
        UserPrincipal p1 = new UserPrincipal(userId, "john@example.com", Role.MEMBER, "John", "Doe");
        UserPrincipal p2 = new UserPrincipal(userId, "john@example.com", Role.MEMBER, "John", "Doe");

        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        UUID userId = UUID.randomUUID();
        UserPrincipal p1 = new UserPrincipal(userId, "john@example.com", Role.MEMBER, "John", "Doe");
        UserPrincipal p2 = new UserPrincipal(userId, "jane@example.com", Role.MEMBER, "John", "Doe");

        assertThat(p1).isNotEqualTo(p2);
    }

    @Test
    void shouldHaveMeaningfulToString() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "john@example.com", Role.MEMBER, "John", "Doe");

        String toString = principal.toString();
        assertThat(toString).contains("userId=").contains("email=john@example.com").contains("role=MEMBER").contains("firstName=John").contains("lastName=Doe");
    }
}