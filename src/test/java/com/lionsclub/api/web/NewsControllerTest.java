package com.lionsclub.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lionsclub.api.TestcontainersConfiguration;
import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import jakarta.servlet.http.Cookie;
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
@Sql(statements = {"DELETE FROM events", "DELETE FROM news", "DELETE FROM members", "DELETE FROM users"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@ImportTestcontainers(TestcontainersConfiguration.class)
class NewsControllerTest {

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

    private String createArticle(String title, String status) throws Exception {
        var result = mockMvc.perform(multipart("/api/news")
                        .param("title", title)
                        .param("content", "<p>Body of " + title + "</p>")
                        .param("category", "News")
                        .param("status", status)
                        .cookie(adminCookie))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    @Test
    void publicList_shouldContainOnlyPublishedWithoutContent() throws Exception {
        createArticle("Published One", "published");
        createArticle("Draft One", "draft");

        mockMvc.perform(get("/api/news?page=1&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Published One"))
                .andExpect(jsonPath("$.data[0].content").doesNotExist())
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void featured_shouldReturnPublishedSummaries() throws Exception {
        createArticle("Published One", "published");

        mockMvc.perform(get("/api/news/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").doesNotExist());
    }

    @Test
    void getBySlug_shouldReturnFullPublishedArticle() throws Exception {
        String body = createArticle("My Great News", "published");
        String slug = JsonPath.read(body, "$.slug");

        mockMvc.perform(get("/api/news/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.authorName").value("Test User"));
    }

    @Test
    void getBySlug_forDraft_shouldReturn404() throws Exception {
        String body = createArticle("Hidden Draft", "draft");
        String slug = JsonPath.read(body, "$.slug");

        mockMvc.perform(get("/api/news/" + slug))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminList_shouldRequireAdmin() throws Exception {
        createArticle("Published One", "published");

        mockMvc.perform(get("/api/news/admin"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/news/admin").cookie(memberCookie))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/news/admin").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").exists());
    }

    @Test
    void create_shouldUniqueifySlugOnCollision() throws Exception {
        String first = createArticle("Same Title", "published");
        String second = createArticle("Same Title", "published");

        String slug1 = JsonPath.read(first, "$.slug");
        String slug2 = JsonPath.read(second, "$.slug");

        org.assertj.core.api.Assertions.assertThat(slug1).isEqualTo("same-title");
        org.assertj.core.api.Assertions.assertThat(slug2).isEqualTo("same-title-2");
    }

    @Test
    void create_withEmptyContent_shouldReturn400() throws Exception {
        mockMvc.perform(multipart("/api/news")
                        .param("title", "Bad Article")
                        .param("content", "<p>   </p>")
                        .param("category", "News")
                        .param("status", "draft")
                        .cookie(adminCookie))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_shouldRemoveArticle() throws Exception {
        String body = createArticle("To Delete", "draft");
        String id = JsonPath.read(body, "$.id");

        mockMvc.perform(delete("/api/news/" + id).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/news/admin/" + id).cookie(adminCookie))
                .andExpect(status().isNotFound());
    }
}
