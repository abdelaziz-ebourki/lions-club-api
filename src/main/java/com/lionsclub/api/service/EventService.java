package com.lionsclub.api.service;

import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.event.EventCategory;
import com.lionsclub.api.domain.event.EventStatus;
import com.lionsclub.api.domain.rsvp.RsvpStatus;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.EventRepository;
import com.lionsclub.api.infrastructure.persistence.RsvpRepository;
import com.lionsclub.api.web.dto.EventRequest;
import com.lionsclub.api.web.dto.EventResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int DEFAULT_DURATION_HOURS = 2;
    private static final String STATUS_PAST = "past";

    private final EventRepository eventRepository;
    private final RsvpRepository rsvpRepository;

    public List<EventResponse> listEvents(String statusFilter) {
        var now = LocalDateTime.now();
        List<Event> events;

        if (statusFilter == null || statusFilter.isBlank()) {
            events = eventRepository.findAll();
        } else {
            events = switch (statusFilter.toLowerCase(Locale.ROOT)) {
                case "upcoming" -> eventRepository.findUpcomingEvents(EventStatus.PUBLISHED, now);
                case "ongoing" -> eventRepository.findOngoingEvents(EventStatus.PUBLISHED, now);
                case STATUS_PAST -> eventRepository.findPastEvents(EventStatus.COMPLETED, EventStatus.PUBLISHED, now);
                default -> throw new IllegalArgumentException("Invalid status filter: " + statusFilter);
            };
        }

        return events.stream().map(this::toResponse).toList();
    }

    public EventResponse getEvent(UUID id) {
        return eventRepository.findById(id)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public EventResponse createEvent(User creator, EventRequest request) {
        var startDate = LocalDate.parse(request.date(), DATE_FORMAT);
        var startTime = LocalTime.parse(request.time(), TIME_FORMAT);
        var startDateTime = LocalDateTime.of(startDate, startTime);
        var endDateTime = startDateTime.plusHours(DEFAULT_DURATION_HOURS);

        var event = new Event();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartDateTime(startDateTime);
        event.setEndDateTime(endDateTime);
        event.setLocation(request.location());
        event.setCategory(EventCategory.valueOf(request.category().toUpperCase(Locale.ROOT)));
        event.setStatus(EventStatus.PUBLISHED);
        event.setCreatedBy(creator);

        var saved = eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional
    public EventResponse updateEvent(UUID id, EventRequest request) {
        var event = eventRepository.findById(id);
        if (event.isEmpty()) {
            return null;
        }
        var existing = event.get();

        var startDate = LocalDate.parse(request.date(), DATE_FORMAT);
        var startTime = LocalTime.parse(request.time(), TIME_FORMAT);
        var startDateTime = LocalDateTime.of(startDate, startTime);
        var endDateTime = startDateTime.plusHours(DEFAULT_DURATION_HOURS);

        existing.setTitle(request.title());
        existing.setDescription(request.description());
        existing.setStartDateTime(startDateTime);
        existing.setEndDateTime(endDateTime);
        existing.setLocation(request.location());
        existing.setCategory(EventCategory.valueOf(request.category().toUpperCase(Locale.ROOT)));

        if (request.status() != null && !request.status().isBlank()) {
            existing.setStatus(mapStatus(request.status()));
        }

        var saved = eventRepository.save(existing);
        return toResponse(saved);
    }

    @Transactional
    public boolean deleteEvent(UUID id) {
        if (!eventRepository.existsById(id)) {
            return false;
        }
        eventRepository.deleteById(id);
        return true;
    }

    private EventResponse toResponse(Event event) {
        var startDateTime = event.getStartDateTime();
        var rsvpCounts = getRsvpCounts(event.getId());
        var rsvpCount = rsvpCounts.values().stream().mapToInt(Long::intValue).sum();
        var rsvpBreakdown = Map.of(
                "yes", rsvpCounts.getOrDefault(RsvpStatus.YES, 0L).intValue(),
                "no", rsvpCounts.getOrDefault(RsvpStatus.NO, 0L).intValue(),
                "maybe", rsvpCounts.getOrDefault(RsvpStatus.MAYBE, 0L).intValue()
        );

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                startDateTime.format(DATE_FORMAT),
                startDateTime.format(TIME_FORMAT),
                event.getLocation(),
                event.getCategory().name(),
                deriveFrontendStatus(event),
                rsvpCount,
                rsvpBreakdown,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private Map<RsvpStatus, Long> getRsvpCounts(UUID eventId) {
        return Map.of(
                RsvpStatus.YES, rsvpRepository.countByEventIdAndStatus(eventId, RsvpStatus.YES),
                RsvpStatus.NO, rsvpRepository.countByEventIdAndStatus(eventId, RsvpStatus.NO),
                RsvpStatus.MAYBE, rsvpRepository.countByEventIdAndStatus(eventId, RsvpStatus.MAYBE)
        );
    }

    private String deriveFrontendStatus(Event event) {
        return switch (event.getStatus()) {
            case PUBLISHED -> {
                var now = LocalDateTime.now();
                if (event.getStartDateTime().isAfter(now)) {
                    yield "upcoming";
                } else if (event.getEndDateTime().isBefore(now)) {
                    yield STATUS_PAST;
                } else {
                    yield "ongoing";
                }
            }
            case COMPLETED -> STATUS_PAST;
            case CANCELLED -> "cancelled";
            case DRAFT -> "draft";
        };
    }

    private EventStatus mapStatus(String frontendStatus) {
        return switch (frontendStatus.toLowerCase(Locale.ROOT)) {
            case "upcoming", "ongoing" -> EventStatus.PUBLISHED;
            case STATUS_PAST -> EventStatus.COMPLETED;
            case "cancelled" -> EventStatus.CANCELLED;
            case "draft" -> EventStatus.DRAFT;
            default -> throw new IllegalArgumentException("Invalid status: " + frontendStatus);
        };
    }
}