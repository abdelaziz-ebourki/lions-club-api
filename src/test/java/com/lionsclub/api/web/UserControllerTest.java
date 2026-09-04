package com.lionsclub.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionsclub.api.TestcontainersConfiguration;
import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
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
@Sql(statements = {"DELETE FROM events", "DELETE FROM users"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@ImportTestcontainers(TestcontainersConfiguration.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authCookie;

    @BeforeEach
    void setUp() throws Exception {
        var user = new User();
        user.setEmail("member@test.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFirstName("Test");
        user.setLastName("Member");
        user.setRole(Role.MEMBER);
        user.setEnabled(true);
        userRepository.save(user);

        var other = new User();
        other.setEmail("other@test.com");
        other.setPasswordHash(passwordEncoder.encode("password123"));
        other.setFirstName("Other");
        other.setLastName("User");
        other.setRole(Role.MEMBER);
        other.setEnabled(true);
        userRepository.save(other);

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "member@test.com", "password": "password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        authCookie = loginResult.getResponse().getCookie("auth_token").getValue();
    }

    @Test
    void shouldReturn401ForProfileWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnProfileWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("member@test.com"))
                .andExpect(jsonPath("$.name").value("Test Member"))
                .andExpect(jsonPath("$.role").value("member"));
    }

    @Test
    void shouldUpdateProfile() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", authCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Renamed Person", "email": "renamed@test.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Person"))
                .andExpect(jsonPath("$.email").value("renamed@test.com"));
    }

    @Test
    void shouldReturn409WhenEmailTaken() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", authCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Test Member", "email": "other@test.com"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldChangePasswordAndLoginWithNewPassword() throws Exception {
        mockMvc.perform(put("/api/user/password")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", authCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "password123", "newPassword": "newpassword123", "confirmPassword": "newpassword123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "member@test.com", "password": "newpassword123"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectPasswordChangeWithWrongCurrentPassword() throws Exception {
        mockMvc.perform(put("/api/user/password")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", authCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "wrong", "newPassword": "newpassword123", "confirmPassword": "newpassword123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectPasswordChangeWhenConfirmationMismatches() throws Exception {
        mockMvc.perform(put("/api/user/password")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", authCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "password123", "newPassword": "newpassword123", "confirmPassword": "different123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUploadAvatar() throws Exception {
        var avatar = new MockMultipartFile("avatar", "photo.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        mockMvc.perform(multipart("/api/user/profile")
                        .file(avatar)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", authCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar").value(org.hamcrest.Matchers.startsWith("/api/uploads/avatars/")));
    }

    @Test
    void shouldRejectNonImageAvatar() throws Exception {
        var avatar = new MockMultipartFile("avatar", "notes.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/user/profile")
                        .file(avatar)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", authCookie)))
                .andExpect(status().isBadRequest());
    }
}
