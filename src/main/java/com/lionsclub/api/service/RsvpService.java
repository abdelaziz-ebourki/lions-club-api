package com.lionsclub.api.service;

import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.event.EventStatus;
import com.lionsclub.api.domain.rsvp.Rsvp;
import com.lionsclub.api.domain.rsvp.RsvpStatus;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.EventRepository;
import com.lionsclub.api.infrastructure.persistence.RsvpRepository;
import com.lionsclub.api.web.dto.RsvpRequest;
import com.lionsclub.api.web.dto.RsvpResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RsvpService {

    private final RsvpRepository rsvpRepository;
    private final EventRepository eventRepository;

    @Transactional
    public RsvpResponse createOrUpdateRsvp(UUID eventId, User member, RsvpRequest request) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot RSVP to a cancelled event");
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot RSVP to a completed event");
        }

        var rsvpStatus = parseStatus(request.status());

        if (rsvpStatus == RsvpStatus.YES && event.getMaxAttendees() != null) {
            long currentYesCount = rsvpRepository.countByEventIdAndStatus(eventId, RsvpStatus.YES);
            if (currentYesCount >= event.getMaxAttendees()) {
                throw new IllegalStateException("Event is at full capacity");
            }
        }

        var existingRsvp = rsvpRepository.findByEventIdAndMemberId(eventId, member.getId());
        Rsvp rsvp;

        if (existingRsvp.isPresent()) {
            rsvp = existingRsvp.get();
        } else {
            rsvp = new Rsvp();
            rsvp.setEvent(event);
            rsvp.setMember(member);
        }

        rsvp.setStatus(rsvpStatus);
        rsvp.setPlusOne(request.plusOne());
        rsvp.setNotes(request.notes());

        var saved = rsvpRepository.save(rsvp);
        return toResponse(saved);
    }

    public List<RsvpResponse> getRsvpsForEvent(UUID eventId) {
        return rsvpRepository.findByEventId(eventId).stream()
                .map(this::toResponseWithMember)
                .toList();
    }

    public long countByEventAndStatus(UUID eventId, RsvpStatus status) {
        return rsvpRepository.countByEventIdAndStatus(eventId, status);
    }

    private RsvpStatus parseStatus(String status) {
        try {
            return RsvpStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid RSVP status. Must be YES, NO, or MAYBE");
        }
    }

    private RsvpResponse toResponse(Rsvp rsvp) {
        return new RsvpResponse(
                rsvp.getId(),
                rsvp.getEvent().getId(),
                rsvp.getMember().getId(),
                rsvp.getStatus().name(),
                rsvp.getPlusOne(),
                rsvp.getNotes(),
                rsvp.getCreatedAt(),
                rsvp.getUpdatedAt(),
                null
        );
    }

    private RsvpResponse toResponseWithMember(Rsvp rsvp) {
        return new RsvpResponse(
                rsvp.getId(),
                rsvp.getEvent().getId(),
                rsvp.getMember().getId(),
                rsvp.getStatus().name(),
                rsvp.getPlusOne(),
                rsvp.getNotes(),
                rsvp.getCreatedAt(),
                rsvp.getUpdatedAt(),
                new RsvpResponse.MemberInfo(
                        rsvp.getMember().getId(),
                        rsvp.getMember().getFirstName(),
                        rsvp.getMember().getLastName(),
                        rsvp.getMember().getEmail()
                )
        );
    }
}