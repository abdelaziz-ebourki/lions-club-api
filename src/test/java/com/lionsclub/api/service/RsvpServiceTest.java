package com.lionsclub.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.event.EventStatus;
import com.lionsclub.api.domain.rsvp.Rsvp;
import com.lionsclub.api.domain.rsvp.RsvpStatus;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.EventRepository;
import com.lionsclub.api.infrastructure.persistence.RsvpRepository;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import com.lionsclub.api.web.dto.RsvpRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RsvpServiceTest {

    @Mock
    private RsvpRepository rsvpRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    private RsvpService rsvpService;
    private User member;
    private Event publishedEvent;
    private Event cancelledEvent;
    private Event completedEvent;

    @BeforeEach
    void setUp() {
        rsvpService = new RsvpService(rsvpRepository, eventRepository, userRepository);

        member = new User();
        member.setId(UUID.randomUUID());

        publishedEvent = new Event();
        publishedEvent.setId(UUID.randomUUID());
        publishedEvent.setStatus(EventStatus.PUBLISHED);
        publishedEvent.setMaxAttendees(10);

        cancelledEvent = new Event();
        cancelledEvent.setId(UUID.randomUUID());
        cancelledEvent.setStatus(EventStatus.CANCELLED);

        completedEvent = new Event();
        completedEvent.setId(UUID.randomUUID());
        completedEvent.setStatus(EventStatus.COMPLETED);
    }

    @Test
    void shouldCreateRsvpWhenNoneExists() {
        when(eventRepository.findById(publishedEvent.getId())).thenReturn(Optional.of(publishedEvent));
        when(rsvpRepository.findByEventIdAndMemberId(publishedEvent.getId(), member.getId())).thenReturn(Optional.empty());
        when(rsvpRepository.countByEventIdAndStatus(publishedEvent.getId(), RsvpStatus.YES)).thenReturn(0L);
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));

        Rsvp savedRsvp = new Rsvp();
        savedRsvp.setId(UUID.randomUUID());
        savedRsvp.setEvent(publishedEvent);
        savedRsvp.setMember(member);
        savedRsvp.setStatus(RsvpStatus.YES);
        savedRsvp.setPlusOne(2);
        savedRsvp.setNotes("Looking forward!");
        when(rsvpRepository.save(any())).thenReturn(savedRsvp);

        RsvpRequest request = new RsvpRequest("YES", 2, "Looking forward!");
        var result = rsvpService.createOrUpdateRsvp(publishedEvent.getId(), member.getId(), request);

        assertThat(result.id()).isNotNull();
        assertThat(result.status()).isEqualTo("YES");
        assertThat(result.plusOne()).isEqualTo(2);
        assertThat(result.notes()).isEqualTo("Looking forward!");
    }

    @Test
    void shouldUpdateExistingRsvpOnUpsert() {
        when(eventRepository.findById(publishedEvent.getId())).thenReturn(Optional.of(publishedEvent));

        Rsvp existing = new Rsvp();
        existing.setId(UUID.randomUUID());
        existing.setEvent(publishedEvent);
        existing.setMember(member);
        existing.setStatus(RsvpStatus.YES);
        when(rsvpRepository.findByEventIdAndMemberId(publishedEvent.getId(), member.getId())).thenReturn(Optional.of(existing));

        // When saving the updated existing rsvp, return it
        when(rsvpRepository.save(any(Rsvp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RsvpRequest request = new RsvpRequest("NO", 0, null);
        var result = rsvpService.createOrUpdateRsvp(publishedEvent.getId(), member.getId(), request);

        assertThat(result.status()).isEqualTo("NO");
        assertThat(result.plusOne()).isZero();
    }

    @Test
    void shouldRejectRsvpForCancelledEvent() {
        when(eventRepository.findById(cancelledEvent.getId())).thenReturn(Optional.of(cancelledEvent));

        RsvpRequest request = new RsvpRequest("YES", 0, null);
        assertThatThrownBy(() -> rsvpService.createOrUpdateRsvp(cancelledEvent.getId(), member.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void shouldRejectRsvpForCompletedEvent() {
        when(eventRepository.findById(completedEvent.getId())).thenReturn(Optional.of(completedEvent));

        RsvpRequest request = new RsvpRequest("YES", 0, null);
        assertThatThrownBy(() -> rsvpService.createOrUpdateRsvp(completedEvent.getId(), member.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void shouldRejectRsvpWhenEventAtCapacity() {
        when(eventRepository.findById(publishedEvent.getId())).thenReturn(Optional.of(publishedEvent));
        when(rsvpRepository.countByEventIdAndStatus(publishedEvent.getId(), RsvpStatus.YES)).thenReturn(10L);

        RsvpRequest request = new RsvpRequest("YES", 0, null);
        assertThatThrownBy(() -> rsvpService.createOrUpdateRsvp(publishedEvent.getId(), member.getId(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("full");
    }

    @Test
    void shouldThrowExceptionForNonExistentEvent() {
        UUID fakeId = UUID.randomUUID();
        when(eventRepository.findById(fakeId)).thenReturn(Optional.empty());

        RsvpRequest request = new RsvpRequest("YES", 0, null);
        assertThatThrownBy(() -> rsvpService.createOrUpdateRsvp(fakeId, member.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldThrowExceptionForInvalidStatus() {
        RsvpRequest request = new RsvpRequest("INVALID", 0, null);
        assertThatThrownBy(() -> rsvpService.createOrUpdateRsvp(publishedEvent.getId(), member.getId(), request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}