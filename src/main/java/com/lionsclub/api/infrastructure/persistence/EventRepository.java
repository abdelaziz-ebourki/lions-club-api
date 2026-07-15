package com.lionsclub.api.infrastructure.persistence;

import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.event.EventStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByStatus(EventStatus status);
}
