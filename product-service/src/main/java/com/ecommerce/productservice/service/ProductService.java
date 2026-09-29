package com.ecommerce.productservice.service;

import com.ecommerce.productservice.Dto.ProductDto;

import java.util.List;

public interface ProductService {
    List<ProductDto> findAll();

    ProductDto findById(Integer productId);

    ProductDto save(ProductDto productDto);

    ProductDto update(ProductDto productDto);

    ProductDto update(Integer productId, ProductDto productDto);

    ProductDto decrementQuantity(Integer productId, Integer amount);

    ProductDto incrementQuantity(Integer productId, Integer amount);

    void deleteById(Integer productId);
}
