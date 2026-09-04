package com.lionsclub.api.infrastructure.persistence;

import com.lionsclub.api.domain.forum.ForumReply;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumReplyRepository extends JpaRepository<ForumReply, UUID> {

    List<ForumReply> findByThreadIdOrderByCreatedAtAsc(UUID threadId);

    long countByThreadId(UUID threadId);
}
