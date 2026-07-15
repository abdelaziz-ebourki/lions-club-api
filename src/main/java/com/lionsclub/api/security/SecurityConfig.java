package com.lionsclub.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String EVENTS_PATH = "/api/events/**";
    private static final String MEMBERS_PATH = "/api/members/**";
    private static final String FORUM_REPLIES_PATH = "/api/forum/replies/**";
    private static final String FORUM_THREADS_PATH = "/api/forum/threads/**";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, EVENTS_PATH).permitAll()
                        .requestMatchers(HttpMethod.POST, EVENTS_PATH).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, EVENTS_PATH).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, EVENTS_PATH).hasRole(ADMIN)
                        .requestMatchers("/api/admin/**").hasRole(ADMIN)
                        .requestMatchers("/api/contact").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/members").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, MEMBERS_PATH).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, MEMBERS_PATH).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, FORUM_REPLIES_PATH).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH, FORUM_THREADS_PATH).hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, FORUM_THREADS_PATH).hasRole(ADMIN)
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                    Map.of("error", "Unauthorized"));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                    Map.of("error", "Forbidden"));
        };
    }
}
