package com.lionsclub.api.infrastructure.persistence;

import com.lionsclub.api.domain.forum.ForumThread;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumThreadRepository extends JpaRepository<ForumThread, UUID> {

    List<ForumThread> findByCategoryIdOrderByLastActivityDesc(UUID categoryId);

    long countByCategoryId(UUID categoryId);
}
