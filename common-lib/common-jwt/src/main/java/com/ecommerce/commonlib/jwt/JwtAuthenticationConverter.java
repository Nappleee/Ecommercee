package com.ecommerce.commonlib.jwt;
import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class JwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");

        Collection<GrantedAuthority> authorities = roles == null
                ? List.of()
                : roles.stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
        System.out.println(
        "JWT roles = " + roles
        + ", authorities = " + authorities
        );
        return new JwtAuthenticationToken(jwt, authorities);
    }
}