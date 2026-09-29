package com.ecommerce.promotionservice.controller;

import com.ecommerce.promotionservice.dto.PromotionRequest;
import com.ecommerce.promotionservice.entity.Promotion;
import com.ecommerce.promotionservice.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/backoffice/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public List<Promotion> findAll() {
        return promotionService.findAll();
    }

    @GetMapping("/{id}")
    public Promotion findById(@PathVariable Long id) {
        return promotionService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Promotion create(@Valid @RequestBody PromotionRequest request) {
        return promotionService.create(request);
    }

    @PutMapping("/{id}")
    public Promotion update(@PathVariable Long id, @Valid @RequestBody PromotionRequest request) {
        return promotionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        promotionService.delete(id);
    }
}
