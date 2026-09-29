package com.ecommerce.paymentservice.service;

import com.ecommerce.commonlib.viewmodel.ApiResponse;
import com.ecommerce.paymentservice.dto.OrderDto;
import com.ecommerce.paymentservice.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CallAPI {

    private final RestClient.Builder restClientBuilder;

    public UserDto receiverCurrentUserDto(String token) {
        ApiResponse<UserDto> response = restClientBuilder.baseUrl("http://AUTH-SERVICE").build()
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return response.data();
    }

    public OrderDto receiverPaymentDto(Integer orderId, String token) {
        return restClientBuilder.baseUrl("http://ORDER-SERVICE").build()
                .get()
                .uri("/api/orders/{id}", orderId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .body(OrderDto.class);
    }

    public UserDto receiverUserDto(Long userId, String token) {
        ApiResponse<UserDto> response = restClientBuilder.baseUrl("http://AUTH-SERVICE").build()
                .get()
                .uri("/api/v1/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return response.data();
    }
}