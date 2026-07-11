package com.lionsclub.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String DELETE = "DELETE";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String PATCH = "PATCH";
    private static final String EVENTS_PATH = "/api/events/";
    private static final String EVENTS_ROOT = "/api/events";

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
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(r -> "GET".equals(r.getMethod())
                                && (EVENTS_ROOT.equals(r.getRequestURI())
                                || r.getRequestURI().startsWith(EVENTS_PATH))).permitAll()
                        .requestMatchers(r -> POST.equals(r.getMethod())
                                && (EVENTS_ROOT.equals(r.getRequestURI())
                                || EVENTS_PATH.equals(r.getRequestURI()))).hasRole(ADMIN)
                        .requestMatchers(r -> PUT.equals(r.getMethod())
                                && r.getRequestURI().startsWith(EVENTS_PATH)).hasRole(ADMIN)
                        .requestMatchers(r -> DELETE.equals(r.getMethod())
                                && r.getRequestURI().startsWith(EVENTS_PATH)).hasRole(ADMIN)
                        .requestMatchers(r -> r.getRequestURI().startsWith("/api/admin/")).hasRole(ADMIN)
                        .requestMatchers(r -> "/api/contact".equals(r.getRequestURI())).hasRole(ADMIN)
                        .requestMatchers(r -> POST.equals(r.getMethod())
                                && "/api/members".equals(r.getRequestURI())).hasRole(ADMIN)
                        .requestMatchers(r -> PUT.equals(r.getMethod())
                                && r.getRequestURI().startsWith("/api/members/")).hasRole(ADMIN)
                        .requestMatchers(r -> DELETE.equals(r.getMethod())
                                && r.getRequestURI().startsWith("/api/members/")).hasRole(ADMIN)
                        .requestMatchers(r -> DELETE.equals(r.getMethod())
                                && r.getRequestURI().startsWith("/api/forum/replies/")).hasRole(ADMIN)
                        .requestMatchers(r -> PATCH.equals(r.getMethod())
                                && r.getRequestURI().startsWith("/api/forum/threads/")).hasRole(ADMIN)
                        .requestMatchers(r -> DELETE.equals(r.getMethod())
                                && r.getRequestURI().startsWith("/api/forum/threads/")).hasRole(ADMIN)
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
