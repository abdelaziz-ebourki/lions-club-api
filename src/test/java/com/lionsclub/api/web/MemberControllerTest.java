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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Sql(statements = {"DELETE FROM events", "DELETE FROM members", "DELETE FROM users"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@ImportTestcontainers(TestcontainersConfiguration.class)
class MemberControllerTest {

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
        createUser("admin@test.com", "password123", Role.ADMIN);
        createUser("member@test.com", "password123", Role.MEMBER);
        adminCookie = login("admin@test.com");
        memberCookie = login("member@test.com");
    }

    private void createUser(String email, String password, Role role) {
        var user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
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
    void publicList_shouldReturnEmptyArrayInitially() throws Exception {
        mockMvc.perform(get("/api/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void createAsAdmin_shouldReturn201WithDerivedName() throws Exception {
        mockMvc.perform(multipart("/api/members")
                        .param("name", "Jane Doe")
                        .param("role", "President")
                        .param("bio", "Serving since 2020")
                        .cookie(adminCookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.role").value("President"))
                .andExpect(jsonPath("$.bio").value("Serving since 2020"))
                .andExpect(jsonPath("$.joinedAt").exists());
    }

    @Test
    void createAsAdmin_withAvatarFile_shouldStoreFile() throws Exception {
        var avatar = new MockMultipartFile("avatar", "photo.png", "image/png", new byte[]{(byte) 0x89, 0x50});

        mockMvc.perform(multipart("/api/members")
                        .file(avatar)
                        .param("name", "John Smith")
                        .param("role", "Member")
                        .cookie(adminCookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avatar").value(
                        org.hamcrest.Matchers.startsWith("/api/uploads/members/")));
    }

    @Test
    void create_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(multipart("/api/members")
                        .param("name", "Jane Doe")
                        .param("role", "President"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_asMember_shouldReturn403() throws Exception {
        mockMvc.perform(multipart("/api/members")
                        .param("name", "Jane Doe")
                        .param("role", "President")
                        .cookie(memberCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withBlankName_shouldReturn400() throws Exception {
        mockMvc.perform(multipart("/api/members")
                        .param("name", "J")
                        .param("role", "President")
                        .cookie(adminCookie))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_shouldReturnMember() throws Exception {
        var created = mockMvc.perform(multipart("/api/members")
                        .param("name", "Jane Doe")
                        .param("role", "President")
                        .cookie(adminCookie))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/members/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    @Test
    void getById_withUnknownId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/members/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAsAdmin_shouldKeepAvatarWhenOmitted() throws Exception {
        var avatar = new MockMultipartFile("avatar", "photo.png", "image/png", new byte[]{(byte) 0x89, 0x50});
        var created = mockMvc.perform(multipart("/api/members")
                        .file(avatar)
                        .param("name", "Jane Doe")
                        .param("role", "President")
                        .cookie(adminCookie))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        String avatarUrl = JsonPath.read(created.getResponse().getContentAsString(), "$.avatar");

        mockMvc.perform(multipart("/api/members/" + id)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .param("name", "Jane Updated")
                        .param("role", "President")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Updated"))
                .andExpect(jsonPath("$.avatar").value(avatarUrl));
    }

    @Test
    void deleteAsAdmin_shouldRemoveMember() throws Exception {
        var created = mockMvc.perform(multipart("/api/members")
                        .param("name", "Jane Doe")
                        .param("role", "President")
                        .cookie(adminCookie))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/members/" + id).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/members/" + id))
                .andExpect(status().isNotFound());
    }
}
