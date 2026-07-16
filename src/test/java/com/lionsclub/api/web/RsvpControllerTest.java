package com.lionsclub.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionsclub.api.TestcontainersConfiguration;
import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.event.EventCategory;
import com.lionsclub.api.domain.event.EventStatus;
import com.lionsclub.api.domain.rsvp.Rsvp;
import com.lionsclub.api.domain.rsvp.RsvpStatus;
import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.EventRepository;
import com.lionsclub.api.infrastructure.persistence.RsvpRepository;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import com.lionsclub.api.security.WithMockUserPrincipal;
import com.lionsclub.api.service.RsvpService;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Sql(statements = {"DELETE FROM rsvps", "DELETE FROM events", "DELETE FROM users"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@ImportTestcontainers(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RsvpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RsvpRepository rsvpRepository;

    @Autowired
    private RsvpService rsvpService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User memberUser;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setPasswordHash(passwordEncoder.encode("adminpass"));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(Role.ADMIN);
        adminUser.setEnabled(true);
        adminUser = userRepository.save(adminUser);

        memberUser = new User();
        memberUser.setEmail("member@test.com");
        memberUser.setPasswordHash(passwordEncoder.encode("memberpass"));
        memberUser.setFirstName("Member");
        memberUser.setLastName("User");
        memberUser.setRole(Role.MEMBER);
        memberUser.setEnabled(true);
        memberUser = userRepository.save(memberUser);

        testEvent = new Event();
        testEvent.setTitle("RSVP Test Event");
        testEvent.setDescription("Test event for RSVP");
        testEvent.setStartDateTime(LocalDateTime.now().plusDays(30));
        testEvent.setEndDateTime(LocalDateTime.now().plusDays(30).plusHours(2));
        testEvent.setLocation("Test Location");
        testEvent.setCategory(EventCategory.COMMUNITY);
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent.setCreatedBy(adminUser);
        testEvent.setMaxAttendees(2);
        testEvent = eventRepository.save(testEvent);
    }

    private Cookie loginAs(User user, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(user.getEmail(), password)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("auth_token");
    }

    @Test
    void shouldCreateRsvpAsMember() throws Exception {
        var cookie = loginAs(memberUser, "memberpass");

        mockMvc.perform(post("/api/events/{id}/rsvp", testEvent.getId())
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"YES\",\"plusOne\":1,\"notes\":\"Coming!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("YES"))
                .andExpect(jsonPath("$.eventId").value(testEvent.getId().toString()))
                .andExpect(jsonPath("$.plusOne").value(1))
                .andExpect(jsonPath("$.notes").value("Coming!"));
    }

    @Test
    void shouldUpdateExistingRsvp() throws Exception {
        // First RSVP
        var cookie = loginAs(memberUser, "memberpass");
        mockMvc.perform(post("/api/events/{id}/rsvp", testEvent.getId())
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"YES\"}"))
                .andExpect(status().isCreated());

        // Update RSVP
        mockMvc.perform(post("/api/events/{id}/rsvp", testEvent.getId())
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NO"));
    }

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/events/{id}/rsvp", testEvent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"YES\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400ForInvalidStatus() throws Exception {
        var cookie = loginAs(memberUser, "memberpass");

        mockMvc.perform(post("/api/events/{id}/rsvp", testEvent.getId())
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForCancelledEvent() throws Exception {
        var cancelledEvent = new Event();
        cancelledEvent.setTitle("Cancelled Event");
        cancelledEvent.setStartDateTime(LocalDateTime.now().plusDays(10));
        cancelledEvent.setEndDateTime(LocalDateTime.now().plusDays(10).plusHours(2));
        cancelledEvent.setStatus(EventStatus.CANCELLED);
        cancelledEvent.setCategory(EventCategory.COMMUNITY);
        cancelledEvent.setCreatedBy(adminUser);
        cancelledEvent = eventRepository.save(cancelledEvent);

        var cookie = loginAs(memberUser, "memberpass");

        mockMvc.perform(post("/api/events/{id}/rsvp", cancelledEvent.getId())
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"YES\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot RSVP to a cancelled event"));
    }

    @Test
    void shouldReturn409WhenEventAtCapacity() throws Exception {
        // Fill up the event (maxAttendees = 2)
        var otherMember1 = new User();
        otherMember1.setEmail("other1@test.com");
        otherMember1.setPasswordHash(passwordEncoder.encode("pass"));
        otherMember1.setFirstName("Other1");
        otherMember1.setLastName("User");
        otherMember1.setRole(Role.MEMBER);
        otherMember1.setEnabled(true);
        otherMember1 = userRepository.save(otherMember1);

        var otherMember2 = new User();
        otherMember2.setEmail("other2@test.com");
        otherMember2.setPasswordHash(passwordEncoder.encode("pass"));
        otherMember2.setFirstName("Other2");
        otherMember2.setLastName("User");
        otherMember2.setRole(Role.MEMBER);
        otherMember2.setEnabled(true);
        otherMember2 = userRepository.save(otherMember2);

        var rsvp1 = new Rsvp();
        rsvp1.setEvent(testEvent);
        rsvp1.setMember(otherMember1);
        rsvp1.setStatus(RsvpStatus.YES);
        rsvpRepository.save(rsvp1);

        var rsvp2 = new Rsvp();
        rsvp2.setEvent(testEvent);
        rsvp2.setMember(otherMember2);
        rsvp2.setStatus(RsvpStatus.YES);
        rsvpRepository.save(rsvp2);

        // Now testEvent is at capacity, memberUser should get 409
        var cookie = loginAs(memberUser, "memberpass");

        mockMvc.perform(post("/api/events/{id}/rsvp", testEvent.getId())
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"YES\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Event is at full capacity"));
    }

    @Test
    void shouldReturnRsvpListAsAdmin() throws Exception {
        // Create some RSVPs
        var rsvp1 = new Rsvp();
        rsvp1.setEvent(testEvent);
        rsvp1.setMember(memberUser);
        rsvp1.setStatus(RsvpStatus.YES);
        rsvp1.setPlusOne(1);
        rsvpRepository.save(rsvp1);

        var adminCookie = loginAs(adminUser, "adminpass");

        mockMvc.perform(get("/api/events/{id}/rsvps", testEvent.getId())
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("YES"))
                .andExpect(jsonPath("$[0].plusOne").value(1))
                .andExpect(jsonPath("$[0].member.firstName").value("Member"));
    }

    @Test
    void shouldReturn403ForNonAdminRsvpList() throws Exception {
        var cookie = loginAs(memberUser, "memberpass");

        mockMvc.perform(get("/api/events/{id}/rsvps", testEvent.getId())
                        .cookie(cookie))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUserPrincipal(userId = "00000000-0000-0000-0000-000000000001",
            email = "admin@test.com", role = "ADMIN",
            firstName = "Admin", lastName = "User")
    void shouldAllowAdminToViewRsvpsViaService() {
        var result = rsvpService.getRsvpsForEvent(testEvent.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @WithMockUserPrincipal(userId = "00000000-0000-0000-0000-000000000002",
            email = "member@test.com", role = "MEMBER",
            firstName = "Member", lastName = "User")
    void shouldDenyMemberFromViewingRsvpsViaService() {
        assertThatThrownBy(() -> rsvpService.getRsvpsForEvent(testEvent.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }
}