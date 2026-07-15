package com.lionsclub.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionsclub.api.TestcontainersConfiguration;
import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.event.EventCategory;
import com.lionsclub.api.domain.event.EventStatus;
import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.EventRepository;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("dev")
@ImportTestcontainers(TestcontainersConfiguration.class)
@Transactional
class DataSeederTest {

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void seedsDataWhenDatabaseIsEmpty() throws Exception {
        assertThat(dataSeeder).isNotNull();
        dataSeeder.run();

        assertThat(userRepository.count()).isEqualTo(3);
        assertThat(eventRepository.count()).isEqualTo(5);
    }

    @Test
    void skipsSeedingWhenDatabaseHasData() throws Exception {
        assertThat(dataSeeder).isNotNull();
        var existing = new User();
        existing.setEmail("manual@test.com");
        existing.setPasswordHash(passwordEncoder.encode("manual"));
        existing.setFirstName("Manual");
        existing.setLastName("User");
        existing.setRole(Role.MEMBER);
        existing.setEnabled(true);
        userRepository.save(existing);

        dataSeeder.run();

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(eventRepository.count()).isEqualTo(0);
    }

    @Test
    void seededUserPasswordsAreBcryptHashed() throws Exception {
        assertThat(dataSeeder).isNotNull();
        dataSeeder.run();

        var admin = userRepository.findByEmail("admin@lionsclub.com").orElseThrow();
        assertThat(passwordEncoder.matches("admin123", admin.getPasswordHash())).isTrue();

        var member = userRepository.findByEmail("fatima@lionsclub.com").orElseThrow();
        assertThat(passwordEncoder.matches("member123", member.getPasswordHash())).isTrue();
    }

    @Test
    void seededEventsHaveCorrectFields() throws Exception {
        assertThat(dataSeeder).isNotNull();
        dataSeeder.run();

        var gala = eventRepository.findAll().stream()
                .filter(e -> e.getTitle().equals("Annual Charity Gala 2026"))
                .findFirst().orElseThrow();

        assertThat(gala.getDescription()).isEqualTo("Join us for an elegant evening of dinner, auctions, and entertainment to raise funds for local education initiatives.");
        assertThat(gala.getLocation()).isEqualTo("Hyatt Regency Casablanca");
        assertThat(gala.getCategory()).isEqualTo(EventCategory.FUNDRAISER);
        assertThat(gala.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(gala.getCreatedBy().getEmail()).isEqualTo("admin@lionsclub.com");

        var camp = eventRepository.findAll().stream()
                .filter(e -> e.getTitle().equals("Sight Screening Camp"))
                .findFirst().orElseThrow();

        assertThat(camp.getCategory()).isEqualTo(EventCategory.HEALTH);
        assertThat(camp.getStatus()).isEqualTo(EventStatus.COMPLETED);
    }

    @Test
    void hasDevProfileAnnotation() {
        var profileAnnotation = DataSeeder.class.getAnnotation(Profile.class);
        assertThat(profileAnnotation).isNotNull();
        assertThat(profileAnnotation.value()).contains("dev");
    }

    @Test
    void usesDeterministicUuids() throws Exception {
        assertThat(dataSeeder).isNotNull();
        dataSeeder.run();

        var adminId = userRepository.findByEmail("admin@lionsclub.com").orElseThrow().getId();
        var expectedAdminId = UUID.nameUUIDFromBytes("admin-1".getBytes(StandardCharsets.UTF_8));

        assertThat(adminId).isEqualTo(expectedAdminId);
    }
}
