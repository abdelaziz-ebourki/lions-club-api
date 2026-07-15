package com.lionsclub.api.infrastructure.persistence;

import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.event.EventStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByStatus(EventStatus status);

    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.startDateTime > :now ORDER BY e.startDateTime ASC")
    List<Event> findUpcomingEvents(@Param("status") EventStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.startDateTime <= :now AND e.endDateTime >= :now ORDER BY e.startDateTime ASC")
    List<Event> findOngoingEvents(@Param("status") EventStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.status = :completedStatus OR (e.status = :publishedStatus AND e.endDateTime < :now) ORDER BY e.startDateTime DESC")
    List<Event> findPastEvents(@Param("completedStatus") EventStatus completedStatus, @Param("publishedStatus") EventStatus publishedStatus, @Param("now") LocalDateTime now);
}
