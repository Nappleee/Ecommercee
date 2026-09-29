package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.product.ProductDto;
import com.ecommerce.orderservice.dto.user.UserDto;
import com.ecommerce.commonlib.viewmodel.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CallAPI {
    private final RestClient.Builder restClientBuilder;

    public UserDto receiverCurrentUserDto(String token) {
        ApiResponse<UserDto> response = restClientBuilder.baseUrl("http://auth-service:8088").build()
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return response.data();
    }

    public UserDto receiverUserDto(Long userId, String token) {
        ApiResponse<UserDto> response = restClientBuilder.baseUrl("http://auth-service:8088").build()
                .get()
                .uri("/api/v1/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return response.data();
    }
    public ProductDto receiverProductDto(Integer productId) {
        return restClientBuilder.baseUrl("http://product-service:8086").build()
                .get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .body(ProductDto.class);
    }
}
