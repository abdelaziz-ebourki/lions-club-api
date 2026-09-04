package com.lionsclub.api.infrastructure.persistence;

import com.lionsclub.api.domain.member.Member;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    List<Member> findAllByOrderByJoinedAtDesc();
}
