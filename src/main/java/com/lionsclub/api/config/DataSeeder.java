package com.lionsclub.api.config;

import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.event.EventCategory;
import com.lionsclub.api.domain.event.EventStatus;
import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.EventRepository;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, EventRepository eventRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains data — skipping seed");
            return;
        }

        log.info("Seeding database with dev data...");

        var admin = createUser("admin-1", "Ahmed", "Benali",
                "admin@lionsclub.com", "admin123", Role.ADMIN);
        createUser("user-1", "Fatima", "El Amrani",
                "fatima@lionsclub.com", "member123", Role.MEMBER);
        createUser("user-2", "Youssef", "Idrissi",
                "youssef@lionsclub.com", "member123", Role.MEMBER);

        createEvent("Annual Charity Gala 2026",
                "Join us for an elegant evening of dinner, auctions, and entertainment to raise funds for local education initiatives.",
                LocalDateTime.of(2026, 9, 15, 19, 0),
                LocalDateTime.of(2026, 9, 15, 21, 0),
                "Hyatt Regency Casablanca",
                EventCategory.FUNDRAISER, EventStatus.PUBLISHED, admin);

        createEvent("Community Clean-Up Day",
                "A day dedicated to cleaning and beautifying our local parks and public spaces.",
                LocalDateTime.of(2026, 8, 22, 8, 0),
                LocalDateTime.of(2026, 8, 22, 10, 0),
                "Parc de la Ligue Arabe, Casablanca",
                EventCategory.COMMUNITY, EventStatus.PUBLISHED, admin);

        createEvent("Health & Wellness Workshop",
                "Free workshop covering basic health screenings, nutrition advice, and mental wellness resources.",
                LocalDateTime.of(2026, 7, 10, 10, 0),
                LocalDateTime.of(2026, 7, 10, 12, 0),
                "Centre Culturel d'Anfa",
                EventCategory.HEALTH, EventStatus.PUBLISHED, admin);

        createEvent("Sight Screening Camp",
                "A club-organized vision screening camp providing free eye checkups and glasses to those in need.",
                LocalDateTime.of(2026, 5, 20, 9, 0),
                LocalDateTime.of(2026, 5, 20, 11, 0),
                "Sidi Moumen Community Center",
                EventCategory.HEALTH, EventStatus.COMPLETED, admin);

        createEvent("Youth Leadership Summit",
                "A two-day summit empowering young leaders with skills in public speaking and project management.",
                LocalDateTime.of(2026, 4, 5, 9, 0),
                LocalDateTime.of(2026, 4, 5, 11, 0),
                "Université Hassan II",
                EventCategory.YOUTH, EventStatus.COMPLETED, admin);

        log.info("Database seeded with 3 users and 5 events");
    }

    private User createUser(String stringId, String firstName, String lastName,
                            String email, String plainPassword, Role role) {
        var user = new User();
        user.setId(UUID.nameUUIDFromBytes(stringId.getBytes(StandardCharsets.UTF_8)));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private Event createEvent(String title, String description,
                              LocalDateTime startDateTime, LocalDateTime endDateTime,
                              String location, EventCategory category, EventStatus status,
                              User createdBy) {
        var event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setStartDateTime(startDateTime);
        event.setEndDateTime(endDateTime);
        event.setLocation(location);
        event.setCategory(category);
        event.setStatus(status);
        event.setCreatedBy(createdBy);
        return eventRepository.save(event);
    }
}
