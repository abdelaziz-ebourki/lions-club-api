package com.lionsclub.api.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final Path root;

    public FileStorageService(@Value("${app.upload.dir:/app/uploads}") String uploadDir) {
        this.root = Path.of(uploadDir);
    }

    public String store(String subdirectory, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File must not exceed 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only PNG, JPEG and WebP images are allowed");
        }
        try {
            Path dir = root.resolve(subdirectory);
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + extensionFor(contentType);
            Files.copy(file.getInputStream(), dir.resolve(filename));
            return "/api/uploads/" + subdirectory + "/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
