package com.lionsclub.api.security;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {

    private String secret;
    private Duration expiration = Duration.ofMinutes(15);
    private boolean secure;

    @PostConstruct
    void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret must be configured");
        }
    }
}
