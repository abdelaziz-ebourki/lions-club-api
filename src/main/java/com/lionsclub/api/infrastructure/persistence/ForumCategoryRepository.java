package com.lionsclub.api.infrastructure.persistence;

import com.lionsclub.api.domain.forum.ForumCategory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumCategoryRepository extends JpaRepository<ForumCategory, UUID> {
}
