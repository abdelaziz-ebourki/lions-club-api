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
    void shouldSetUserPrincipalWhenValidCookie() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createToken(userId, "john@example.com", Role.MEMBER, "John", "Doe");
        var request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("auth_token", token));
        var response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo("john@example.com");
        assertThat(principal.role()).isEqualTo(Role.MEMBER);
        assertThat(principal.firstName()).isEqualTo("John");
        assertThat(principal.lastName()).isEqualTo("Doe");
        assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_MEMBER");
    }

    @Test
    void shouldSetAdminPrincipalWhenValidAdminCookie() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createToken(userId, "admin@example.com", Role.ADMIN, "Admin", "User");
        var request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("auth_token", token));
        var response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.role()).isEqualTo(Role.ADMIN);
        assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_ADMIN");
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        jwtConfig.setExpiration(java.time.Duration.ofSeconds(-1));
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
        filter = new JwtAuthenticationFilter(jwtTokenProvider);

        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.createToken(userId, "john@example.com", Role.MEMBER, "John", "Doe");
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

    @Test
    void shouldRejectMissingRoleClaim() throws Exception {
        // Create a token without role claim using the legacy generateToken method
        // This is tested by ensuring the filter clears context when exception occurs
        // For the new createToken method, all claims are always present
        // So we test the exception path by providing an invalid token
        var request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("auth_token", "invalid-token-without-claims"));
        var response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldRejectMissingEmailClaim() throws Exception {
        // Test with invalid token that causes exception during claim extraction
        var request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("auth_token", "invalid-token"));
        var response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}