package com.lionsclub.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
@Sql(statements = {"DELETE FROM events", "DELETE FROM contact_messages", "DELETE FROM gallery_items",
        "DELETE FROM news", "DELETE FROM members", "DELETE FROM users"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@ImportTestcontainers(TestcontainersConfiguration.class)
class ContactControllerTest {

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
    void submitPublic_shouldReturn201Unread() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Visitor", "email": "visitor@test.com", "subject": "Hello there", "message": "I would like to know more about you."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("unread"))
                .andExpect(jsonPath("$.id").isString());
    }

    @Test
    void submit_withShortMessage_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Visitor", "email": "visitor@test.com", "subject": "Hello there", "message": "short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_shouldRequireAdmin() throws Exception {
        mockMvc.perform(get("/api/contact"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/contact").cookie(memberCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/contact").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updateAndDelete_shouldWorkForAdmin() throws Exception {
        var created = mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Visitor", "email": "visitor@test.com", "subject": "Hello there", "message": "I would like to know more about you."}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(patch("/api/contact/" + id)
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "read"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("read"));

        mockMvc.perform(patch("/api/contact/" + id)
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "bogus"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/contact/" + id).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(patch("/api/contact/" + UUID.randomUUID())
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "read"}
                                """))
                .andExpect(status().isNotFound());
    }
}
