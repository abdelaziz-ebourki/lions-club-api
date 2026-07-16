package com.lionsclub.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.event.EventCategory;
import com.lionsclub.api.domain.event.EventStatus;
import com.lionsclub.api.domain.rsvp.RsvpStatus;
import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.EventRepository;
import com.lionsclub.api.infrastructure.persistence.RsvpRepository;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import com.lionsclub.api.web.dto.EventRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RsvpRepository rsvpRepository;

    @Mock
    private UserRepository userRepository;

    private EventService eventService;
    private Event upcomingEvent;
    private Event ongoingEvent;
    private Event pastPublishedEvent;
    private Event completedEvent;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, rsvpRepository, userRepository);

        var now = LocalDateTime.now();

        upcomingEvent = createEvent("Upcoming Event", EventStatus.PUBLISHED, now.plusDays(10), now.plusDays(10).plusHours(2), EventCategory.COMMUNITY);
        ongoingEvent = createEvent("Ongoing Event", EventStatus.PUBLISHED, now.minusHours(1), now.plusHours(1), EventCategory.HEALTH);
        pastPublishedEvent = createEvent("Past Published", EventStatus.PUBLISHED, now.minusDays(5), now.minusDays(5).plusHours(2), EventCategory.ENVIRONMENT);
        completedEvent = createEvent("Completed Event", EventStatus.COMPLETED, now.minusDays(20), now.minusDays(20).plusHours(2), EventCategory.FUNDRAISER);
    }

    private Event createEvent(String title, EventStatus status, LocalDateTime start, LocalDateTime end, EventCategory category) {
        var event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle(title);
        event.setStatus(status);
        event.setStartDateTime(start);
        event.setEndDateTime(end);
        event.setCategory(category);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        return event;
    }

    @Test
    void shouldReturnUpcomingStatusForPublishedEventInFuture() {
        when(eventRepository.findUpcomingEvents(eq(EventStatus.PUBLISHED), any(LocalDateTime.class)))
                .thenReturn(List.of(upcomingEvent));
        when(rsvpRepository.countByEventIdAndStatus(eq(upcomingEvent.getId()), eq(RsvpStatus.YES))).thenReturn(0L);
        when(rsvpRepository.countByEventIdAndStatus(eq(upcomingEvent.getId()), eq(RsvpStatus.NO))).thenReturn(0L);
        when(rsvpRepository.countByEventIdAndStatus(eq(upcomingEvent.getId()), eq(RsvpStatus.MAYBE))).thenReturn(0L);

        var results = eventService.listEvents("upcoming");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo("upcoming");
        assertThat(results.get(0).rsvpCount()).isZero();
    }

    @Test
    void shouldReturnOngoingStatusForCurrentlyActiveEvent() {
        when(eventRepository.findOngoingEvents(eq(EventStatus.PUBLISHED), any(LocalDateTime.class)))
                .thenReturn(List.of(ongoingEvent));
        when(rsvpRepository.countByEventIdAndStatus(eq(ongoingEvent.getId()), eq(RsvpStatus.YES))).thenReturn(0L);
        when(rsvpRepository.countByEventIdAndStatus(eq(ongoingEvent.getId()), eq(RsvpStatus.NO))).thenReturn(0L);
        when(rsvpRepository.countByEventIdAndStatus(eq(ongoingEvent.getId()), eq(RsvpStatus.MAYBE))).thenReturn(0L);

        var results = eventService.listEvents("ongoing");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo("ongoing");
    }

    @Test
    void shouldReturnPastStatusForPublishedEndedEvent() {
        when(eventRepository.findPastEvents(eq(EventStatus.COMPLETED), eq(EventStatus.PUBLISHED), any(LocalDateTime.class)))
                .thenReturn(List.of(pastPublishedEvent));
        when(rsvpRepository.countByEventIdAndStatus(eq(pastPublishedEvent.getId()), eq(RsvpStatus.YES))).thenReturn(0L);
        when(rsvpRepository.countByEventIdAndStatus(eq(pastPublishedEvent.getId()), eq(RsvpStatus.NO))).thenReturn(0L);
        when(rsvpRepository.countByEventIdAndStatus(eq(pastPublishedEvent.getId()), eq(RsvpStatus.MAYBE))).thenReturn(0L);

        var results = eventService.listEvents("past");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo("past");
    }

    @Test
    void shouldReturnPastStatusForCompletedEvent() {
        when(eventRepository.findPastEvents(eq(EventStatus.COMPLETED), eq(EventStatus.PUBLISHED), any(LocalDateTime.class)))
                .thenReturn(List.of(completedEvent));
        when(rsvpRepository.countByEventIdAndStatus(eq(completedEvent.getId()), eq(RsvpStatus.YES))).thenReturn(0L);
        when(rsvpRepository.countByEventIdAndStatus(eq(completedEvent.getId()), eq(RsvpStatus.NO))).thenReturn(0L);
        when(rsvpRepository.countByEventIdAndStatus(eq(completedEvent.getId()), eq(RsvpStatus.MAYBE))).thenReturn(0L);

        var results = eventService.listEvents("past");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo("past");
    }

    @Test
    void shouldReturnNullForNonExistentEvent() {
        when(eventRepository.findById(any())).thenReturn(Optional.empty());

        var result = eventService.getEvent(UUID.randomUUID());
        assertThat(result).isNull();
    }

    @Test
    void shouldCreateEventWithDefaultTwoHourDuration() {
        var creator = new User();
        creator.setId(UUID.randomUUID());

        var request = new EventRequest(
                "Test Event",
                "A test event description for testing",
                "2026-10-01",
                "14:00",
                "Test Location",
                "Health",
                null
        );

        var savedEvent = new Event();
        savedEvent.setId(UUID.randomUUID());
        savedEvent.setTitle("Test Event");
        savedEvent.setDescription("A test event description for testing");
        savedEvent.setStartDateTime(LocalDateTime.of(2026, 10, 1, 14, 0));
        savedEvent.setEndDateTime(LocalDateTime.of(2026, 10, 1, 16, 0));
        savedEvent.setLocation("Test Location");
        savedEvent.setCategory(EventCategory.HEALTH);
        savedEvent.setStatus(EventStatus.PUBLISHED);
        savedEvent.setCreatedBy(creator);
        savedEvent.setCreatedAt(LocalDateTime.now());
        savedEvent.setUpdatedAt(LocalDateTime.now());

        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(eventRepository.save(any())).thenReturn(savedEvent);
        lenient().when(rsvpRepository.countByEventIdAndStatus(any(), eq(RsvpStatus.YES))).thenReturn(0L);
        lenient().when(rsvpRepository.countByEventIdAndStatus(any(), eq(RsvpStatus.NO))).thenReturn(0L);
        lenient().when(rsvpRepository.countByEventIdAndStatus(any(), eq(RsvpStatus.MAYBE))).thenReturn(0L);

        var result = eventService.createEvent(creator.getId(), request);
        assertThat(result.title()).isEqualTo("Test Event");
        assertThat(result.category()).isEqualTo("HEALTH");
        assertThat(result.status()).isEqualTo("upcoming");
        assertThat(result.rsvpCount()).isZero();
        assertThat(result.rsvpBreakdown()).containsExactlyInAnyOrderEntriesOf(Map.of("yes", 0, "no", 0, "maybe", 0));
    }

    @Test
    void shouldReturnAllEventsWhenNoStatusFilter() {
        when(eventRepository.findAll()).thenReturn(List.of(upcomingEvent, ongoingEvent, pastPublishedEvent));
        when(rsvpRepository.countByEventIdAndStatus(any(), eq(RsvpStatus.YES))).thenReturn(0L);
        when(rsvpRepository.countByEventIdAndStatus(any(), eq(RsvpStatus.NO))).thenReturn(0L);
        when(rsvpRepository.countByEventIdAndStatus(any(), eq(RsvpStatus.MAYBE))).thenReturn(0L);

        var results = eventService.listEvents(null);
        assertThat(results).hasSize(3);
    }

    @Test
    void shouldReturnFalseForDeleteOfNonExistentEvent() {
        when(eventRepository.existsById(any())).thenReturn(false);

        var result = eventService.deleteEvent(UUID.randomUUID(), UUID.randomUUID(), Role.ADMIN);
        assertThat(result).isFalse();
    }
}