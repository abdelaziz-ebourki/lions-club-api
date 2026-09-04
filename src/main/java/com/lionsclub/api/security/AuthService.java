package com.lionsclub.api.security;

import com.lionsclub.api.domain.user.DisplayNames;
import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import com.lionsclub.api.web.dto.UserResponse;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String ERROR_DUPLICATE_EMAIL = "Email is already registered";
    public static final String ERROR_INVALID_TOKEN = "Invalid token";
    public static final String ERROR_EXPIRED_TOKEN = "Token expired";

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String DUMMY_PASSWORD_HASH = "$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQm4sEPhMNPfFhpYNnfOq";

    public AuthResult login(String email, String password) {
        var userOpt = userRepository.findByEmail(email);
        var user = userOpt.orElse(null);
        String expectedHash = user != null ? user.getPasswordHash() : DUMMY_PASSWORD_HASH;

        if (!passwordEncoder.matches(password, expectedHash)) {
            return AuthResult.failure("Invalid credentials");
        }

        if (user == null || !user.isEnabled()) {
            return AuthResult.failure("Invalid credentials");
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getRole(), user.getFirstName(), user.getLastName());
        return AuthResult.success(token);
    }

    @Transactional
    public AuthResult register(String email, String password, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            return AuthResult.failure(ERROR_DUPLICATE_EMAIL);
        }
        var user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(DisplayNames.firstNameOf(name));
        user.setLastName(DisplayNames.lastNameOf(name));
        user.setRole(Role.MEMBER);
        user.setEnabled(true);
        user.setEmailVerified(false);
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            return AuthResult.failure(ERROR_DUPLICATE_EMAIL);
        }
        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getRole(), user.getFirstName(), user.getLastName());
        return AuthResult.success(token);
    }

    public UserResponse getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .map(AuthService::toUserResponse)
                .orElse(null);
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                DisplayNames.displayName(user.getFirstName(), user.getLastName()),
                user.getEmail(),
                DisplayNames.apiRole(user.getRole()),
                user.getAvatarUrl(),
                user.isEmailVerified(),
                user.getCreatedAt());
    }

    public String refreshToken(UUID userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .map(user -> jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getRole(), user.getFirstName(), user.getLastName()))
                .orElse(null);
    }

    @Transactional
    public void resendVerification(UUID userId) {
        var user = userRepository.findById(userId).filter(User::isEnabled).orElse(null);
        if (user == null || user.isEmailVerified()) {
            return;
        }
        String token = newToken();
        user.setVerificationToken(token);
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        userRepository.save(user);
        log.info("Email verification token for {}: {}", user.getEmail(), token);
    }

    @Transactional
    public TokenResult verifyEmail(String token) {
        var user = userRepository.findByVerificationToken(token).orElse(null);
        if (user == null || !user.isEnabled()) {
            return TokenResult.failure(ERROR_INVALID_TOKEN);
        }
        if (user.getVerificationTokenExpiresAt() == null
                || user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return TokenResult.failure(ERROR_EXPIRED_TOKEN);
        }
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);
        return TokenResult.success();
    }

    @Transactional
    public void forgotPassword(String email) {
        var user = userRepository.findByEmail(email).filter(User::isEnabled).orElse(null);
        if (user == null) {
            return;
        }
        String token = newToken();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        log.info("Password reset token for {}: {}", user.getEmail(), token);
    }

    @Transactional
    public TokenResult resetPassword(String token, String newPassword) {
        var user = userRepository.findByPasswordResetToken(token).orElse(null);
        if (user == null || !user.isEnabled()) {
            return TokenResult.failure(ERROR_INVALID_TOKEN);
        }
        if (user.getPasswordResetTokenExpiresAt() == null
                || user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return TokenResult.failure(ERROR_EXPIRED_TOKEN);
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);
        return TokenResult.success();
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record AuthResult(boolean success, String token, String error) {
        public static AuthResult success(String token) {
            return new AuthResult(true, token, null);
        }

        public static AuthResult failure(String error) {
            return new AuthResult(false, null, error);
        }
    }

    public record TokenResult(boolean ok, String error) {
        public static TokenResult success() {
            return new TokenResult(true, null);
        }

        public static TokenResult failure(String error) {
            return new TokenResult(false, error);
        }
    }
}