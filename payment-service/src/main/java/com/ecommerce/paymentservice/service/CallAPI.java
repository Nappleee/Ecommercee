package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.OrderDto;
import com.ecommerce.paymentservice.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Component
public class CallAPI {

    private final RestClient.Builder restClientBuilder;
    private final String orderServiceUrl;
    private final String authServiceUrl;

    public CallAPI(
            RestClient.Builder restClientBuilder,
            @Value("${services.order.url:http://order-service:8084}") String orderServiceUrl,
            @Value("${services.auth.url:http://auth-service:8088}") String authServiceUrl
    ) {
        this.restClientBuilder = restClientBuilder;
        this.orderServiceUrl = orderServiceUrl;
        this.authServiceUrl = authServiceUrl;
    }

    public OrderDto receiverPaymentDto(Integer orderId, String token) {
        return restClientBuilder.baseUrl(orderServiceUrl).build()
                .get()
                .uri("/api/orders/{id}", orderId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .body(OrderDto.class);
    }

    public UserDto receiverUserDto(Long userId, String token) {
        Map<?, ?> response = restClientBuilder.baseUrl(authServiceUrl).build()
                .get()
                .uri("/api/v1/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .body(Map.class);
        if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
            return null;
        }
        UserDto userDto = new UserDto();
        userDto.setId(asLong(data.get("id")));
        userDto.setFullname((String) data.get("fullname"));
        userDto.setUsername((String) data.get("username"));
        userDto.setEmail((String) data.get("email"));
        userDto.setGender((String) data.get("gender"));
        userDto.setPhone((String) data.get("phone"));
        userDto.setAvatar((String) data.get("avatar"));
        return userDto;
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}