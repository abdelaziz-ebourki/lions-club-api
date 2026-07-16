package com.lionsclub.api.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionsclub.api.TestcontainersConfiguration;
import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.event.EventCategory;
import com.lionsclub.api.domain.event.EventStatus;
import com.lionsclub.api.domain.rsvp.Rsvp;
import com.lionsclub.api.domain.rsvp.RsvpStatus;
import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("dev")
@Sql(statements = {"DELETE FROM rsvps", "DELETE FROM events", "DELETE FROM users"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@ImportTestcontainers(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RsvpRepositoryTest {

    @Autowired
    private RsvpRepository rsvpRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private User member;
    private Event event;

    @BeforeEach
    void setUp() {
        member = new User();
        member.setEmail("member@lionsclub.org");
        member.setPasswordHash("hashed-password");
        member.setFirstName("Test");
        member.setLastName("Member");
        member.setRole(Role.MEMBER);
        member.setEnabled(true);
        member = userRepository.save(member);

        event = new Event();
        event.setTitle("RSVP Test Event");
        event.setStartDateTime(LocalDateTime.of(2026, 9, 1, 10, 0));
        event.setEndDateTime(LocalDateTime.of(2026, 9, 1, 18, 0));
        event.setStatus(EventStatus.PUBLISHED);
        event.setCategory(EventCategory.COMMUNITY);
        event.setCreatedBy(member);
        event = eventRepository.save(event);
    }

    @Test
    void contextLoads() {
        assertThat(rsvpRepository).isNotNull();
    }

    @Test
    void shouldPersistAndRetrieveRsvp() {
        Rsvp rsvp = new Rsvp();
        rsvp.setEvent(event);
        rsvp.setMember(member);
        rsvp.setStatus(RsvpStatus.YES);
        rsvp.setPlusOne(2);
        rsvp.setNotes("Bringing friends");

        Rsvp saved = rsvpRepository.save(rsvp);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(RsvpStatus.YES);
        assertThat(saved.getPlusOne()).isEqualTo(2);
        assertThat(saved.getNotes()).isEqualTo("Bringing friends");
    }

    @Test
    void shouldEnforceUniqueEventMemberConstraint() {
        Rsvp rsvp1 = new Rsvp();
        rsvp1.setEvent(event);
        rsvp1.setMember(member);
        rsvp1.setStatus(RsvpStatus.YES);
        rsvpRepository.save(rsvp1);

        Rsvp rsvp2 = new Rsvp();
        rsvp2.setEvent(event);
        rsvp2.setMember(member);
        rsvp2.setStatus(RsvpStatus.MAYBE);

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> rsvpRepository.save(rsvp2));
    }

    @Test
    void shouldFindByEventIdAndMemberId() {
        Rsvp rsvp = new Rsvp();
        rsvp.setEvent(event);
        rsvp.setMember(member);
        rsvp.setStatus(RsvpStatus.NO);
        rsvpRepository.save(rsvp);

        var found = rsvpRepository.findByEventIdAndMemberId(event.getId(), member.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(RsvpStatus.NO);
    }

    @Test
    void shouldCountByEventAndStatus() {
        Rsvp rsvp1 = new Rsvp();
        rsvp1.setEvent(event);
        rsvp1.setMember(member);
        rsvp1.setStatus(RsvpStatus.YES);
        rsvpRepository.save(rsvp1);

        User otherMember = new User();
        otherMember.setEmail("other@lionsclub.org");
        otherMember.setPasswordHash("hashed");
        otherMember.setFirstName("Other");
        otherMember.setLastName("Member");
        otherMember.setRole(Role.MEMBER);
        otherMember.setEnabled(true);
        otherMember = userRepository.save(otherMember);

        Rsvp rsvp2 = new Rsvp();
        rsvp2.setEvent(event);
        rsvp2.setMember(otherMember);
        rsvp2.setStatus(RsvpStatus.YES);
        rsvpRepository.save(rsvp2);

        User maybeMember = new User();
        maybeMember.setEmail("maybe@lionsclub.org");
        maybeMember.setPasswordHash("hashed");
        maybeMember.setFirstName("Maybe");
        maybeMember.setLastName("Member");
        maybeMember.setRole(Role.MEMBER);
        maybeMember.setEnabled(true);
        maybeMember = userRepository.save(maybeMember);

        Rsvp rsvp3 = new Rsvp();
        rsvp3.setEvent(event);
        rsvp3.setMember(maybeMember);
        rsvp3.setStatus(RsvpStatus.MAYBE);
        rsvpRepository.save(rsvp3);

        long yesCount = rsvpRepository.countByEventIdAndStatus(event.getId(), RsvpStatus.YES);
        assertThat(yesCount).isEqualTo(2);

        long maybeCount = rsvpRepository.countByEventIdAndStatus(event.getId(), RsvpStatus.MAYBE);
        assertThat(maybeCount).isEqualTo(1);
    }

    @Test
    void shouldFindByEventIdWithMember() {
        Rsvp rsvp = new Rsvp();
        rsvp.setEvent(event);
        rsvp.setMember(member);
        rsvp.setStatus(RsvpStatus.YES);
        rsvpRepository.save(rsvp);

        List<Rsvp> rsvps = rsvpRepository.findByEventId(event.getId());
        assertThat(rsvps).hasSize(1);
        assertThat(rsvps.get(0).getMember().getEmail()).isEqualTo("member@lionsclub.org");
    }
}
