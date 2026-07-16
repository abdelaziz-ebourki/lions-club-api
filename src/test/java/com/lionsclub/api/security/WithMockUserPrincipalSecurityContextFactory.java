package com.lionsclub.api.security;

import com.lionsclub.api.domain.user.Role;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockUserPrincipalSecurityContextFactory implements WithSecurityContextFactory<WithMockUserPrincipal> {

    @Override
    public SecurityContext createSecurityContext(WithMockUserPrincipal annotation) {
        UserPrincipal principal = new UserPrincipal(
                UUID.fromString(annotation.userId()),
                annotation.email(),
                Role.valueOf(annotation.role()),
                annotation.firstName(),
                annotation.lastName()
        );
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}