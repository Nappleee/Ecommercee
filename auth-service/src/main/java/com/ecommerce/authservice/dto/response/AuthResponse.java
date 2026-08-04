package com.ecommerce.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private final String tokenType;
    private final String accessToken;
    private final String refreshToken;
    private final String username;
    private final List<String> roles;
    private final Instant expiresAt;
}