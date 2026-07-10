package com.lionsclub.api.security;

import com.lionsclub.api.domain.user.Role;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String ERROR_DUPLICATE_EMAIL = "Email is already registered";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResult login(String email, String password) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return AuthResult.failure("Invalid credentials");
        }
        var user = userOpt.get();
        if (!user.isEnabled()) {
            return AuthResult.failure("Account disabled");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return AuthResult.failure("Invalid credentials");
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole());
        return AuthResult.success(token);
    }

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
        user = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole());
        return AuthResult.success(token);
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
