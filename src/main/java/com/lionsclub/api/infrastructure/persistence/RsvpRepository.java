package com.lionsclub.api.infrastructure.persistence;

import com.lionsclub.api.domain.rsvp.Rsvp;
import com.lionsclub.api.domain.rsvp.RsvpStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RsvpRepository extends JpaRepository<Rsvp, UUID> {

    Optional<Rsvp> findByEventIdAndMemberId(UUID eventId, UUID memberId);

    @EntityGraph(attributePaths = {"member"})
    List<Rsvp> findByEventId(UUID eventId);

    long countByEventIdAndStatus(UUID eventId, RsvpStatus status);
}
