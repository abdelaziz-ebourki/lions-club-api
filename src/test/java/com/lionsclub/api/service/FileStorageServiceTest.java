package com.lionsclub.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileStorageServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldStorePngAndReturnPublicPath() {
        var service = new FileStorageService(tempDir.toString());
        MultipartFile file = new MockMultipartFile("avatar", "photo.png", "image/png", new byte[]{(byte) 0x89, 0x50});

        String path = service.store("avatars", file);

        assertThat(path).startsWith("/api/uploads/avatars/");
        assertThat(path).endsWith(".png");
        assertThat(tempDir.resolve("avatars").resolve(path.substring(path.lastIndexOf('/') + 1))).exists();
    }

    @Test
    void shouldRejectNonImageType() {
        var service = new FileStorageService(tempDir.toString());
        MultipartFile file = new MockMultipartFile("avatar", "notes.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> service.store("avatars", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PNG");
    }

    @Test
    void shouldRejectOversizedFile() {
        var service = new FileStorageService(tempDir.toString());
        MultipartFile file = new MockMultipartFile("avatar", "big.png", "image/png", new byte[6 * 1024 * 1024]);

        assertThatThrownBy(() -> service.store("avatars", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    void shouldRejectEmptyFile() {
        var service = new FileStorageService(tempDir.toString());
        MultipartFile file = new MockMultipartFile("avatar", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.store("avatars", file))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
