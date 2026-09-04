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
@Sql(statements = {"DELETE FROM events", "DELETE FROM forum_replies", "DELETE FROM forum_threads",
        "DELETE FROM notifications", "DELETE FROM contact_messages", "DELETE FROM gallery_items",
        "DELETE FROM news", "DELETE FROM members", "DELETE FROM users"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@ImportTestcontainers(TestcontainersConfiguration.class)
class ForumControllerTest {

    private static final UUID GENERAL_CATEGORY = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie adminCookie;
    private Cookie memberCookie;
    private Cookie otherCookie;

    @BeforeEach
    void setUp() throws Exception {
        createUser("admin@test.com", Role.ADMIN);
        createUser("member@test.com", Role.MEMBER);
        createUser("other@test.com", Role.MEMBER);
        adminCookie = login("admin@test.com");
        memberCookie = login("member@test.com");
        otherCookie = login("other@test.com");
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

    private String createThread(Cookie cookie, String title) throws Exception {
        var result = mockMvc.perform(post("/api/forum/" + GENERAL_CATEGORY + "/threads")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "content": "This is a sufficiently long thread body."}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    @Test
    void categories_shouldBeSeededAndPublic() throws Exception {
        mockMvc.perform(get("/api/forum/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].threadCount").exists())
                .andExpect(jsonPath("$[0].postCount").exists());
    }

    @Test
    void threadLifecycle_shouldWorkEndToEnd() throws Exception {
        String threadBody = createThread(memberCookie, "Welcome Thread");
        String threadId = JsonPath.read(threadBody, "$.id");

        org.assertj.core.api.Assertions.assertThat(
                ((String) JsonPath.read(threadBody, "$.author"))).isEqualTo("Test User");

        mockMvc.perform(get("/api/forum/" + GENERAL_CATEGORY + "/threads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/forum/" + GENERAL_CATEGORY + "/" + threadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1))
                .andExpect(jsonPath("$.replyCount").value(0));

        var replyResult = mockMvc.perform(post("/api/forum/replies")
                        .cookie(otherCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"threadId": "%s", "content": "Great to be here, welcome everyone!"}
                                """.formatted(threadId)))
                .andExpect(status().isCreated())
                .andReturn();
        String replyId = JsonPath.read(replyResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/forum/replies")
                        .cookie(memberCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"threadId": "%s", "content": "Thanks for joining us here!", "parentReplyId": "%s"}
                                """.formatted(threadId, replyId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentReplyId").value(replyId));

        mockMvc.perform(get("/api/forum/replies?threadId=" + threadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/forum/" + GENERAL_CATEGORY + "/" + threadId))
                .andExpect(jsonPath("$.replyCount").value(2));

        mockMvc.perform(patch("/api/forum/threads/" + threadId)
                        .cookie(memberCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "pinned"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/forum/threads/" + threadId)
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "pinned"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pinned"));

        mockMvc.perform(delete("/api/forum/threads/" + threadId).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/forum/" + GENERAL_CATEGORY + "/" + threadId))
                .andExpect(status().isNotFound());
    }

    @Test
    void reply_shouldNotifyThreadAuthor() throws Exception {
        String threadBody = createThread(memberCookie, "Notify Me Thread");
        String threadId = JsonPath.read(threadBody, "$.id");

        mockMvc.perform(post("/api/forum/replies")
                        .cookie(otherCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"threadId": "%s", "content": "Pinging the author right here!"}
                                """.formatted(threadId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/notifications").cookie(memberCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications[0].type").value("forum_reply"));
    }

    @Test
    void reply_withParentFromAnotherThread_shouldReturn400() throws Exception {
        String threadA = JsonPath.read(createThread(memberCookie, "Thread Alpha One"), "$.id");
        String threadB = JsonPath.read(createThread(memberCookie, "Thread Beta Two"), "$.id");

        var replyResult = mockMvc.perform(post("/api/forum/replies")
                        .cookie(otherCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"threadId": "%s", "content": "Reply living in thread A here."}
                                """.formatted(threadA)))
                .andExpect(status().isCreated())
                .andReturn();
        String replyId = JsonPath.read(replyResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/forum/replies")
                        .cookie(otherCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"threadId": "%s", "content": "Trying to nest under another thread.", "parentReplyId": "%s"}
                                """.formatted(threadB, replyId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createThread_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/forum/" + GENERAL_CATEGORY + "/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "No Auth Thread", "content": "This is a sufficiently long thread body."}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
