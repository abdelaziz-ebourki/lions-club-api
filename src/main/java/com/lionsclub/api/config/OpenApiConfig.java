package com.lionsclub.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev", "test"})
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        var securitySchemeName = "cookie-jwt";
        return new OpenAPI()
                .info(new Info()
                        .title("Lions Club FSBM API")
                        .version("0.0.1")
                        .description("REST API for Lions Club FSBM — member management, events, and RSVPs"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("auth_token")
                                .description("JWT obtained from /api/auth/login, sent as auth_token cookie")));
    }
}
