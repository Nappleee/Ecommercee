package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    @Modifying
    @Query("update Product p set p.quantity = p.quantity - :amount " +
            "where p.productId = :productId and p.quantity >= :amount")
    int decrementQuantity(@Param("productId") Integer productId, @Param("amount") Integer amount);
    @Modifying
    @Query("update Product p set p.quantity = p.quantity + :amount where p.productId = :productId")
    int incrementQuantity(@Param("productId") Integer productId, @Param("amount") Integer amount);
}
