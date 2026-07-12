package com.lionsclub.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
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
}
