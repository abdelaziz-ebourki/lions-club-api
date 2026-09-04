package com.lionsclub.api.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
@Sql(statements = {"DELETE FROM events", "DELETE FROM users"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@ImportTestcontainers(TestcontainersConfiguration.class)
class AuthEmailFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        var user = new User();
        user.setEmail("verify@test.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFirstName("Verify");
        user.setLastName("Me");
        user.setRole(Role.MEMBER);
        user.setEnabled(true);
        userRepository.save(user);
    }

    private String loginCookie() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "verify@test.com", "password": "password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("auth_token").getValue();
    }

    @Test
    void fullVerificationFlow_shouldVerifyEmail() throws Exception {
        var cookie = new Cookie("auth_token", loginCookie());

        mockMvc.perform(post("/api/auth/resend-verification").cookie(cookie))
                .andExpect(status().isOk());

        String token = userRepository.findByEmail("verify@test.com").orElseThrow().getVerificationToken();
        org.assertj.core.api.Assertions.assertThat(token).isNotBlank();

        mockMvc.perform(post("/api/auth/verify-email").param("token", token).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        org.assertj.core.api.Assertions.assertThat(
                userRepository.findByEmail("verify@test.com").orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    void verifyEmail_withInvalidToken_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email").param("token", "nope")
                        .cookie(new Cookie("auth_token", loginCookie())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid token"));
    }

    @Test
    void resendVerification_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullPasswordResetFlow_shouldAllowLoginWithNewPassword() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "verify@test.com"}
                                """))
                .andExpect(status().isOk());

        String token = userRepository.findByEmail("verify@test.com").orElseThrow().getPasswordResetToken();
        org.assertj.core.api.Assertions.assertThat(token).isNotBlank();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "%s", "password": "brandnew123", "confirmPassword": "brandnew123"}
                                """.formatted(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "verify@test.com", "password": "brandnew123"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_withUnknownEmail_shouldStillReturn200() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nobody@test.com"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_withMismatchedConfirmation_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "whatever", "password": "brandnew123", "confirmPassword": "different123"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
