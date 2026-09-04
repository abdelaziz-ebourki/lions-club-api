package com.lionsclub.api.infrastructure.persistence;

import com.lionsclub.api.domain.contact.ContactMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<ContactMessage, UUID> {

    List<ContactMessage> findAllByOrderByCreatedAtDesc();
}
