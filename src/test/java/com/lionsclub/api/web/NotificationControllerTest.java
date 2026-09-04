package com.lionsclub.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lionsclub.api.TestcontainersConfiguration;
import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Sql(statements = {"DELETE FROM events", "DELETE FROM notifications", "DELETE FROM contact_messages",
        "DELETE FROM gallery_items", "DELETE FROM news", "DELETE FROM members", "DELETE FROM users"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@ImportTestcontainers(TestcontainersConfiguration.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminCookie;
    private Cookie memberCookie;

    @BeforeEach
    void setUp() throws Exception {
        createUser("admin@test.com", Role.ADMIN);
        createUser("member@test.com", Role.MEMBER);
        adminCookie = login("admin@test.com");
        memberCookie = login("member@test.com");
    }

    private void createUser(String email, Role role) {
        var user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
    }

    private Cookie login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "password123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("auth_token");
    }

    @Test
    void inbox_shouldRequireAuthAndStartEmpty() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/notifications").cookie(memberCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.notifications.length()").value(0))
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void contactSubmit_shouldNotifyAdminsOnly() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Visitor", "email": "visitor@test.com", "subject": "Hello there", "message": "I would like to know more about you."}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/notifications").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications[0].type").value("admin_announcement"))
                .andExpect(jsonPath("$.notifications[0].targetUrl").value("/admin/messages"))
                .andExpect(jsonPath("$.notifications[0].read").value(false));

        mockMvc.perform(get("/api/notifications").cookie(memberCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void publishedNews_shouldNotifyAllMembers() throws Exception {
        mockMvc.perform(multipart("/api/news")
                        .param("title", "Big Announcement")
                        .param("content", "<p>Read all about it</p>")
                        .param("category", "Announcement")
                        .param("status", "published")
                        .cookie(adminCookie))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/notifications").cookie(memberCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications[0].targetUrl").value("/news/big-announcement"));
    }

    @Test
    void markReadFlows_shouldUpdateUnreadCount() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Visitor", "email": "visitor@test.com", "subject": "Hello there", "message": "I would like to know more about you."}
                                """))
                .andExpect(status().isCreated());

        var inbox = mockMvc.perform(get("/api/notifications").cookie(adminCookie))
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andReturn();
        String id = JsonPath.read(inbox.getResponse().getContentAsString(), "$.notifications[0].id");

        mockMvc.perform(put("/api/notifications/" + id + "/read").cookie(memberCookie))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/notifications/" + id + "/read").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/notifications").cookie(adminCookie))
                .andExpect(jsonPath("$.unreadCount").value(0));

        mockMvc.perform(put("/api/notifications/" + UUID.randomUUID() + "/read").cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAllRead_shouldClearUnreadCount() throws Exception {
        for (int i = 1; i <= 2; i++) {
            mockMvc.perform(post("/api/contact")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Visitor", "email": "visitor@test.com", "subject": "Hello %d", "message": "I would like to know more about you."}
                                    """.formatted(i)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(put("/api/notifications/read-all").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/notifications").cookie(adminCookie))
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.notifications.length()").value(2));
    }
}
