package com.ecommerce.productservice.controller;


import com.ecommerce.productservice.Dto.ProductDto;
import com.ecommerce.productservice.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductDto>> findAll() {
        log.info("ProductDto list, controller, fetch all products");
        return ResponseEntity.ok(productService.findAll());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDto> findById(@PathVariable("productId")
                                               @NotBlank(message = "Input must not be blank!")
                                               @Valid final String productId) {
        log.info("ProductDto, controller, fetch a product by id");
        return ResponseEntity.ok(productService.findById(Integer.parseInt(productId)));
    }

    @PostMapping
    public ResponseEntity<ProductDto> save(@RequestBody
                                           @NotNull(message = "Input must not be NULL!")
                                           @Valid final ProductDto productDto) {
        log.info("ProductDto, controller, save data");
        return ResponseEntity.ok(productService.save(productDto));
    }

    @PutMapping
    public ResponseEntity<ProductDto> update(@RequestBody
                                                 @NotNull(message = "Input must not be NULL!")
                                                 @Valid final ProductDto productDto) {
        log.info("ProductDto, controller, update data");
        return ResponseEntity.ok(productService.update(productDto));
    }
    @PutMapping("/{productId}")
    public ResponseEntity<ProductDto> update(@PathVariable("productId")
                                             @NotBlank(message = "Input must not be blank!")
                                             @Valid final String productId,
                                             @RequestBody
                                             @NotNull(message = "Input must not be NULL!")
                                             @Valid final ProductDto productDto) {
        log.info("ProductDto, resource; update product with productId");
        return ResponseEntity.ok(productService.update(Integer.parseInt(productId), productDto));
    }
    @PatchMapping("/{productId}/decrement")
    public ResponseEntity<ProductDto> decrementQuantity(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "1") Integer amount) {
        return ResponseEntity.ok(productService.decrementQuantity(productId, amount));
    }
    @PatchMapping("/{productId}/increment")
    public ResponseEntity<ProductDto> incrementQuantity(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "1") Integer amount) {
        return ResponseEntity.ok(productService.incrementQuantity(productId, amount));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Boolean> deleteById(@PathVariable("productId") final String productId) {
        log.info("Boolean, resource; delete product by id");
        productService.deleteById(Integer.parseInt(productId));
        return ResponseEntity.ok(true);
    }
}
