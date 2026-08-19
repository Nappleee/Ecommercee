package com.ecommerce.authservice.service;

import com.ecommerce.authservice.dto.request.LoginRequest;
import com.ecommerce.authservice.dto.request.RegisterRequest;
import com.ecommerce.authservice.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);

    void logout(String authorizationHeader, String refreshToken);

    void revokeAccessToken(String authorizationHeader);

    void revokeRefreshToken(String refreshToken);
}