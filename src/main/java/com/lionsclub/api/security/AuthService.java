package com.lionsclub.api.security;

import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import com.lionsclub.api.web.dto.UserResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String ERROR_DUPLICATE_EMAIL = "Email is already registered";

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
    public AuthResult register(String email, String password, String firstName, String lastName) {
        if (userRepository.findByEmail(email).isPresent()) {
            return AuthResult.failure(ERROR_DUPLICATE_EMAIL);
        }
        var user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(Role.MEMBER);
        user.setEnabled(true);
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
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getRole().name()))
                .orElse(null);
    }

    public String refreshToken(UUID userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .map(user -> jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getRole(), user.getFirstName(), user.getLastName()))
                .orElse(null);
    }

    public record AuthResult(boolean success, String token, String error) {
        public static AuthResult success(String token) {
            return new AuthResult(true, token, null);
        }

        public static AuthResult failure(String error) {
            return new AuthResult(false, null, error);
        }
    }
}