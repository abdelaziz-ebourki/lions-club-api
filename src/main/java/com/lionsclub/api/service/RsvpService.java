package com.lionsclub.api.service;

import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.event.EventStatus;
import com.lionsclub.api.domain.rsvp.Rsvp;
import com.lionsclub.api.domain.rsvp.RsvpStatus;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.EventRepository;
import com.lionsclub.api.infrastructure.persistence.RsvpRepository;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import com.lionsclub.api.web.dto.RsvpRequest;
import com.lionsclub.api.web.dto.RsvpResponse;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RsvpService {

    private final RsvpRepository rsvpRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
    @PreAuthorize("principal.userId == #memberId or principal.role == T(com.lionsclub.api.domain.user.Role).ADMIN")
    public RsvpResponse createOrUpdateRsvp(UUID eventId, UUID memberId, RsvpRequest request) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot RSVP to a cancelled event");
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot RSVP to a completed event");
        }

        var rsvpStatus = parseStatus(request.status());

        var existingRsvp = rsvpRepository.findByEventIdAndMemberId(eventId, memberId);

        if (rsvpStatus == RsvpStatus.YES && event.getMaxAttendees() != null) {
            boolean isAlreadyYes = existingRsvp.isPresent() && existingRsvp.get().getStatus() == RsvpStatus.YES;
            if (!isAlreadyYes) {
                long currentYesCount = rsvpRepository.countByEventIdAndStatus(eventId, RsvpStatus.YES);
                if (currentYesCount >= event.getMaxAttendees()) {
                    throw new IllegalStateException("Event is at full capacity");
                }
            }
        }
        Rsvp rsvp;

        if (existingRsvp.isPresent()) {
            rsvp = existingRsvp.get();
        } else {
            var member = userRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("Member not found"));
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

    @PreAuthorize("principal.role == T(com.lionsclub.api.domain.user.Role).ADMIN")
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
            return RsvpStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid RSVP status. Must be YES, NO, or MAYBE", e);
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