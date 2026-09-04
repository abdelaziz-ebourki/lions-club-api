package com.lionsclub.api.service;

import com.lionsclub.api.domain.user.DisplayNames;
import com.lionsclub.api.domain.user.User;
import com.lionsclub.api.infrastructure.persistence.UserRepository;
import com.lionsclub.api.security.AuthService;
import com.lionsclub.api.web.dto.UserResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    public static final String ERROR_DUPLICATE_EMAIL = "Email is already registered";
    public static final String ERROR_INVALID_CURRENT_PASSWORD = "Current password is incorrect";
    public static final String ERROR_PASSWORD_MISMATCH = "New passwords do not match";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public UserResponse getProfile(UUID userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .map(AuthService::toUserResponse)
                .orElse(null);
    }

    @Transactional
    public ProfileResult updateProfile(UUID userId, String name, String email) {
        var user = userRepository.findById(userId).filter(User::isEnabled).orElse(null);
        if (user == null) {
            return ProfileResult.unauthorized();
        }
        if (!user.getEmail().equalsIgnoreCase(email)
                && userRepository.findByEmail(email).isPresent()) {
            return ProfileResult.duplicateEmail();
        }
        user.setFirstName(DisplayNames.firstNameOf(name));
        user.setLastName(DisplayNames.lastNameOf(name));
        user.setEmail(email);
        userRepository.save(user);
        return ProfileResult.ok(AuthService.toUserResponse(user));
    }

    @Transactional
    public ProfileResult updateAvatar(UUID userId, MultipartFile avatar) {
        var user = userRepository.findById(userId).filter(User::isEnabled).orElse(null);
        if (user == null) {
            return ProfileResult.unauthorized();
        }
        user.setAvatarUrl(fileStorageService.store("avatars", avatar));
        userRepository.save(user);
        return ProfileResult.ok(AuthService.toUserResponse(user));
    }

    @Transactional
    public PasswordResult changePassword(UUID userId, String currentPassword, String newPassword, String confirmPassword) {
        var user = userRepository.findById(userId).filter(User::isEnabled).orElse(null);
        if (user == null) {
            return PasswordResult.unauthorized();
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return PasswordResult.failure(ERROR_INVALID_CURRENT_PASSWORD);
        }
        if (!newPassword.equals(confirmPassword)) {
            return PasswordResult.failure(ERROR_PASSWORD_MISMATCH);
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return PasswordResult.success();
    }

    public record ProfileResult(boolean present, boolean emailTaken, UserResponse profile) {
        public static ProfileResult ok(UserResponse profile) {
            return new ProfileResult(true, false, profile);
        }

        public static ProfileResult unauthorized() {
            return new ProfileResult(false, false, null);
        }

        public static ProfileResult duplicateEmail() {
            return new ProfileResult(true, true, null);
        }
    }

    public record PasswordResult(boolean present, boolean changed, String error) {
        public static PasswordResult success() {
            return new PasswordResult(true, true, null);
        }

        public static PasswordResult failure(String error) {
            return new PasswordResult(true, false, error);
        }

        public static PasswordResult unauthorized() {
            return new PasswordResult(false, false, null);
        }
    }
}
