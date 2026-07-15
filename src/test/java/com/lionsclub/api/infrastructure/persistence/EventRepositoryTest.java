package com.lionsclub.api.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionsclub.api.TestcontainersConfiguration;
import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.event.EventStatus;
import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import java.time.LocalDateTime;
import java.util.List;
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
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private User eventCreator;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("event-creator@lionsclub.org");
        user.setPasswordHash("hashed-password");
        user.setFirstName("Event");
        user.setLastName("Creator");
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        eventCreator = userRepository.save(user);
    }

    private Event createEvent(String title, EventStatus status) {
        Event event = new Event();
        event.setTitle(title);
        event.setStartDateTime(LocalDateTime.of(2026, 8, 1, 10, 0));
        event.setEndDateTime(LocalDateTime.of(2026, 8, 1, 18, 0));
        event.setStatus(status);
        event.setCreatedBy(eventCreator);
        return event;
    }

    @Test
    void contextLoads() {
        assertThat(eventRepository).isNotNull();
    }

    @Test
    void shouldPersistAndRetrieveStatus() {
        Event event = createEvent("Test Event", EventStatus.PUBLISHED);
        Event saved = eventRepository.save(event);

        Event found = eventRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void shouldAutoPopulateTimestamps() {
        Event event = createEvent("Timestamps Test", EventStatus.DRAFT);
        Event saved = eventRepository.save(event);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindEventsByStatus() {
        eventRepository.save(createEvent("Draft Event", EventStatus.DRAFT));
        eventRepository.save(createEvent("Published Event", EventStatus.PUBLISHED));
        eventRepository.save(createEvent("Cancelled Event", EventStatus.CANCELLED));

        List<Event> published = eventRepository.findByStatus(EventStatus.PUBLISHED);
        assertThat(published).hasSize(1);
        assertThat(published.get(0).getTitle()).isEqualTo("Published Event");

        List<Event> drafts = eventRepository.findByStatus(EventStatus.DRAFT);
        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getTitle()).isEqualTo("Draft Event");
    }
}
