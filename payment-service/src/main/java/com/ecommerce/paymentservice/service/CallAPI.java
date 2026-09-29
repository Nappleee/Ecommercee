package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.OrderDto;
import com.ecommerce.paymentservice.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CallAPI {

    private final RestClient.Builder restClientBuilder;
    @Value("${services.order.url:http://order-service:8084}")
    private String orderServiceUrl;
    @Value("${services.auth.url:http://auth-service:8088}")
    private String authServiceUrl;
    @Value("${services.product.url:http://product-service:8086}")
    private String productServiceUrl;

    public OrderDto receiverPaymentDto(Integer orderId, String token) {
        return restClientBuilder.baseUrl(orderServiceUrl).build()
                .get()
                .uri("/api/orders/{id}", orderId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .body(OrderDto.class);
    }

    public UserDto receiverUserDto(Long userId, String token) {
        return restClientBuilder.baseUrl(authServiceUrl).build()
                .get()
                .uri("/api/manager/user/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .body(UserDto.class);
    }

    public void decrementProductQuantity(Integer productId, Integer amount, String token) {
        restClientBuilder.baseUrl(productServiceUrl).build()
                .patch()
                .uri(uriBuilder -> uriBuilder.path("/api/products/{id}/decrement")
                        .queryParam("amount", amount)
                        .build(productId))
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .toBodilessEntity();
    }
}