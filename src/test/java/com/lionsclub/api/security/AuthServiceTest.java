package com.lionsclub.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import com.lionsclub.api.web.dto.UserResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private JwtConfig jwtConfig;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("test-secret-key-that-is-at-least-256-bits-long-for-hs256");
        jwtConfig.setExpiration(java.time.Duration.ofMinutes(15));
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void login_withDisabledAccount_shouldReturnInvalidCredentials() {
        var user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("disabled@test.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setEnabled(false);
        user.setRole(Role.MEMBER);

        when(userRepository.findByEmail("disabled@test.com")).thenReturn(Optional.of(user));

        var result = authService.login("disabled@test.com", "password123");

        assertThat(result.success()).isFalse();
        assertThat(result.error()).isEqualTo("Invalid credentials");
    }

    @Test
    void register_withConcurrentDuplicateEmail_shouldReturnDuplicateEmailError() {
        var user = new User();
        user.setEmail("dup@test.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFirstName("Dup");
        user.setLastName("User");
        user.setRole(Role.MEMBER);
        user.setEnabled(true);

        when(userRepository.findByEmail("dup@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenThrow(DataIntegrityViolationException.class);

        var result = authService.register("dup@test.com", "password123", "Dup", "User");

        assertThat(result.success()).isFalse();
        assertThat(result.error()).isEqualTo(AuthService.ERROR_DUPLICATE_EMAIL);
    }

    @Test
    void getCurrentUser_shouldReturnUserResponse() {
        var userId = UUID.randomUUID();
        var user = new User();
        user.setId(userId);
        user.setEmail("test@test.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(Role.MEMBER);
        user.setEnabled(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        var response = authService.getCurrentUser(userId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.firstName()).isEqualTo("Test");
        assertThat(response.lastName()).isEqualTo("User");
        assertThat(response.role()).isEqualTo("MEMBER");
    }

    @Test
    void getCurrentUser_shouldReturnNullForDisabledUser() {
        var userId = UUID.randomUUID();
        var user = new User();
        user.setId(userId);
        user.setEmail("disabled@test.com");
        user.setEnabled(false);
        user.setRole(Role.MEMBER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        var response = authService.getCurrentUser(userId);

        assertThat(response).isNull();
    }

    @Test
    void getCurrentUser_shouldReturnNullForNonExistentUser() {
        var userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        var response = authService.getCurrentUser(userId);

        assertThat(response).isNull();
    }

    @Test
    void refreshToken_shouldIssueNewToken() {
        var userId = UUID.randomUUID();
        var user = new User();
        user.setId(userId);
        user.setEmail("refresh@test.com");
        user.setFirstName("Refresh");
        user.setLastName("User");
        user.setRole(Role.MEMBER);
        user.setEnabled(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        var token = authService.refreshToken(userId);

        assertThat(token).isNotNull();
        var decoded = jwtTokenProvider.validateToken(token);
        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getClaim("role").asString()).isEqualTo("MEMBER");
    }

    @Test
    void refreshToken_shouldReturnNullForDisabledUser() {
        var userId = UUID.randomUUID();
        var user = new User();
        user.setId(userId);
        user.setEmail("disabled@test.com");
        user.setEnabled(false);
        user.setRole(Role.MEMBER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        var token = authService.refreshToken(userId);

        assertThat(token).isNull();
    }
}
