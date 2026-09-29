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
import com.ecommerce.authservice.entity.RefreshToken;
import com.ecommerce.authservice.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    @Override
    @Transactional
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
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUserName(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        Jwt jwt = jwtDecoder.decode(refreshToken);

        if (!"refresh".equals(jwt.getClaimAsString("token_type"))) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        if (jwt.getId() == null || jwt.getId().isBlank()) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHash(hashToken(refreshToken))
                .orElseThrow(() -> new BadCredentialsException("Refresh token not found"));

        if (!storedToken.getJti().equals(jwt.getId())) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        if (storedToken.isRevoked()) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token has expired");
        }

        // Rotation: token cũ không thể dùng lại sau lần refresh này.
        storedToken.setRevoked(true);
        storedToken.setRevokedAt(Instant.now());

        // Vì method có @Transactional, Hibernate tự UPDATE token cũ khi transaction kết thúc.
        return issueTokens(storedToken.getUser());
    }

    private AuthResponse issueTokens(User user) {
        Instant now = Instant.now();

        List<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getName().name())
                .toList();

        String accessToken = encodeAccessToken(
                user.getId(),
                user.getUserName(),
                roles,
                now
        );

        IssuedRefreshToken issuedRefreshToken = encodeRefreshToken(
                user.getUserName(),
                now
        );

        saveRefreshToken(user, issuedRefreshToken);

        return AuthResponse.builder()
                .tokenType("Bearer")
                .username(user.getUserName())
                .roles(roles)
                .accessToken(accessToken)
                .refreshToken(issuedRefreshToken.tokenValue())
                .expiresAt(now.plus(jwtProperties.getAccessTokenTtl()))
                .build();
    }

    private String encodeAccessToken(
            Long userId,
            String subject,
            List<String> roles,
            Instant issuedAt
    ) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(jwtProperties.getAccessTokenTtl()))
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("token_type", "access")
                .build();

        return encode(claims);
    }
    private IssuedRefreshToken encodeRefreshToken(
            String subject,
            Instant issuedAt
    ) {
        String jti = UUID.randomUUID().toString();
        Instant expiresAt = issuedAt.plus(jwtProperties.getRefreshTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(subject)
                .id(jti)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("token_type", "refresh")
                .build();

        return new IssuedRefreshToken(
                encode(claims),
                jti,
                expiresAt
        );
    }
    private String encode(JwtClaimsSet claims) {
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(jwsHeader, claims)
        ).getTokenValue();
    }
    private void saveRefreshToken(User user, IssuedRefreshToken issuedRefreshToken) {
    RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .jti(issuedRefreshToken.jti())
            .tokenHash(hashToken(issuedRefreshToken.tokenValue()))
            .expiresAt(issuedRefreshToken.expiresAt())
            .revoked(false)
            .build();

    refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private record IssuedRefreshToken(
            String tokenValue,
            String jti,
            Instant expiresAt
    ) {
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

    @Override
    @Transactional
    public void logout(String authorizationHeader, String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is required for logout");
        }

        revokeRefreshTokenInternal(refreshToken);
    }

    @Override
    public void revokeAccessToken(String authorizationHeader) {
        // Access token vẫn stateless và sống ngắn (15 phút).
        // Chưa có access-token blacklist ở phạm vi hiện tại.
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        revokeRefreshTokenInternal(refreshToken);
    }

    private void revokeRefreshTokenInternal(String refreshToken) {
        refreshTokenRepository.findByTokenHash(hashToken(refreshToken))
                .ifPresent(token -> {
                    if (!token.isRevoked()) {
                        token.setRevoked(true);
                        token.setRevokedAt(Instant.now());
                    }
                });
    }
}