package com.lionsclub.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.lionsclub.api.domain.user.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private JwtConfig jwtConfig;
    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthenticationFilter filter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("test-secret-key-that-is-at-least-256-bits-long-for-hs256");
        jwtConfig.setExpiration(java.time.Duration.ofMinutes(15));
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Test
    void shouldPassThroughWhenNoCookie() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSetSecurityContextWhenValidCookie() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId, Role.MEMBER);
        var request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("auth_token", token));
        var response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(userId.toString());
        assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_MEMBER");
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        jwtConfig.setExpiration(java.time.Duration.ofSeconds(-1));
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId, Role.MEMBER);
        var request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("auth_token", token));
        var response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldRejectTamperedToken() throws Exception {
        var request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("auth_token", "invalid-token"));
        var response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
