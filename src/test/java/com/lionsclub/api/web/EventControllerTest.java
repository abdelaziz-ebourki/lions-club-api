package com.lionsclub.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionsclub.api.TestcontainersConfiguration;
import com.lionsclub.api.domain.event.Event;
import com.lionsclub.api.domain.event.EventCategory;
import com.lionsclub.api.domain.event.EventStatus;
import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.EventRepository;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import com.lionsclub.api.security.WithMockUserPrincipal;
import com.lionsclub.api.service.EventService;
import com.lionsclub.api.web.dto.EventRequest;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Sql(statements = {"DELETE FROM events", "DELETE FROM users"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@ImportTestcontainers(TestcontainersConfiguration.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventService eventService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User memberUser;
    private Event existingEvent;

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

        var start = LocalDateTime.now().plusDays(30);
        var end = start.plusHours(8);
        var event = new Event();
        event.setTitle("Test Event");
        event.setDescription("A test event description for testing");
        event.setStartDateTime(start);
        event.setEndDateTime(end);
        event.setLocation("Test Location");
        event.setCategory(EventCategory.COMMUNITY);
        event.setStatus(EventStatus.PUBLISHED);
        event.setCreatedBy(adminUser);
        existingEvent = eventRepository.save(event);
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
    void shouldListAllEvents() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isString())
                .andExpect(jsonPath("$[0].title").value("Test Event"))
                .andExpect(jsonPath("$[0].rsvpCount").value(0));
    }

    @Test
    void shouldFilterEventsByStatus() throws Exception {
        mockMvc.perform(get("/api/events?status=upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("upcoming"));
    }

    @Test
    void shouldReturn400ForInvalidStatusFilter() throws Exception {
        mockMvc.perform(get("/api/events?status=invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetSingleEvent() throws Exception {
        mockMvc.perform(get("/api/events/{id}", existingEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.title").value("Test Event"));
    }

    @Test
    void shouldReturn404ForNonExistentEvent() throws Exception {
        mockMvc.perform(get("/api/events/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateEventAsAdmin() throws Exception {
        var cookie = loginAs(adminUser, "adminpass");

        mockMvc.perform(post("/api/events")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "New Event",
                                    "description": "A brand new event for testing purposes",
                                    "date": "2026-10-01",
                                    "time": "14:00",
                                    "location": "New Location",
                                    "category": "Health"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.title").value("New Event"))
                .andExpect(jsonPath("$.status").value("upcoming"));
    }

    @Test
    void shouldReturn401ForUnauthenticatedCreate() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "New Event",
                                    "description": "A brand new event for testing purposes",
                                    "date": "2026-10-01",
                                    "time": "14:00",
                                    "location": "New Location",
                                    "category": "Health"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForNonAdminCreate() throws Exception {
        var cookie = loginAs(memberUser, "memberpass");

        mockMvc.perform(post("/api/events")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "New Event",
                                    "description": "A brand new event for testing purposes",
                                    "date": "2026-10-01",
                                    "time": "14:00",
                                    "location": "New Location",
                                    "category": "Health"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400ForInvalidCategory() throws Exception {
        var cookie = loginAs(adminUser, "adminpass");

        mockMvc.perform(post("/api/events")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "New Event",
                                    "description": "A brand new event for testing purposes",
                                    "date": "2026-10-01",
                                    "time": "14:00",
                                    "location": "Test Location",
                                    "category": "INVALID_CATEGORY"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No enum constant com.lionsclub.api.domain.event.EventCategory.INVALID_CATEGORY"));
    }

    @Test
    void shouldReturn400ForInvalidCreateInput() throws Exception {
        var cookie = loginAs(adminUser, "adminpass");

        mockMvc.perform(post("/api/events")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "AB",
                                    "description": "short",
                                    "date": "",
                                    "time": "",
                                    "location": "",
                                    "category": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateEventAsAdmin() throws Exception {
        var cookie = loginAs(adminUser, "adminpass");

        mockMvc.perform(put("/api/events/{id}", existingEvent.getId())
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Event Title",
                                    "description": "This event has been updated with new details",
                                    "date": "2026-11-01",
                                    "time": "09:00",
                                    "location": "Updated Location",
                                    "category": "Environment"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Event Title"))
                .andExpect(jsonPath("$.category").value("ENVIRONMENT"));
    }

    @Test
    void shouldReturn404ForUpdateOfNonExistentEvent() throws Exception {
        var cookie = loginAs(adminUser, "adminpass");

        mockMvc.perform(put("/api/events/{id}", "00000000-0000-0000-0000-000000000000")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated Event Title",
                                    "description": "This event has been updated with new details",
                                    "date": "2026-11-01",
                                    "time": "09:00",
                                    "location": "Updated Location",
                                    "category": "Environment"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401ForUnauthenticatedUpdate() throws Exception {
        mockMvc.perform(put("/api/events/{id}", existingEvent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated",
                                    "description": "This event has been updated with new details",
                                    "date": "2026-11-01",
                                    "time": "09:00",
                                    "location": "Updated Location",
                                    "category": "Environment"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForNonAdminUpdate() throws Exception {
        var cookie = loginAs(memberUser, "memberpass");

        mockMvc.perform(put("/api/events/{id}", existingEvent.getId())
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Updated",
                                    "description": "This event has been updated with new details",
                                    "date": "2026-11-01",
                                    "time": "09:00",
                                    "location": "Updated Location",
                                    "category": "Environment"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteEventAsAdmin() throws Exception {
        var cookie = loginAs(adminUser, "adminpass");

        mockMvc.perform(delete("/api/events/{id}", existingEvent.getId())
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldReturn404ForDeleteOfNonExistentEvent() throws Exception {
        var cookie = loginAs(adminUser, "adminpass");

        mockMvc.perform(delete("/api/events/{id}", "00000000-0000-0000-0000-000000000000")
                        .cookie(cookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403ForNonAdminDelete() throws Exception {
        var cookie = loginAs(memberUser, "memberpass");

        mockMvc.perform(delete("/api/events/{id}", existingEvent.getId())
                        .cookie(cookie))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUserPrincipal(userId = "00000000-0000-0000-0000-000000000002",
            email = "member@test.com", role = "MEMBER",
            firstName = "Member", lastName = "User")
    void shouldAllowUpdateWhenUserIdMatchesPrincipal() {
        var request = new EventRequest(
                "Updated Title",
                "Updated Description",
                "2026-11-01",
                "09:00",
                "New Location",
                "Health",
                null
        );
        var result = eventService.updateEvent(existingEvent.getId(), request,
                UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Updated Title");
    }

    @Test
    @WithMockUserPrincipal(userId = "00000000-0000-0000-0000-000000000002",
            email = "member@test.com", role = "MEMBER",
            firstName = "Member", lastName = "User")
    void shouldDenyUpdateWhenUserIdDiffersFromPrincipal() {
        var request = new EventRequest(
                "Updated Title",
                "Updated Description",
                "2026-11-01",
                "09:00",
                "New Location",
                "Health",
                null
        );
        var differentUserId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        assertThatThrownBy(() ->
                eventService.updateEvent(existingEvent.getId(), request, differentUserId))
                .isInstanceOf(AccessDeniedException.class);
    }
}
