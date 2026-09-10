package com.lionsclub.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CorsPropertiesTest {

    @Test
    void shouldDefaultToCanonicalLocalUiOrigins() {
        assertThat(new CorsProperties().getAllowedOrigins())
                .containsExactly("http://localhost:5173", "http://localhost:5174");
    }

    @Test
    void shouldAcceptMultipleConfiguredOrigins() {
        var properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("http://localhost:5174", "https://club.example.org"));

        assertThat(properties.getAllowedOrigins())
                .containsExactly("http://localhost:5174", "https://club.example.org");
    }
}
