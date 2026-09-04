package com.lionsclub.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Sql(statements = {"DELETE FROM events", "DELETE FROM gallery_items", "DELETE FROM news", "DELETE FROM members", "DELETE FROM users"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@ImportTestcontainers(TestcontainersConfiguration.class)
class GalleryControllerTest {

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

    private String uploadImage() throws Exception {
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{(byte) 0x89, 0x50});
        var result = mockMvc.perform(multipart("/api/gallery/upload")
                        .file(file)
                        .cookie(adminCookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value(
                        org.hamcrest.Matchers.startsWith("/api/uploads/gallery/")))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.imageUrl");
    }

    private String createItem(String title, String category, String imageUrl) throws Exception {
        var result = mockMvc.perform(post("/api/gallery")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "category": "%s", "imageUrl": "%s", "tags": ["outdoor", "outdoor", "team"]}
                                """.formatted(title, category, imageUrl)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    @Test
    void upload_asMember_shouldReturn403() throws Exception {
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{(byte) 0x89, 0x50});
        mockMvc.perform(multipart("/api/gallery/upload")
                        .file(file)
                        .cookie(memberCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_withNonImage_shouldReturn400() throws Exception {
        var file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());
        mockMvc.perform(multipart("/api/gallery/upload")
                        .file(file)
                        .cookie(adminCookie))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fullFlow_shouldUploadCreateListAndDelete() throws Exception {
        String imageUrl = uploadImage();
        String body = createItem("Beach Cleanup", "Event", imageUrl);
        String id = JsonPath.read(body, "$.id");

        org.assertj.core.api.Assertions.assertThat(
                ((java.util.List<String>) JsonPath.read(body, "$.tags"))).containsExactly("outdoor", "team");

        mockMvc.perform(get("/api/gallery?page=1&limit=12&category=Event"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id));

        mockMvc.perform(get("/api/gallery/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Beach Cleanup"));

        mockMvc.perform(get("/api/gallery/admin").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(patch("/api/gallery/" + id)
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Beach Cleanup 2026", "category": "Event", "imageUrl": "%s", "tags": []}
                                """.formatted(imageUrl)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Beach Cleanup 2026"));

        mockMvc.perform(delete("/api/gallery/" + id).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/gallery/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withoutImage_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/gallery")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "No Image", "category": "Event", "tags": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withUnknownEvent_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/gallery")
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Bad Event", "category": "Event", "imageUrl": "/api/uploads/gallery/x.png", "eventId": "%s", "tags": []}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }
}
