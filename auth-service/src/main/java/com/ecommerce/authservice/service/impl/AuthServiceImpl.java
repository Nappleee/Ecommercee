package com.ecommerce.authservice.service.impl;

import com.ecommerce.authservice.config.JwtProperties;
import com.ecommerce.authservice.dto.request.LoginRequest;
import com.ecommerce.authservice.dto.request.RegisterRequest;
import com.ecommerce.authservice.dto.response.AuthResponse;
import com.ecommerce.authservice.entity.Role;
import com.ecommerce.authservice.entity.RoleName;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.repository.RoleRepository;
import com.ecommerce.authservice.repository.UserRepository;
import com.ecommerce.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new IllegalArgumentException("Username already exists");
        }

        Set<Role> roles = resolveRoles(request.getRoles());
        User user = User.builder()
                .fullName(request.getFullName())
                .userName(request.getUserName())
                .email(request.getEmail())
                .gender(request.getGender())
                .phone(request.getPhone())
                .avatar(request.getAvatar())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .build();
        userRepository.save(user);
        return issueTokens(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUserName(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return issueTokens(user);
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        Jwt jwt = jwtDecoder.decode(refreshToken);
        if (!"refresh".equals(jwt.getClaimAsString("token_type"))) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        User user = userRepository.findByUserName(jwt.getSubject())
                .orElseThrow(() -> new BadCredentialsException("Unknown user"));
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(role -> role.getName().name()).toList();
        String accessToken = encodeToken(user.getUserName(), roles, "access", now, jwtProperties.getAccessTokenTtl());
        String refreshToken = encodeToken(user.getUserName(), roles, "refresh", now, jwtProperties.getRefreshTokenTtl());
        return AuthResponse.builder()
                .tokenType("Bearer")
                .username(user.getUserName())
                .roles(roles)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(now.plus(jwtProperties.getAccessTokenTtl()))
                .build();
    }

    private String encodeToken(
        String subject,
        List<String> roles,
        String tokenType,
        Instant issuedAt,
        java.time.Duration ttl
) {
    JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(jwtProperties.getIssuer())
            .subject(subject)
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(ttl))
            .claim("roles", roles)
            .claim("token_type", tokenType)
            .build();

    JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256)
            .type("JWT")
            .build();

    JwtEncoderParameters parameters =
            JwtEncoderParameters.from(jwsHeader, claims);

    return jwtEncoder.encode(parameters).getTokenValue();
}

    private Set<Role> resolveRoles(Set<String> requestedRoles) {
        Set<String> effectiveRoles = requestedRoles == null || requestedRoles.isEmpty()
                ? Set.of(RoleName.USER.name())
                : requestedRoles;
        Set<Role> roles = new HashSet<>();
        for (String roleName : effectiveRoles) {
            RoleName parsedRole = RoleName.valueOf(roleName.toUpperCase());
            Role role = roleRepository.findByName(parsedRole)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(parsedRole).build()));
            roles.add(role);
        }
        return roles;
    }
}