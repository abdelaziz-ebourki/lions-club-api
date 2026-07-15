package com.lionsclub.api.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionsclub.api.TestcontainersConfiguration;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("dev")
@Sql(statements = {"DELETE FROM events", "DELETE FROM users"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@ImportTestcontainers(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("admin@lionsclub.org");
        user.setPasswordHash("hashed-password");
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        savedUser = userRepository.save(user);
    }

    @Test
    void contextLoads() {
        assertThat(userRepository).isNotNull();
    }

    @Test
    void shouldPersistAndRetrieveRole() {
        Optional<User> found = userRepository.findByEmail("admin@lionsclub.org");
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldAutoPopulateTimestamps() {
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldReturnEmptyForNonExistentEmail() {
        Optional<User> found = userRepository.findByEmail("nonexistent@test.com");
        assertThat(found).isEmpty();
    }
}