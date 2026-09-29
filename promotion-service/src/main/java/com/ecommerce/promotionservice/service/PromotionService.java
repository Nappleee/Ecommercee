package com.ecommerce.promotionservice.service;

import com.ecommerce.promotionservice.dto.PromotionRequest;
import com.ecommerce.promotionservice.entity.Promotion;
import com.ecommerce.promotionservice.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionService {

    private final PromotionRepository repository;

    @Transactional(readOnly = true)
    public List<Promotion> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Promotion findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found: " + id));
    }

    public Promotion create(PromotionRequest request) {
        validateDates(request);
        if (repository.existsByCodeIgnoreCase(normalizeCode(request.code()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Promotion code already exists");
        }
        return repository.save(toEntity(new Promotion(), request));
    }

    public Promotion update(Long id, PromotionRequest request) {
        validateDates(request);
        if (repository.existsByCodeIgnoreCaseAndIdNot(normalizeCode(request.code()), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Promotion code already exists");
        }
        return repository.save(toEntity(findById(id), request));
    }

    public void delete(Long id) {
        repository.delete(findById(id));
    }

    private Promotion toEntity(Promotion promotion, PromotionRequest request) {
        promotion.setCode(normalizeCode(request.code()));
        promotion.setName(request.name().trim());
        promotion.setDiscountType(request.discountType());
        promotion.setDiscountValue(request.discountValue());
        promotion.setStartsAt(request.startsAt());
        promotion.setEndsAt(request.endsAt());
        promotion.setActive(request.active() == null || request.active());
        return promotion;
    }

    private void validateDates(PromotionRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endsAt must be after startsAt");
        }
        if (request.discountType() == com.ecommerce.promotionservice.entity.DiscountType.PERCENTAGE
                && request.discountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Percentage discount cannot exceed 100");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }
}
