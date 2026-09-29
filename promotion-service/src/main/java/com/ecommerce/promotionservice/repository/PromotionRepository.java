package com.ecommerce.promotionservice.repository;

import com.ecommerce.promotionservice.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    Optional<Promotion> findByCodeIgnoreCase(String code);
}
