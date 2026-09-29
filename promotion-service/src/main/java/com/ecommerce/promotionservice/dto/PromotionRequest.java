package com.ecommerce.promotionservice.dto;

import com.ecommerce.promotionservice.entity.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin(value = "0.01") BigDecimal discountValue,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt,
        Boolean active
) {
}
